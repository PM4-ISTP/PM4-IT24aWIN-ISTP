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
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
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
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
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

  // Stable labels used for Kubernetes selectors and ownership checks.
  private static final String LABEL_APP = "istp-lab-pod";
  private static final String LABEL_USER_ID = "istp.pm4.ch/user-id";
  private static final String LABEL_LAB_ID = "istp.pm4.ch/lab-id";

  // Lifecycle data changes over time, so keep it out of selectors.
  private static final String ANNOTATION_CREATED_AT = "istp.pm4.ch/created-at-epoch";
  private static final String ANNOTATION_LAST_ACTIVITY_AT = "istp.pm4.ch/last-activity-at-epoch";
  private static final String ANNOTATION_BASE_TTL_SECONDS = "istp.pm4.ch/base-ttl-seconds";
  private static final String ANNOTATION_EXTENSION_COUNT = "istp.pm4.ch/ttl-extension-count";

  private static final int DEFAULT_TTL_SECONDS = 3600;
  private static final int DEFAULT_EXTENSION_INCREMENT_SECONDS = 1800;
  private static final int DEFAULT_MAX_EXTENSION_COUNT = 2;
  private static final int ACTIVITY_TOUCH_MIN_SECONDS = 60;

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
  private final int extensionIncrementSeconds;
  private final int maxExtensionCount;

  private final AtomicReference<KubernetesClient> clientRef = new AtomicReference<>();

  public LabPodService(
      @NonNull AdminConfigurationService adminConfigurationService,
      @NonNull LabService labService,
      @NonNull DockerImageAvailabilityService dockerImageAvailabilityService,
      @NonNull CourseLabRepository courseLabRepository,
      @Value("${k8s.default.namespace}") String defaultNamespace,
      @Value("${istp.domain}") String domain,
      @Value("${istp.tls}") boolean tls,
      @Value("${istp.lab-host-prefix:}") String labHostPrefix,
      @Value("${istp.pod-extension-seconds:1800}") int extensionIncrementSeconds,
      @Value("${istp.pod-max-extensions:2}") int maxExtensionCount) {
    this.adminConfigurationService = adminConfigurationService;
    this.labService = labService;
    this.dockerImageAvailabilityService = dockerImageAvailabilityService;
    this.courseLabRepository = courseLabRepository;
    this.defaultNamespace = defaultNamespace;
    this.domain = domain;
    this.tls = tls;
    this.labHostPrefix = normalizeHostPrefix(labHostPrefix);
    this.extensionIncrementSeconds =
        extensionIncrementSeconds > 0
            ? extensionIncrementSeconds
            : DEFAULT_EXTENSION_INCREMENT_SECONDS;
    this.maxExtensionCount =
        maxExtensionCount >= 0 ? maxExtensionCount : DEFAULT_MAX_EXTENSION_COUNT;
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
    Lab lab = labService.getLab(userId, labId);
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
            || !labId.toString().equals(labels.get(LABEL_LAB_ID))) {
          log.error(
              "Hash collision detected for instance {}: labels don't match caller", instanceName);
          throw new LabPodException(
              "Pod naming conflict detected. Please contact an administrator.");
        }
        int ttl = resolveBaseTtlSeconds(lab, adminConfig);
        Deployment touched = ensureLifecycleAnnotationsAndTouch(d, ttl);
        return Pair.of(buildResponse(touched, ttl), false);
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
          int ttl = resolveBaseTtlSeconds(lab, adminConfig);
          Deployment touched = ensureLifecycleAnnotationsAndTouch(existing.get(0), ttl);
          return Pair.of(buildResponse(touched, ttl), false);
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

      int ttl = resolveConfiguredDefaultTtlSeconds();
      Deployment touched = touchLastActivityIfNeeded(existing.get(0));
      return buildResponse(touched, ttl);

    } catch (LabPodException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error getting pod for lab {} user {}", labId, userId, e);
      throw new LabPodException("Failed to get pod status: " + e.getMessage(), e);
    }
  }

  public List<RunningPodResponse> listPods(UUID userId) {
    try {
      int ttl = resolveConfiguredDefaultTtlSeconds();

      return findDeployments(userId).stream()
          .sorted(Comparator.comparing(this::createdAtOf).reversed())
          .map(this::touchLastActivityIfNeeded)
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
          || !labId.toString().equals(labels.get(LABEL_LAB_ID))) {
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

  public PodStatusResponse extendPod(UUID userId, UUID labId) {
    try {
      List<Deployment> existing = findDeployments(userId, labId);
      if (existing.isEmpty()) {
        throw new LabPodException("No active pod found for this lab.");
      }

      Deployment deployment = existing.get(0);
      Map<String, String> labels = deployment.getMetadata().getLabels();
      if (labels == null
          || !userId.toString().equals(labels.get(LABEL_USER_ID))
          || !labId.toString().equals(labels.get(LABEL_LAB_ID))) {
        throw new LabPodException("Ownership check failed for pod extension.");
      }

      int fallbackTtl = resolveConfiguredDefaultTtlSeconds();
      Map<String, String> annotations = deployment.getMetadata().getAnnotations();
      if (annotations == null) {
        annotations = Map.of();
      }
      int currentCount =
          Math.max(0, parseIntMetadata(annotations, labels, ANNOTATION_EXTENSION_COUNT, 0));
      if (currentCount >= maxExtensionCount) {
        throw new LabPodException("Maximum pod extension limit reached.");
      }

      Map<String, String> updatedAnnotations = new HashMap<>(annotations);
      long nowEpoch = Instant.now().getEpochSecond();
      long createdAt = parseLongMetadata(annotations, labels, ANNOTATION_CREATED_AT, nowEpoch);
      updatedAnnotations.putIfAbsent(ANNOTATION_CREATED_AT, String.valueOf(createdAt));
      updatedAnnotations.putIfAbsent(ANNOTATION_BASE_TTL_SECONDS, String.valueOf(fallbackTtl));
      updatedAnnotations.put(ANNOTATION_EXTENSION_COUNT, String.valueOf(currentCount + 1));
      updatedAnnotations.put(ANNOTATION_LAST_ACTIVITY_AT, String.valueOf(nowEpoch));

      Deployment updated = updateDeploymentAnnotations(deployment, updatedAnnotations);

      log.info(
          "Extended pod {} for user {} lab {} by {}s (extension {}/{})",
          deployment.getMetadata().getName(),
          userId,
          labId,
          extensionIncrementSeconds,
          currentCount + 1,
          maxExtensionCount);

      return buildResponse(updated, fallbackTtl);
    } catch (LabPodException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error extending pod for lab {} user {}", labId, userId, e);
      throw new LabPodException("Failed to extend pod: " + e.getMessage(), e);
    }
  }

  /** Reap pods whose idle-time exceeds effective TTL. Called by the scheduler. */
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
        Map<String, String> labels = d.getMetadata().getLabels();
        Map<String, String> annotations = d.getMetadata().getAnnotations();
        long createdAt = parseLongMetadata(annotations, labels, ANNOTATION_CREATED_AT, -1);
        if (createdAt <= 0) {
          log.warn(
              "Pod {} has no {} annotation, skipping reap", instanceName, ANNOTATION_CREATED_AT);
          continue;
        }
        long lastActivity =
            parseLongMetadata(annotations, labels, ANNOTATION_LAST_ACTIVITY_AT, createdAt);
        int effectiveTtl = resolveEffectiveTtlSeconds(annotations, labels, ttlSeconds);

        if (nowEpoch - lastActivity > effectiveTtl) {
          deleteByName(instanceName);
          log.info(
              "Reaped expired pod {} (idle {}s, ttl {}s, created {}s ago)",
              instanceName,
              nowEpoch - lastActivity,
              effectiveTtl,
              nowEpoch - createdAt);
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
        .withLabel(LABEL_LAB_ID, labId.toString())
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
      labId = UUID.fromString(labels.get(LABEL_LAB_ID));
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
    try {
      long createdAt =
          parseLongMetadata(
              deployment.getMetadata().getAnnotations(), labels, ANNOTATION_CREATED_AT, -1);
      return createdAt > 0 ? Instant.ofEpochSecond(createdAt) : Instant.EPOCH;
    } catch (Exception e) {
      return Instant.EPOCH;
    }
  }

  private int resolveConfiguredDefaultTtlSeconds() {
    AdminConfig adminConfig = adminConfigurationService.getAdminConfiguration().orElse(null);
    return adminConfig != null ? adminConfig.getPodTtlSeconds() : DEFAULT_TTL_SECONDS;
  }

  private int resolveBaseTtlSeconds(Lab lab, AdminConfig adminConfig) {
    if (lab != null && lab.getPodTtlSeconds() != null) {
      return lab.getPodTtlSeconds();
    }
    if (adminConfig != null) {
      return adminConfig.getPodTtlSeconds();
    }
    return DEFAULT_TTL_SECONDS;
  }

  private int resolveEffectiveTtlSeconds(
      Map<String, String> annotations, Map<String, String> labels, int fallbackTtlSeconds) {
    int baseTtl =
        parseIntMetadata(annotations, labels, ANNOTATION_BASE_TTL_SECONDS, fallbackTtlSeconds);
    int extensionCount = parseIntMetadata(annotations, labels, ANNOTATION_EXTENSION_COUNT, 0);
    extensionCount = Math.max(0, Math.min(extensionCount, maxExtensionCount));
    return baseTtl + extensionCount * extensionIncrementSeconds;
  }

  private long parseLongMetadata(
      Map<String, String> annotations,
      Map<String, String> fallbackLabels,
      String key,
      long fallbackValue) {
    String raw = metadataValue(annotations, fallbackLabels, key);
    if (raw == null || raw.isBlank()) {
      return fallbackValue;
    }
    try {
      return Long.parseLong(raw);
    } catch (NumberFormatException ex) {
      return fallbackValue;
    }
  }

  private int parseIntMetadata(
      Map<String, String> annotations,
      Map<String, String> fallbackLabels,
      String key,
      int fallbackValue) {
    String raw = metadataValue(annotations, fallbackLabels, key);
    if (raw == null || raw.isBlank()) {
      return fallbackValue;
    }
    try {
      return Integer.parseInt(raw);
    } catch (NumberFormatException ex) {
      return fallbackValue;
    }
  }

  private String metadataValue(
      Map<String, String> annotations, Map<String, String> fallbackLabels, String key) {
    if (annotations != null && annotations.containsKey(key)) {
      return annotations.get(key);
    }
    return fallbackLabels != null ? fallbackLabels.get(key) : null;
  }

  private Deployment touchLastActivityIfNeeded(Deployment deployment) {
    try {
      Map<String, String> labels = deployment.getMetadata().getLabels();
      if (labels == null) {
        labels = Map.of();
      }
      Map<String, String> annotations = deployment.getMetadata().getAnnotations();
      if (annotations == null) {
        annotations = Map.of();
      }
      long now = Instant.now().getEpochSecond();
      long createdAt = parseLongMetadata(annotations, labels, ANNOTATION_CREATED_AT, now);
      long lastActivity =
          parseLongMetadata(annotations, labels, ANNOTATION_LAST_ACTIVITY_AT, createdAt);
      if (now - lastActivity < ACTIVITY_TOUCH_MIN_SECONDS) {
        return deployment;
      }

      // We rely on Kubernetes optimistic locking during replace(); if another request updated
      // the same deployment meanwhile, the API will reject stale writes and we keep the old object.
      Map<String, String> updatedAnnotations = new HashMap<>(annotations);
      updatedAnnotations.putIfAbsent(ANNOTATION_CREATED_AT, String.valueOf(createdAt));
      updatedAnnotations.put(ANNOTATION_LAST_ACTIVITY_AT, String.valueOf(now));
      return updateDeploymentAnnotations(deployment, updatedAnnotations);
    } catch (Exception e) {
      log.debug("Could not update last activity for {}", deployment.getMetadata().getName(), e);
      return deployment;
    }
  }

  /**
   * Initializes missing lifecycle annotations (base-ttl-seconds, ttl-extension-count) and updates
   * last-activity-at if the touch threshold has been exceeded — all in a single K8s write to avoid
   * resource-version conflicts from two consecutive updates.
   */
  private Deployment ensureLifecycleAnnotationsAndTouch(Deployment deployment, int baseTtl) {
    try {
      Map<String, String> labels = deployment.getMetadata().getLabels();
      if (labels == null) {
        labels = Map.of();
      }
      Map<String, String> annotations = deployment.getMetadata().getAnnotations();
      if (annotations == null) {
        annotations = Map.of();
      }

      long now = Instant.now().getEpochSecond();
      long createdAt = parseLongMetadata(annotations, labels, ANNOTATION_CREATED_AT, now);
      long lastActivity =
          parseLongMetadata(annotations, labels, ANNOTATION_LAST_ACTIVITY_AT, createdAt);

      boolean activityNeedsTouch = (now - lastActivity) >= ACTIVITY_TOUCH_MIN_SECONDS;
      boolean missingCreatedAt = !annotations.containsKey(ANNOTATION_CREATED_AT);
      boolean missingLastActivity = !annotations.containsKey(ANNOTATION_LAST_ACTIVITY_AT);
      boolean missingBaseTtl = !annotations.containsKey(ANNOTATION_BASE_TTL_SECONDS);
      boolean missingExtCount = !annotations.containsKey(ANNOTATION_EXTENSION_COUNT);

      if (!activityNeedsTouch
          && !missingCreatedAt
          && !missingLastActivity
          && !missingBaseTtl
          && !missingExtCount) {
        return deployment;
      }

      Map<String, String> updatedAnnotations = new HashMap<>(annotations);
      updatedAnnotations.putIfAbsent(ANNOTATION_CREATED_AT, String.valueOf(createdAt));
      updatedAnnotations.putIfAbsent(ANNOTATION_BASE_TTL_SECONDS, String.valueOf(baseTtl));
      updatedAnnotations.putIfAbsent(ANNOTATION_EXTENSION_COUNT, "0");
      if (activityNeedsTouch) {
        updatedAnnotations.put(ANNOTATION_LAST_ACTIVITY_AT, String.valueOf(now));
      } else {
        updatedAnnotations.putIfAbsent(ANNOTATION_LAST_ACTIVITY_AT, String.valueOf(lastActivity));
      }
      return updateDeploymentAnnotations(deployment, updatedAnnotations);
    } catch (Exception e) {
      log.debug(
          "Could not update lifecycle annotations for {}", deployment.getMetadata().getName(), e);
      return deployment;
    }
  }

  private Deployment updateDeploymentAnnotations(
      Deployment deployment, Map<String, String> updatedAnnotations) {
    Deployment patch =
        new DeploymentBuilder()
            .withNewMetadata()
            .withName(deployment.getMetadata().getName())
            .withNamespace(defaultNamespace)
            .withAnnotations(updatedAnnotations)
            .endMetadata()
            .build();

    return getClient()
        .apps()
        .deployments()
        .inNamespace(defaultNamespace)
        .withName(deployment.getMetadata().getName())
        .patch(PatchContext.of(PatchType.JSON_MERGE), patch);
  }

  private PodStatusResponse buildResponse(Deployment deployment, int fallbackTtlSeconds) {
    Map<String, String> labels = deployment.getMetadata().getLabels();
    if (labels == null) {
      labels = Map.of();
    }
    Map<String, String> annotations = deployment.getMetadata().getAnnotations();
    if (annotations == null) {
      annotations = Map.of();
    }

    String instanceName = deployment.getMetadata().getName();
    String hash = instanceName.substring("pod-".length());

    Instant createdAt = null;
    Instant expiresAt = null;
    Instant lastActivityAt = null;
    long createdEpoch = parseLongMetadata(annotations, labels, ANNOTATION_CREATED_AT, -1);
    if (createdEpoch > 0) {
      createdAt = Instant.ofEpochSecond(createdEpoch);
    }
    long lastActivityEpoch =
        parseLongMetadata(
            annotations, labels, ANNOTATION_LAST_ACTIVITY_AT, createdEpoch > 0 ? createdEpoch : -1);
    if (lastActivityEpoch > 0) {
      lastActivityAt = Instant.ofEpochSecond(lastActivityEpoch);
    } else {
      lastActivityAt = createdAt;
    }
    int effectiveTtl = resolveEffectiveTtlSeconds(annotations, labels, fallbackTtlSeconds);
    if (lastActivityAt != null) {
      expiresAt = lastActivityAt.plusSeconds(effectiveTtl);
    }

    String scheme = tls ? "https" : "http";
    String appUrl =
        scheme
            + "://"
            + findIngressHost(instanceName, 80).orElseGet(() -> buildLabHost("app", hash));

    PodStatusEnum status = mapDeploymentStatus(deployment);

    // Clamp extension count to the configured limit — consistent with
    // resolveEffectiveTtlSeconds
    int extensionCount = parseIntMetadata(annotations, labels, ANNOTATION_EXTENSION_COUNT, 0);
    extensionCount = Math.max(0, Math.min(extensionCount, maxExtensionCount));
    boolean canExtend = extensionCount < maxExtensionCount;

    return new PodStatusResponse(
        status,
        instanceName,
        appUrl,
        null,
        null,
        createdAt,
        expiresAt,
        lastActivityAt,
        effectiveTtl,
        extensionCount,
        maxExtensionCount,
        canExtend);
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
    int baseTtlSeconds = resolveBaseTtlSeconds(lab, adminConfig);

    Map<String, String> labels = new HashMap<>();
    labels.put("app", LABEL_APP);
    labels.put(LABEL_USER_ID, userId.toString());
    labels.put(LABEL_LAB_ID, labId.toString());
    Map<String, String> annotations = new HashMap<>();
    annotations.put(ANNOTATION_CREATED_AT, String.valueOf(nowEpoch));
    annotations.put(ANNOTATION_LAST_ACTIVITY_AT, String.valueOf(nowEpoch));
    annotations.put(ANNOTATION_BASE_TTL_SECONDS, String.valueOf(baseTtlSeconds));
    annotations.put(ANNOTATION_EXTENSION_COUNT, "0");
    int containerPort = resolveContainerPort(lab);

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
            .withAutomountServiceAccountToken(false)
            .addNewContainer()
            .withName("app")
            .withImage(lab.getDockerImage())
            .withResources(buildLabResourceRequirements(adminConfig))
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

    // Inject a Kubernetes Secret as environment variables into the lab container.
    // Currently introduced for the "LLM01 - Prompt Injection" lab, which needs a
    // GROQ_API_KEY to call the Groq API (set via the "groq-api-secret" K8s Secret).
    // The envSecretName is set directly in the DB for that lab — no instructor UI exists yet.
    String envSecretName = lab.getEnvSecretName();
    if (envSecretName != null && !envSecretName.isBlank()) {
      deployment
          .getSpec()
          .getTemplate()
          .getSpec()
          .getContainers()
          .get(0)
          .setEnvFrom(
              List.of(
                  new io.fabric8.kubernetes.api.model.EnvFromSourceBuilder()
                      .withNewSecretRef()
                      .withName(envSecretName)
                      .endSecretRef()
                      .build()));
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
    Instant expiresAt = createdAt.plusSeconds(baseTtlSeconds);

    return new PodStatusResponse(
        PodStatusEnum.PROVISIONING,
        instanceName,
        scheme + "://" + appHost,
        null,
        null,
        createdAt,
        expiresAt,
        createdAt,
        baseTtlSeconds,
        0,
        maxExtensionCount,
        true);
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

  private ResourceRequirements buildLabResourceRequirements(AdminConfig adminConfig) {
    ResourceRequirementsBuilder resources = new ResourceRequirementsBuilder();
    String cpuLimit = normalizeQuantity(adminConfig.getCpuLimit());
    if (cpuLimit != null) {
      Quantity cpu = new Quantity(cpuLimit);
      resources.addToRequests("cpu", cpu);
      resources.addToLimits("cpu", cpu);
    }

    String memoryLimit = normalizeQuantity(adminConfig.getMemoryLimit());
    if (memoryLimit != null) {
      Quantity memory = new Quantity(memoryLimit);
      resources.addToRequests("memory", memory);
      resources.addToLimits("memory", memory);
    }

    return resources.build();
  }

  private String normalizeQuantity(String quantity) {
    if (quantity == null || quantity.isBlank()) {
      return null;
    }
    return quantity.trim();
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
