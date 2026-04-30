package com.pm4.istp.challengepod.services;

import com.pm4.istp.admin.db.AdminConfig;
import com.pm4.istp.admin.services.AdminConfigurationService;
import com.pm4.istp.challengepod.dto.PodStatusEnum;
import com.pm4.istp.challengepod.dto.PodStatusResponse;
import com.pm4.istp.challengepod.events.KubeconfigChangedEvent;
import com.pm4.istp.challengepod.exceptions.ChallengePodException;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.services.ChallengeService;
import com.pm4.istp.course.services.DockerImageAvailabilityService;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentCondition;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import jakarta.annotation.PreDestroy;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
public class ChallengePodService {

  // Label / annotation keys
  private static final String LABEL_APP = "istp-challenge-pod";
  private static final String LABEL_USER_ID = "istp.pm4.ch/user-id";
  private static final String LABEL_CHALLENGE_ID = "istp.pm4.ch/challenge-id";
  private static final String LABEL_CREATED_AT = "istp.pm4.ch/created-at-epoch";
  private static final String ANNOTATION_TERMINAL_PASSWORD = "istp.pm4.ch/terminal-password";

  // TODO(#163): replace with Challenge.containerPort when the model supports it.
  private static final int DEFAULT_APP_PORT = 80;

  private static final int POD_NAME_HASH_LENGTH = 8;

  private final AdminConfigurationService adminConfigurationService;
  private final ChallengeService challengeService;
  private final DockerImageAvailabilityService dockerImageAvailabilityService;
  private final String defaultNamespace;
  private final String domain;
  private final boolean tls;

  private final AtomicReference<KubernetesClient> clientRef = new AtomicReference<>();

  public ChallengePodService(
      @NonNull AdminConfigurationService adminConfigurationService,
      @NonNull ChallengeService challengeService,
      @NonNull DockerImageAvailabilityService dockerImageAvailabilityService,
      @Value("${k8s.default.namespace}") String defaultNamespace,
      @Value("${istp.domain}") String domain,
      @Value("${istp.tls}") boolean tls) {
    this.adminConfigurationService = adminConfigurationService;
    this.challengeService = challengeService;
    this.dockerImageAvailabilityService = dockerImageAvailabilityService;
    this.defaultNamespace = defaultNamespace;
    this.domain = domain;
    this.tls = tls;
  }

  // -------------------------------------------------------------------------
  // Client lifecycle
  // -------------------------------------------------------------------------

  private KubernetesClient getClient() {
    KubernetesClient existing = clientRef.get();
    if (existing != null) {
      return existing;
    }

    AdminConfig adminConfig =
        adminConfigurationService
            .getAdminConfiguration()
            .orElseThrow(
                () ->
                    new ChallengePodException(
                        "No admin configuration found. Upload a kubeconfig first."));

    String kubeconfigContent = adminConfig.getKubeconfig();
    if (kubeconfigContent == null || kubeconfigContent.isBlank()) {
      throw new ChallengePodException(
          "Admin configuration exists but kubeconfig content is missing.");
    }

    Config config = Config.fromKubeconfig(kubeconfigContent);
    config.setConnectionTimeout(5_000);
    config.setRequestTimeout(10_000);
    KubernetesClient newClient = new KubernetesClientBuilder().withConfig(config).build();

    // Another thread may have raced us — discard ours if we lost
    if (!clientRef.compareAndSet(null, newClient)) {
      newClient.close();
      return clientRef.get();
    }

    log.debug("Built new KubernetesClient");
    return newClient;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onKubeconfigChanged(KubeconfigChangedEvent event) {
    KubernetesClient old = clientRef.getAndSet(null);
    if (old != null) {
      try {
        old.close();
      } catch (Exception e) {
        log.warn("Error closing stale KubernetesClient", e);
      }
      log.info("KubernetesClient cache invalidated due to kubeconfig change");
    }
  }

  @PreDestroy
  public void shutdown() {
    KubernetesClient client = clientRef.getAndSet(null);
    if (client != null) {
      try {
        client.close();
      } catch (Exception e) {
        log.warn("Error closing KubernetesClient on shutdown", e);
      }
    }
  }

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  /**
   * Start a pod for (userId, challengeId). Idempotent — returns the existing pod if already
   * running. Boolean in the pair indicates whether a new pod was created (true) or an existing one
   * returned (false).
   */
  public Pair<PodStatusResponse, Boolean> startPod(UUID userId, UUID challengeId) {
    // Visibility / existence check — throws ChallengeNotFoundException or
    // ChallengeAccessDeniedException which flow through GlobalExceptionHandler
    Challenge challenge = challengeService.getChallenge(userId, challengeId);
    dockerImageAvailabilityService.assertImageExists(challenge.getDockerImage());

    AdminConfig adminConfig =
        adminConfigurationService
            .getAdminConfiguration()
            .orElseThrow(() -> new ChallengePodException("No admin configuration found."));

    String hash = computeHash(userId, challengeId);
    String instanceName = "pod-" + hash;

    try {
      // Check for existing pod
      List<Deployment> existing = findDeployments(userId, challengeId);
      if (!existing.isEmpty()) {
        Deployment d = existing.get(0);
        // Defense-in-depth: verify labels match (guards against hash collision)
        Map<String, String> labels = d.getMetadata().getLabels();
        if (!userId.toString().equals(labels.get(LABEL_USER_ID))
            || !challengeId.toString().equals(labels.get(LABEL_CHALLENGE_ID))) {
          log.error(
              "Hash collision detected for instance {}: labels don't match caller", instanceName);
          throw new ChallengePodException(
              "Pod naming conflict detected. Please contact an administrator.");
        }
        return Pair.of(buildResponse(d, adminConfig.getPodTtlSeconds()), false);
      }

      // Create new pod resources
      return Pair.of(
          createResources(userId, challengeId, instanceName, adminConfig, challenge), true);

    } catch (KubernetesClientException e) {
      if (e.getCode() == 409) {
        // Race: another request created the pod between our check and our create — read it
        List<Deployment> existing = findDeployments(userId, challengeId);
        if (!existing.isEmpty()) {
          return Pair.of(buildResponse(existing.get(0), adminConfig.getPodTtlSeconds()), false);
        }
      }
      log.error("K8s error starting pod for challenge {} user {}", challengeId, userId, e);
      throw new ChallengePodException("Failed to start pod: " + e.getMessage(), e);
    } catch (ChallengePodException e) {
      throw e;
    } catch (Exception e) {
      log.error("Unexpected error starting pod for challenge {} user {}", challengeId, userId, e);
      throw new ChallengePodException("Failed to start pod: " + e.getMessage(), e);
    }
  }

  /** Get the current pod status for (userId, challengeId). Returns NOT_FOUND if absent. */
  public PodStatusResponse getPod(UUID userId, UUID challengeId) {
    try {
      List<Deployment> existing = findDeployments(userId, challengeId);
      if (existing.isEmpty()) {
        return PodStatusResponse.notFound();
      }

      AdminConfig adminConfig = adminConfigurationService.getAdminConfiguration().orElse(null);
      int ttl = adminConfig != null ? adminConfig.getPodTtlSeconds() : 3600;
      return buildResponse(existing.get(0), ttl);

    } catch (ChallengePodException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error getting pod for challenge {} user {}", challengeId, userId, e);
      throw new ChallengePodException("Failed to get pod status: " + e.getMessage(), e);
    }
  }

  /**
   * Delete the pod for (userId, challengeId).
   *
   * @return true if a pod was found and deleted, false if no pod existed
   */
  public boolean deletePod(UUID userId, UUID challengeId) {
    try {
      List<Deployment> existing = findDeployments(userId, challengeId);
      if (existing.isEmpty()) {
        return false;
      }

      Deployment d = existing.get(0);
      Map<String, String> labels = d.getMetadata().getLabels();

      // Belt-and-braces ownership check
      if (!userId.toString().equals(labels.get(LABEL_USER_ID))
          || !challengeId.toString().equals(labels.get(LABEL_CHALLENGE_ID))) {
        log.warn(
            "Ownership mismatch on delete for instance {}: denying", d.getMetadata().getName());
        throw new ChallengePodException("Ownership check failed for pod deletion.");
      }

      String instanceName = d.getMetadata().getName();
      deleteByName(instanceName);
      log.info("Deleted pod {} for challenge {} user {}", instanceName, challengeId, userId);
      return true;

    } catch (ChallengePodException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error deleting pod for challenge {} user {}", challengeId, userId, e);
      throw new ChallengePodException("Failed to delete pod: " + e.getMessage(), e);
    }
  }

  /** Reap pods whose age exceeds ttlSeconds. Called by the scheduler. */
  public void reapExpiredPods(int ttlSeconds) {
    KubernetesClient client = getClient();
    List<Deployment> allPods =
        client
            .apps()
            .deployments()
            .inNamespace(defaultNamespace)
            .withLabel("app", LABEL_APP)
            .list()
            .getItems();

    long nowEpoch = Instant.now().getEpochSecond();
    for (Deployment d : allPods) {
      String instanceName = d.getMetadata().getName();
      try {
        String createdAtStr = d.getMetadata().getLabels().get(LABEL_CREATED_AT);
        if (createdAtStr == null) {
          log.warn("Pod {} has no {} label, skipping reap", instanceName, LABEL_CREATED_AT);
          continue;
        }
        long createdAt = Long.parseLong(createdAtStr);
        if (nowEpoch - createdAt > ttlSeconds) {
          deleteByName(instanceName);
          log.info(
              "Reaped expired pod {} (age {}s, ttl {}s)",
              instanceName,
              nowEpoch - createdAt,
              ttlSeconds);
        }
      } catch (Exception e) {
        log.error("Error reaping pod {}: {}", instanceName, e.getMessage(), e);
      }
    }
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  private String computeHash(UUID userId, UUID challengeId) {
    try {
      String input = userId.toString() + ":" + challengeId.toString();
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes).substring(0, POD_NAME_HASH_LENGTH);
    } catch (NoSuchAlgorithmException e) {
      throw new ChallengePodException("SHA-256 algorithm not available", e);
    }
  }

  private List<Deployment> findDeployments(UUID userId, UUID challengeId) {
    return getClient()
        .apps()
        .deployments()
        .inNamespace(defaultNamespace)
        .withLabel("app", LABEL_APP)
        .withLabel(LABEL_USER_ID, userId.toString())
        .withLabel(LABEL_CHALLENGE_ID, challengeId.toString())
        .list()
        .getItems();
  }

  private PodStatusResponse buildResponse(Deployment deployment, int ttlSeconds) {
    Map<String, String> labels = deployment.getMetadata().getLabels();
    Map<String, String> annotations = deployment.getMetadata().getAnnotations();

    String instanceName = deployment.getMetadata().getName();
    String hash = instanceName.substring("pod-".length());

    String password = annotations != null ? annotations.get(ANNOTATION_TERMINAL_PASSWORD) : null;

    Instant createdAt = null;
    Instant expiresAt = null;
    String createdAtStr = labels.get(LABEL_CREATED_AT);
    if (createdAtStr != null) {
      createdAt = Instant.ofEpochSecond(Long.parseLong(createdAtStr));
      expiresAt = createdAt.plusSeconds(ttlSeconds);
    }

    String scheme = tls ? "https" : "http";
    String appUrl = scheme + "://app-" + hash + "." + domain;
    String terminalUrl = scheme + "://term-" + hash + "." + domain;

    PodStatusEnum status = mapDeploymentStatus(deployment);

    return new PodStatusResponse(
        status, instanceName, appUrl, terminalUrl, password, createdAt, expiresAt);
  }

  private PodStatusEnum mapDeploymentStatus(Deployment d) {
    if (d.getMetadata().getDeletionTimestamp() != null) {
      return PodStatusEnum.TERMINATING;
    }

    if (d.getStatus() != null) {
      List<DeploymentCondition> conditions = d.getStatus().getConditions();
      if (conditions != null) {
        boolean replicaFailure =
            conditions.stream()
                .anyMatch(
                    c -> "ReplicaFailure".equals(c.getType()) && "True".equals(c.getStatus()));
        if (replicaFailure) {
          return PodStatusEnum.FAILED;
        }
      }

      Integer readyReplicas = d.getStatus().getReadyReplicas();
      if (readyReplicas != null && readyReplicas >= 1) {
        return PodStatusEnum.RUNNING;
      }
    }

    return PodStatusEnum.PROVISIONING;
  }

  private PodStatusResponse createResources(
      UUID userId,
      UUID challengeId,
      String instanceName,
      AdminConfig adminConfig,
      Challenge challenge) {

    KubernetesClient client = getClient();
    long nowEpoch = Instant.now().getEpochSecond();
    String hash = instanceName.substring("pod-".length());
    String terminalPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

    Map<String, String> labels = new HashMap<>();
    labels.put("app", LABEL_APP);
    labels.put(LABEL_USER_ID, userId.toString());
    labels.put(LABEL_CHALLENGE_ID, challengeId.toString());
    labels.put(LABEL_CREATED_AT, String.valueOf(nowEpoch));

    Map<String, String> annotations = Map.of(ANNOTATION_TERMINAL_PASSWORD, terminalPassword);

    // 1. Deployment
    Deployment deployment =
        new DeploymentBuilder()
            .withNewMetadata()
            .withName(instanceName)
            .withNamespace(defaultNamespace)
            .withLabels(labels)
            .withAnnotations(annotations)
            .endMetadata()
            .withNewSpec()
            .withReplicas(1)
            .withNewSelector()
            .withMatchLabels(labels)
            .endSelector()
            .withNewTemplate()
            .withNewMetadata()
            .withLabels(labels)
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName("app")
            .withImage(challenge.getDockerImage())
            .withNewResources()
            .addToLimits(
                "cpu",
                adminConfig.getCpuLimit() != null ? new Quantity(adminConfig.getCpuLimit()) : null)
            .addToLimits(
                "memory",
                adminConfig.getMemoryLimit() != null
                    ? new Quantity(adminConfig.getMemoryLimit())
                    : null)
            .endResources()
            .addNewPort()
            .withContainerPort(DEFAULT_APP_PORT) // TODO(#163): challenge.getContainerPort()
            .endPort()
            .endContainer()
            .addNewContainer()
            .withName("terminal")
            .withImage("tsl0922/ttyd:1.7.7")
            .withArgs("ttyd", "-W", "-c", "student:" + terminalPassword, "sh")
            .addNewPort()
            .withContainerPort(7681)
            .endPort()
            .endContainer()
            .endSpec()
            .endTemplate()
            .endSpec()
            .build();

    client.apps().deployments().inNamespace(defaultNamespace).resource(deployment).create();

    // 2. Service
    io.fabric8.kubernetes.api.model.Service service =
        new ServiceBuilder()
            .withNewMetadata()
            .withName(instanceName + "-svc")
            .withNamespace(defaultNamespace)
            .withLabels(labels)
            .endMetadata()
            .withNewSpec()
            .withSelector(labels)
            .addNewPort()
            .withName("app-port")
            .withProtocol("TCP")
            .withPort(80)
            .withTargetPort(new IntOrString(DEFAULT_APP_PORT))
            .endPort()
            .addNewPort()
            .withName("term-port")
            .withProtocol("TCP")
            .withPort(8081)
            .withTargetPort(new IntOrString(7681))
            .endPort()
            .withType("ClusterIP")
            .endSpec()
            .build();

    client.services().inNamespace(defaultNamespace).resource(service).create();

    // 3. Ingress
    String appHost = "app-" + hash + "." + domain;
    String termHost = "term-" + hash + "." + domain;

    io.fabric8.kubernetes.api.model.networking.v1.Ingress ingress =
        new IngressBuilder()
            .withNewMetadata()
            .withName(instanceName + "-ingress")
            .withNamespace(defaultNamespace)
            .withLabels(labels)
            .endMetadata()
            .withNewSpec()
            .addNewRule()
            .withHost(appHost)
            .withNewHttp()
            .addNewPath()
            .withPath("/")
            .withPathType("Prefix")
            .withNewBackend()
            .withNewService()
            .withName(service.getMetadata().getName())
            .withNewPort()
            .withNumber(80)
            .endPort()
            .endService()
            .endBackend()
            .endPath()
            .endHttp()
            .endRule()
            .addNewRule()
            .withHost(termHost)
            .withNewHttp()
            .addNewPath()
            .withPath("/")
            .withPathType("Prefix")
            .withNewBackend()
            .withNewService()
            .withName(service.getMetadata().getName())
            .withNewPort()
            .withNumber(8081)
            .endPort()
            .endService()
            .endBackend()
            .endPath()
            .endHttp()
            .endRule()
            .endSpec()
            .build();

    client.network().v1().ingresses().inNamespace(defaultNamespace).resource(ingress).create();

    log.info("Created pod resources for instance {}", instanceName);

    String scheme = tls ? "https" : "http";
    Instant createdAt = Instant.ofEpochSecond(nowEpoch);
    Instant expiresAt = createdAt.plusSeconds(adminConfig.getPodTtlSeconds());

    return new PodStatusResponse(
        PodStatusEnum.PROVISIONING,
        instanceName,
        scheme + "://" + appHost,
        scheme + "://" + termHost,
        terminalPassword,
        createdAt,
        expiresAt);
  }

  private void deleteByName(String instanceName) {
    KubernetesClient client = getClient();
    client.apps().deployments().inNamespace(defaultNamespace).withName(instanceName).delete();
    client.services().inNamespace(defaultNamespace).withName(instanceName + "-svc").delete();
    client
        .network()
        .v1()
        .ingresses()
        .inNamespace(defaultNamespace)
        .withName(instanceName + "-ingress")
        .delete();
  }
}
