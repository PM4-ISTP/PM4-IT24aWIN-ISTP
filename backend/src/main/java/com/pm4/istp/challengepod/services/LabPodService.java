package com.pm4.istp.challengepod.services;

import com.pm4.istp.admin.db.AdminConfig;
import com.pm4.istp.admin.services.AdminConfigurationService;
import com.pm4.istp.challengepod.dto.PodStatusEnum;
import com.pm4.istp.challengepod.dto.PodStatusResponse;
import com.pm4.istp.challengepod.dto.RunningPodResponse;
import com.pm4.istp.challengepod.events.KubeconfigChangedEvent;
import com.pm4.istp.challengepod.exceptions.LabPodException;
import com.pm4.istp.course.db.entities.Lab;
import com.pm4.istp.course.repositories.CourseLabRepository;
import com.pm4.istp.course.services.DockerImageAvailabilityService;
import com.pm4.istp.course.services.LabService;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LocalObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentCondition;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import jakarta.annotation.PreDestroy;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
public class LabPodService {

  // Label / annotation keys
  private static final String LABEL_APP = "istp-lab-pod";
  private static final String LABEL_USER_ID = "istp.pm4.ch/user-id";
  private static final String LABEL_CHALLENGE_ID = "istp.pm4.ch/lab-id";
  private static final String LABEL_CREATED_AT = "istp.pm4.ch/created-at-epoch";

  private static final int POD_NAME_HASH_LENGTH = 8;
  private static final String INGRESS_NAME_SUFFIX = "-ingress";

  private final AdminConfigurationService adminConfigurationService;
  private final LabService labService;
  private final DockerImageAvailabilityService dockerImageAvailabilityService;
  private final CourseLabRepository courseLabRepository;
  private final String defaultNamespace;
  private final String domain;
  private final boolean tls;
  private final String labHostPrefix;

  private final AtomicReference<KubernetesClient> clientRef = new AtomicReference<>();

  public LabPodService(
      @NonNull AdminConfigurationService adminConfigurationService,
      @NonNull LabService labService,
      @NonNull DockerImageAvailabilityService dockerImageAvailabilityService,
      @NonNull CourseLabRepository courseLabRepository,
      @Value("${k8s.default.namespace}") String defaultNamespace,
      @Value("${istp.domain}") String domain,
      @Value("${istp.tls}") boolean tls,
      @Value("${istp.lab-host-prefix:}") String labHostPrefix) {
    this.adminConfigurationService = adminConfigurationService;
    this.labService = labService;
    this.dockerImageAvailabilityService = dockerImageAvailabilityService;
    this.courseLabRepository = courseLabRepository;
    this.defaultNamespace = defaultNamespace;
    this.domain = domain;
    this.tls = tls;
    this.labHostPrefix = normalizeHostPrefix(labHostPrefix);
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
                    new LabPodException(
                        "No admin configuration found. Upload a kubeconfig first."));

    String kubeconfigContent = adminConfig.getKubeconfig();
    if (kubeconfigContent == null || kubeconfigContent.isBlank()) {
      throw new LabPodException("Admin configuration exists but kubeconfig content is missing.");
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
   * Start a pod for (userId, labId). Idempotent — returns the existing pod if already running.
   * Boolean in the pair indicates whether a new pod was created (true) or an existing one returned
   * (false).
   */
  public Pair<PodStatusResponse, Boolean> startPod(UUID userId, UUID labId) {
    // Visibility / existence check — throws LabNotFoundException or
    // LabAccessDeniedException which flow through GlobalExceptionHandler
    Lab lab = labService.getChallenge(userId, labId);
    dockerImageAvailabilityService.assertImageExists(lab.getDockerImage());

    AdminConfig adminConfig =
        adminConfigurationService
            .getAdminConfiguration()
            .orElseThrow(() -> new LabPodException("No admin configuration found."));

    String hash = computeHash(userId, labId);
    String instanceName = "pod-" + hash;

    try {
      // Check for existing pod
      List<Deployment> existing = findDeployments(userId, labId);
      if (!existing.isEmpty()) {
        Deployment d = existing.get(0);
        // Defense-in-depth: verify labels match (guards against hash collision)
        Map<String, String> labels = d.getMetadata().getLabels();
        if (!userId.toString().equals(labels.get(LABEL_USER_ID))
            || !labId.toString().equals(labels.get(LABEL_CHALLENGE_ID))) {
          log.error(
              "Hash collision detected for instance {}: labels don't match caller", instanceName);
          throw new LabPodException(
              "Pod naming conflict detected. Please contact an administrator.");
        }
        return Pair.of(buildResponse(d, adminConfig.getPodTtlSeconds()), false);
      }

      List<Deployment> existingForUser = findDeployments(userId);
      if (!existingForUser.isEmpty()) {
        throw new LabPodException("Only one lab pod can be active per student.");
      }

      // Create new pod resources
      return Pair.of(createResources(userId, labId, instanceName, adminConfig, lab), true);

    } catch (KubernetesClientException e) {
      if (e.getCode() == 409) {
        // Race: another request created the pod between our check and our create — read it
        List<Deployment> existing = findDeployments(userId, labId);
        if (!existing.isEmpty()) {
          return Pair.of(buildResponse(existing.get(0), adminConfig.getPodTtlSeconds()), false);
        }
      }
      log.error("K8s error starting pod for lab {} user {}", labId, userId, e);
      throw new LabPodException("Failed to start pod: " + e.getMessage(), e);
    } catch (LabPodException e) {
      throw e;
    } catch (Exception e) {
      log.error("Unexpected error starting pod for lab {} user {}", labId, userId, e);
      throw new LabPodException("Failed to start pod: " + e.getMessage(), e);
    }
  }

  /** Get the current pod status for (userId, labId). Returns NOT_FOUND if absent. */
  public PodStatusResponse getPod(UUID userId, UUID labId) {
    try {
      List<Deployment> existing = findDeployments(userId, labId);
      if (existing.isEmpty()) {
        return PodStatusResponse.notFound();
      }

      AdminConfig adminConfig = adminConfigurationService.getAdminConfiguration().orElse(null);
      int ttl = adminConfig != null ? adminConfig.getPodTtlSeconds() : 3600;
      return buildResponse(existing.get(0), ttl);

    } catch (LabPodException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error getting pod for lab {} user {}", labId, userId, e);
      throw new LabPodException("Failed to get pod status: " + e.getMessage(), e);
    }
  }

  public List<RunningPodResponse> listPods(UUID userId) {
    try {
      AdminConfig adminConfig = adminConfigurationService.getAdminConfiguration().orElse(null);
      int ttl = adminConfig != null ? adminConfig.getPodTtlSeconds() : 3600;

      return findDeployments(userId).stream()
          .sorted(Comparator.comparing(this::createdAtOf).reversed())
          .map(deployment -> buildRunningPodResponse(userId, deployment, ttl))
          .flatMap(Optional::stream)
          .toList();
    } catch (LabPodException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error listing pods for user {}", userId, e);
      throw new LabPodException("Failed to list pods: " + e.getMessage(), e);
    }
  }

  /**
   * Delete the pod for (userId, labId).
   *
   * @return true if a pod was found and deleted, false if no pod existed
   */
  public boolean deletePod(UUID userId, UUID labId) {
    try {
      List<Deployment> existing = findDeployments(userId, labId);
      if (existing.isEmpty()) {
        return false;
      }

      Deployment d = existing.get(0);
      Map<String, String> labels = d.getMetadata().getLabels();

      // Belt-and-braces ownership check
      if (!userId.toString().equals(labels.get(LABEL_USER_ID))
          || !labId.toString().equals(labels.get(LABEL_CHALLENGE_ID))) {
        log.warn(
            "Ownership mismatch on delete for instance {}: denying", d.getMetadata().getName());
        throw new LabPodException("Ownership check failed for pod deletion.");
      }

      String instanceName = d.getMetadata().getName();
      deleteByName(instanceName);
      log.info("Deleted pod {} for lab {} user {}", instanceName, labId, userId);
      return true;

    } catch (LabPodException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error deleting pod for lab {} user {}", labId, userId, e);
      throw new LabPodException("Failed to delete pod: " + e.getMessage(), e);
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

  private String computeHash(UUID userId, UUID labId) {
    try {
      String input = userId.toString() + ":" + labId.toString();
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes).substring(0, POD_NAME_HASH_LENGTH);
    } catch (NoSuchAlgorithmException e) {
      throw new LabPodException("SHA-256 algorithm not available", e);
    }
  }

  private List<Deployment> findDeployments(UUID userId, UUID labId) {
    return getClient()
        .apps()
        .deployments()
        .inNamespace(defaultNamespace)
        .withLabel("app", LABEL_APP)
        .withLabel(LABEL_USER_ID, userId.toString())
        .withLabel(LABEL_CHALLENGE_ID, labId.toString())
        .list()
        .getItems();
  }

  private List<Deployment> findDeployments(UUID userId) {
    return getClient()
        .apps()
        .deployments()
        .inNamespace(defaultNamespace)
        .withLabel("app", LABEL_APP)
        .withLabel(LABEL_USER_ID, userId.toString())
        .list()
        .getItems();
  }

  private Optional<RunningPodResponse> buildRunningPodResponse(
      UUID userId, Deployment deployment, int ttlSeconds) {
    Map<String, String> labels = deployment.getMetadata().getLabels();
    if (labels == null) {
      return Optional.empty();
    }

    UUID labId;
    try {
      labId = UUID.fromString(labels.get(LABEL_CHALLENGE_ID));
    } catch (Exception e) {
      log.warn("Pod {} has no valid lab id label", deployment.getMetadata().getName());
      return Optional.empty();
    }

    List<Object[]> summaries =
        courseLabRepository.findEnrolledCourseLabSummariesForUserAndLab(userId, labId);
    Object[] summary = summaries.isEmpty() ? null : summaries.get(0);

    UUID courseId = summary != null ? (UUID) summary[0] : null;
    String courseTitle = summary != null ? (String) summary[1] : null;
    String labTitle = summary != null ? (String) summary[3] : null;

    return Optional.of(
        new RunningPodResponse(
            labId, labTitle, courseId, courseTitle, buildResponse(deployment, ttlSeconds)));
  }

  private Instant createdAtOf(Deployment deployment) {
    Map<String, String> labels = deployment.getMetadata().getLabels();
    if (labels == null) {
      return Instant.EPOCH;
    }
    try {
      return Instant.ofEpochSecond(Long.parseLong(labels.get(LABEL_CREATED_AT)));
    } catch (Exception e) {
      return Instant.EPOCH;
    }
  }

  private PodStatusResponse buildResponse(Deployment deployment, int ttlSeconds) {
    Map<String, String> labels = deployment.getMetadata().getLabels();

    String instanceName = deployment.getMetadata().getName();
    String hash = instanceName.substring("pod-".length());

    Instant createdAt = null;
    Instant expiresAt = null;
    String createdAtStr = labels.get(LABEL_CREATED_AT);
    if (createdAtStr != null) {
      createdAt = Instant.ofEpochSecond(Long.parseLong(createdAtStr));
      expiresAt = createdAt.plusSeconds(ttlSeconds);
    }

    String scheme = tls ? "https" : "http";
    String appUrl =
        scheme
            + "://"
            + findIngressHost(instanceName, 80).orElseGet(() -> buildLabHost("app", hash));

    PodStatusEnum status = mapDeploymentStatus(deployment);

    return new PodStatusResponse(status, instanceName, appUrl, null, null, createdAt, expiresAt);
  }

  private PodStatusEnum mapDeploymentStatus(Deployment d) {
    if (d.getMetadata().getDeletionTimestamp() != null) {
      return PodStatusEnum.TERMINATING;
    }

    Optional<PodStatusEnum> podFailureStatus = findPodFailureStatus(d);
    if (podFailureStatus.isPresent()) {
      return podFailureStatus.get();
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

  private Optional<PodStatusEnum> findPodFailureStatus(Deployment deployment) {
    Map<String, String> labels = deployment.getMetadata().getLabels();
    if (labels == null || labels.isEmpty()) {
      return Optional.empty();
    }

    try {
      List<Pod> pods =
          getClient().pods().inNamespace(defaultNamespace).withLabels(labels).list().getItems();

      boolean hasFailedContainer =
          pods.stream()
              .filter(pod -> pod.getStatus() != null)
              .map(pod -> pod.getStatus().getContainerStatuses())
              .filter(Objects::nonNull)
              .flatMap(List::stream)
              .anyMatch(this::containerHasFailed);

      return hasFailedContainer ? Optional.of(PodStatusEnum.FAILED) : Optional.empty();
    } catch (Exception e) {
      log.debug("Could not inspect pod status for {}", deployment.getMetadata().getName(), e);
      return Optional.empty();
    }
  }

  private boolean containerHasFailed(ContainerStatus status) {
    if (status.getState() == null) {
      return false;
    }

    if (status.getState().getWaiting() != null) {
      String reason = status.getState().getWaiting().getReason();
      return "ErrImagePull".equals(reason)
          || "ImagePullBackOff".equals(reason)
          || "CreateContainerConfigError".equals(reason)
          || "CreateContainerError".equals(reason)
          || "CrashLoopBackOff".equals(reason);
    }

    if (status.getState().getTerminated() != null) {
      Integer exitCode = status.getState().getTerminated().getExitCode();
      return exitCode != null && exitCode != 0;
    }

    return false;
  }

  private PodStatusResponse createResources(
      UUID userId, UUID labId, String instanceName, AdminConfig adminConfig, Lab lab) {

    KubernetesClient client = getClient();
    long nowEpoch = Instant.now().getEpochSecond();
    String hash = instanceName.substring("pod-".length());

    Map<String, String> labels = new HashMap<>();
    labels.put("app", LABEL_APP);
    labels.put(LABEL_USER_ID, userId.toString());
    labels.put(LABEL_CHALLENGE_ID, labId.toString());
    labels.put(LABEL_CREATED_AT, String.valueOf(nowEpoch));
    int containerPort = resolveContainerPort(lab);

    // 1. Deployment
    Deployment deployment =
        new DeploymentBuilder()
            .withNewMetadata()
            .withName(instanceName)
            .withNamespace(defaultNamespace)
            .withLabels(labels)
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
            .withAutomountServiceAccountToken(false)
            .addNewContainer()
            .withName("app")
            .withImage(lab.getDockerImage())
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
            .withContainerPort(containerPort)
            .endPort()
            .endContainer()
            .endSpec()
            .endTemplate()
            .endSpec()
            .build();

    String imagePullSecretName = adminConfig.getImagePullSecretName();
    if (imagePullSecretName != null && !imagePullSecretName.isBlank()) {
      deployment
          .getSpec()
          .getTemplate()
          .getSpec()
          .setImagePullSecrets(
              List.of(new LocalObjectReferenceBuilder().withName(imagePullSecretName).build()));
    }

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
            .withTargetPort(new IntOrString(containerPort))
            .endPort()
            .withType("ClusterIP")
            .endSpec()
            .build();

    client.services().inNamespace(defaultNamespace).resource(service).create();

    // 3. Ingress
    String appHost = buildLabHost("app", hash);

    io.fabric8.kubernetes.api.model.networking.v1.Ingress ingress =
        new IngressBuilder()
            .withNewMetadata()
            .withName(instanceName + INGRESS_NAME_SUFFIX)
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
        null,
        null,
        createdAt,
        expiresAt);
  }

  private String buildLabHost(String service, String hash) {
    String hostPrefix =
        labHostPrefix.isBlank() ? service + "-" + hash : service + "-" + labHostPrefix + "-" + hash;
    return hostPrefix + "." + domain;
  }

  private int resolveContainerPort(Lab lab) {
    Integer containerPort = lab.getContainerPort();
    if (containerPort == null) {
      return Lab.DEFAULT_CONTAINER_PORT;
    }
    if (containerPort < 1 || containerPort > 65_535) {
      throw new LabPodException("Lab container port must be between 1 and 65535.");
    }
    return containerPort;
  }

  private String normalizeHostPrefix(String prefix) {
    if (prefix == null) {
      return "";
    }
    String normalized = prefix.trim().toLowerCase();
    StringBuilder hostPrefix = new StringBuilder(normalized.length());
    for (int i = 0; i < normalized.length(); i++) {
      char c = normalized.charAt(i);
      if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-') {
        hostPrefix.append(c);
      } else {
        hostPrefix.append('-');
      }
    }
    return trimHyphens(hostPrefix);
  }

  private String trimHyphens(StringBuilder value) {
    int start = 0;
    int end = value.length();
    while (start < end && value.charAt(start) == '-') {
      start++;
    }
    while (end > start && value.charAt(end - 1) == '-') {
      end--;
    }
    return value.substring(start, end);
  }

  private Optional<String> findIngressHost(String instanceName, int servicePort) {
    try {
      var ingress =
          getClient()
              .network()
              .v1()
              .ingresses()
              .inNamespace(defaultNamespace)
              .withName(instanceName + INGRESS_NAME_SUFFIX)
              .get();

      if (ingress == null || ingress.getSpec() == null || ingress.getSpec().getRules() == null) {
        return Optional.empty();
      }

      return ingress.getSpec().getRules().stream()
          .filter(rule -> rule.getHttp() != null && rule.getHttp().getPaths() != null)
          .filter(
              rule ->
                  rule.getHttp().getPaths().stream()
                      .anyMatch(
                          path ->
                              path.getBackend() != null
                                  && path.getBackend().getService() != null
                                  && path.getBackend().getService().getPort() != null
                                  && Integer.valueOf(servicePort)
                                      .equals(
                                          path.getBackend().getService().getPort().getNumber())))
          .map(IngressRule::getHost)
          .filter(host -> host != null && !host.isBlank())
          .findFirst();
    } catch (Exception e) {
      log.debug("Could not inspect ingress host for {}", instanceName, e);
      return Optional.empty();
    }
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
        .withName(instanceName + INGRESS_NAME_SUFFIX)
        .delete();
  }
}
