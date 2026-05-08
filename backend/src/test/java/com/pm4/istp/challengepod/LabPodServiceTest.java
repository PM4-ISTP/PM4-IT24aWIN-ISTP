package com.pm4.istp.challengepod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.admin.db.AdminConfig;
import com.pm4.istp.admin.services.AdminConfigurationService;
import com.pm4.istp.challengepod.dto.PodStatusEnum;
import com.pm4.istp.challengepod.dto.PodStatusResponse;
import com.pm4.istp.challengepod.events.KubeconfigChangedEvent;
import com.pm4.istp.challengepod.exceptions.LabPodException;
import com.pm4.istp.challengepod.services.LabPodService;
import com.pm4.istp.course.db.entities.Lab;
import com.pm4.istp.course.exceptions.LabAccessDeniedException;
import com.pm4.istp.course.exceptions.LabNotFoundException;
import com.pm4.istp.course.repositories.CourseLabRepository;
import com.pm4.istp.course.services.LabService;
import com.pm4.istp.course.services.DockerImageAvailabilityService;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.ContainerStatusBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LabPodServiceTest {

    @Mock
    private AdminConfigurationService adminConfigurationService;

    @Mock
    private LabService labService;

    @Mock
    private DockerImageAvailabilityService dockerImageAvailabilityService;

    @Mock
    private CourseLabRepository courseLabRepository;

    private LabPodService service;

    private Lab buildChallenge() {
        Lab lab = new Lab();
        lab.setDockerImage("ghcr.io/pm4-istp/test:latest");
        lab.setContainerPort(8080);
        return lab;
    }

    @BeforeEach
    void setUp() {
        service = new LabPodService(
                adminConfigurationService,
                labService,
                dockerImageAvailabilityService,
                courseLabRepository,
                "default",
                "test.domain",
                false,
                "");
    }

    // ── startPod: early-exit paths ───────────────────────────────────────────

    @Test
    void startPod_propagatesChallengeNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();

        when(labService.getChallenge(userId, labId))
                .thenThrow(new LabNotFoundException("not found"));

        assertThatThrownBy(() -> service.startPod(userId, labId))
                .isInstanceOf(LabNotFoundException.class);
    }

    @Test
    void startPod_propagatesChallengeAccessDeniedException() {
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();

        when(labService.getChallenge(userId, labId))
                .thenThrow(new LabAccessDeniedException("denied"));

        assertThatThrownBy(() -> service.startPod(userId, labId))
                .isInstanceOf(LabAccessDeniedException.class);
    }

    @Test
    void startPod_throwsLabPodException_whenNoAdminConfig() {
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();

        // lab check passes
        when(labService.getChallenge(userId, labId)).thenReturn(buildChallenge());
        // admin config missing
        when(adminConfigurationService.getAdminConfiguration()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startPod(userId, labId))
                .isInstanceOf(LabPodException.class)
                .hasMessageContaining("No admin configuration found");
    }

    // ── Client cache: onKubeconfigChanged ────────────────────────────────────

    @Test
    void startPod_whenDeploymentAlreadyExists_returnsExistingPodAndCreatedFalse() {
        KubernetesClient client = mock(KubernetesClient.class, Mockito.RETURNS_DEEP_STUBS);
        setClientRef(client);
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        Map<String, String> labels = podLabels(userId, labId, Instant.now().getEpochSecond());
        Deployment deployment =
                new DeploymentBuilder()
                        .withNewMetadata()
                        .withName("pod-deadbeef")
                        .withLabels(labels)
                        .endMetadata()
                        .withNewStatus()
                        .withReadyReplicas(1)
                        .endStatus()
                        .build();

        when(labService.getChallenge(userId, labId)).thenReturn(buildChallenge());
        when(adminConfigurationService.getAdminConfiguration())
                .thenReturn(Optional.of(adminConfigWith("kubeconfig", 600)));
        stubFindDeployments(client, userId, labId, List.of(deployment));
        stubPodsForLabels(client, labels, List.of());
        stubIngressGetThrows(client, "pod-deadbeef-ingress");

        var result = service.startPod(userId, labId);

        assertThat(result.getSecond()).isFalse();
        assertThat(result.getFirst().status()).isEqualTo(PodStatusEnum.RUNNING);
        assertThat(result.getFirst().podName()).isEqualTo("pod-deadbeef");
        assertThat(result.getFirst().appUrl()).isEqualTo("http://app-deadbeef.test.domain");
        verify(dockerImageAvailabilityService).assertImageExists("ghcr.io/pm4-istp/test:latest");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void startPod_whenNoDeploymentExists_createsResourcesAndMarksCreatedTrue() {
        KubernetesClient client = mock(KubernetesClient.class, Mockito.RETURNS_DEEP_STUBS);
        setClientRef(client);
        NonNamespaceOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> deploymentOperation =
                mock(NonNamespaceOperation.class, Mockito.RETURNS_DEEP_STUBS);
        NonNamespaceOperation serviceOperation =
                mock(NonNamespaceOperation.class, Mockito.RETURNS_DEEP_STUBS);
        NonNamespaceOperation ingressOperation =
                mock(NonNamespaceOperation.class, Mockito.RETURNS_DEEP_STUBS);
        RollableScalableResource deploymentResource = mock(RollableScalableResource.class);
        ServiceResource serviceResource = mock(ServiceResource.class);
        Resource ingressResource = mock(Resource.class);
        DeploymentList emptyList = new DeploymentList();
        emptyList.setItems(List.of());

        when(client.apps().deployments().inNamespace("default")).thenReturn(deploymentOperation);
        when(deploymentOperation.withLabel("app", "istp-lab-pod")).thenReturn(deploymentOperation);
        when(deploymentOperation.withLabel(Mockito.eq("istp.pm4.ch/user-id"), Mockito.anyString()))
                .thenReturn(deploymentOperation);
        when(deploymentOperation.withLabel(Mockito.eq("istp.pm4.ch/lab-id"), Mockito.anyString()))
                .thenReturn(deploymentOperation);
        when(deploymentOperation.list()).thenReturn(emptyList);
        when(deploymentOperation.resource(Mockito.any())).thenReturn(deploymentResource);
        when(client.services().inNamespace("default")).thenReturn(serviceOperation);
        when(serviceOperation.resource(Mockito.any())).thenReturn(serviceResource);
        when(client.network().v1().ingresses().inNamespace("default")).thenReturn(ingressOperation);
        when(ingressOperation.resource(Mockito.any())).thenReturn(ingressResource);

        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        when(labService.getChallenge(userId, labId)).thenReturn(buildChallenge());
        when(adminConfigurationService.getAdminConfiguration())
                .thenReturn(Optional.of(adminConfigWith("kubeconfig", 900)));

        var result = service.startPod(userId, labId);

        assertThat(result.getSecond()).isTrue();
        assertThat(result.getFirst().status()).isEqualTo(PodStatusEnum.PROVISIONING);
        assertThat(result.getFirst().podName()).startsWith("pod-");
        assertThat(result.getFirst().appUrl()).startsWith("http://app-");
        verify(deploymentResource).create();
        verify(serviceResource).create();
        verify(ingressResource).create();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void createResources_createsOnlyAppContainerAppPortAndAppIngressRule() {
        KubernetesClient client = mock(KubernetesClient.class, Mockito.RETURNS_DEEP_STUBS);
        NonNamespaceOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> deploymentOperation =
                mock(NonNamespaceOperation.class, Mockito.RETURNS_DEEP_STUBS);
        NonNamespaceOperation serviceOperation =
                mock(NonNamespaceOperation.class, Mockito.RETURNS_DEEP_STUBS);
        NonNamespaceOperation ingressOperation =
                mock(NonNamespaceOperation.class, Mockito.RETURNS_DEEP_STUBS);
        RollableScalableResource deploymentResource = mock(RollableScalableResource.class);
        ServiceResource serviceResource = mock(ServiceResource.class);
        Resource ingressResource = mock(Resource.class);
        when(client.apps().deployments().inNamespace("default")).thenReturn(deploymentOperation);
        when(client.services().inNamespace("default")).thenReturn(serviceOperation);
        when(client.network().v1().ingresses().inNamespace("default")).thenReturn(ingressOperation);
        when(deploymentOperation.resource(Mockito.any())).thenReturn(deploymentResource);
        when(serviceOperation.resource(Mockito.any())).thenReturn(serviceResource);
        when(ingressOperation.resource(Mockito.any())).thenReturn(ingressResource);
        setClientRef(client);

        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        AdminConfig cfg = adminConfigWith("kubeconfig", 1800);
        cfg.setCpuLimit("500m");
        cfg.setMemoryLimit("256Mi");
        cfg.setImagePullSecretName("ghcr-pull-secret");

        PodStatusResponse response =
                ReflectionTestUtils.invokeMethod(
                        service,
                        "createResources",
                        userId,
                        labId,
                        "pod-12345678",
                        cfg,
                        buildChallenge());

        ArgumentCaptor<Deployment> deploymentCaptor = ArgumentCaptor.forClass(Deployment.class);
        verify(deploymentOperation).resource(deploymentCaptor.capture());
        assertCreatedDeployment(deploymentCaptor.getValue());

        ArgumentCaptor<Service> serviceCaptor = ArgumentCaptor.forClass(Service.class);
        verify(serviceOperation).resource(serviceCaptor.capture());
        assertCreatedService(serviceCaptor.getValue());

        ArgumentCaptor<Ingress> ingressCaptor = ArgumentCaptor.forClass(Ingress.class);
        verify(ingressOperation).resource(ingressCaptor.capture());
        assertCreatedIngress(ingressCaptor.getValue());
        assertCreatedResponse(response);
    }

    private void assertCreatedDeployment(Deployment deployment) {
        assertThat(deployment.getSpec().getTemplate().getSpec().getContainers())
                .singleElement()
                .satisfies(
                        container -> {
                            assertThat(container.getName()).isEqualTo("app");
                            assertThat(container.getImage()).isEqualTo("ghcr.io/pm4-istp/test:latest");
                            assertThat(container.getPorts())
                                    .singleElement()
                                    .satisfies(port -> assertThat(port.getContainerPort()).isEqualTo(8080));
                            assertThat(container.getResources().getLimits()).containsKeys("cpu", "memory");
                            assertThat(container.getSecurityContext()).isNull();
                        });
        assertThat(deployment.getSpec().getTemplate().getSpec().getAutomountServiceAccountToken())
                .isFalse();
        assertThat(deployment.getSpec().getTemplate().getSpec().getImagePullSecrets())
                .singleElement()
                .satisfies(secret -> assertThat(secret.getName()).isEqualTo("ghcr-pull-secret"));
        assertThat(deployment.getMetadata().getAnnotations()).isNullOrEmpty();
    }

    private void assertCreatedService(Service service) {
        assertThat(service.getSpec().getPorts())
                .singleElement()
                .satisfies(
                        port -> {
                            assertThat(port.getName()).isEqualTo("app-port");
                            assertThat(port.getPort()).isEqualTo(80);
                            assertThat(port.getTargetPort().getIntVal()).isEqualTo(8080);
                        });
    }

    private void assertCreatedIngress(Ingress ingress) {
        assertThat(ingress.getSpec().getRules())
                .singleElement()
                .satisfies(
                        rule -> {
                            assertThat(rule.getHost()).isEqualTo("app-12345678.test.domain");
                            assertThat(rule.getHttp().getPaths())
                                    .singleElement()
                                    .satisfies(
                                            path ->
                                                    assertThat(path.getBackend().getService().getPort().getNumber())
                                                            .isEqualTo(80));
                        });
    }

    private void assertCreatedResponse(PodStatusResponse response) {
        assertThat(response.status()).isEqualTo(PodStatusEnum.PROVISIONING);
        assertThat(response.appUrl()).isEqualTo("http://app-12345678.test.domain");
        assertThat(response.terminalUrl()).isNull();
        assertThat(response.terminalPassword()).isNull();
    }

    @Test
    void onKubeconfigChanged_invalidatesAndClosesExistingClient() {
        KubernetesClient mockClient = mock(KubernetesClient.class);
        setClientRef(mockClient);

        assertThatCode(() -> service.onKubeconfigChanged(new KubeconfigChangedEvent()))
                .doesNotThrowAnyException();

        verify(mockClient).close();
        assertClientRefIsNull();
    }

    @Test
    void onKubeconfigChanged_doesNotThrow_whenNoClientCached() {
        // clientRef is null by default — should be a no-op
        assertThatCode(() -> service.onKubeconfigChanged(new KubeconfigChangedEvent()))
                .doesNotThrowAnyException();
    }

    // ── Client cache: shutdown ───────────────────────────────────────────────

    @Test
    void getPod_returnsNotFound_whenNoDeploymentExists() {
        KubernetesClient client = mock(KubernetesClient.class, Mockito.RETURNS_DEEP_STUBS);
        setClientRef(client);
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        stubFindDeployments(client, userId, labId, List.of());

        PodStatusResponse response = service.getPod(userId, labId);

        assertThat(response.status()).isEqualTo(PodStatusEnum.NOT_FOUND);
        assertThat(response.podName()).isNull();
    }

    @Test
    void getPod_buildsRunningResponseFromDeploymentIngressAndAdminTtl() {
        KubernetesClient client = mock(KubernetesClient.class, Mockito.RETURNS_DEEP_STUBS);
        setClientRef(client);
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        long createdAt = Instant.now().minusSeconds(60).getEpochSecond();
        Map<String, String> labels = podLabels(userId, labId, createdAt);
        Deployment deployment =
                new DeploymentBuilder()
                        .withNewMetadata()
                        .withName("pod-abcdef12")
                        .withLabels(labels)
                        .endMetadata()
                        .withNewStatus()
                        .withReadyReplicas(1)
                        .endStatus()
                        .build();
        Ingress ingress =
                new IngressBuilder()
                        .withNewSpec()
                        .addNewRule()
                        .withHost("custom.example.test")
                        .withNewHttp()
                        .addNewPath()
                        .withNewBackend()
                        .withNewService()
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
        AdminConfig config = adminConfigWith("kubeconfig", 120);

        stubFindDeployments(client, userId, labId, List.of(deployment));
        stubPodsForLabels(client, labels, List.of());
        stubIngressGet(client, "pod-abcdef12-ingress", ingress);
        when(adminConfigurationService.getAdminConfiguration()).thenReturn(Optional.of(config));

        PodStatusResponse response = service.getPod(userId, labId);

        assertThat(response.status()).isEqualTo(PodStatusEnum.RUNNING);
        assertThat(response.podName()).isEqualTo("pod-abcdef12");
        assertThat(response.appUrl()).isEqualTo("http://custom.example.test");
        assertThat(response.createdAt()).isEqualTo(Instant.ofEpochSecond(createdAt));
        assertThat(response.expiresAt()).isEqualTo(Instant.ofEpochSecond(createdAt + 120));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPod_fallsBackToGeneratedHttpsHost_whenIngressCannotBeInspected() {
        LabPodService tlsService =
                new LabPodService(
                        adminConfigurationService,
                        labService,
                        dockerImageAvailabilityService,
                        courseLabRepository,
                        "default",
                        "test.domain",
                        true,
                        " Team Alpha! ");
        KubernetesClient client = mock(KubernetesClient.class, Mockito.RETURNS_DEEP_STUBS);
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        Map<String, String> labels = podLabels(userId, labId, Instant.now().getEpochSecond());
        Deployment deployment =
                new DeploymentBuilder()
                        .withNewMetadata()
                        .withName("pod-feedbeef")
                        .withLabels(labels)
                        .endMetadata()
                        .build();
        AtomicReference<KubernetesClient> ref =
                (AtomicReference<KubernetesClient>) ReflectionTestUtils.getField(tlsService, "clientRef");
        assert ref != null;
        ref.set(client);
        stubFindDeployments(client, userId, labId, List.of(deployment));
        stubPodsForLabels(client, labels, List.of());
        stubIngressGetThrows(client, "pod-feedbeef-ingress");
        when(adminConfigurationService.getAdminConfiguration()).thenReturn(Optional.empty());

        PodStatusResponse response = tlsService.getPod(userId, labId);

        assertThat(response.status()).isEqualTo(PodStatusEnum.PROVISIONING);
        assertThat(response.appUrl()).isEqualTo("https://app-team-alpha-feedbeef.test.domain");
    }

    @Test
    void listPods_returnsCurrentUserDeploymentsWithCourseAndLabMetadata() {
        KubernetesClient client = mock(KubernetesClient.class, Mockito.RETURNS_DEEP_STUBS);
        setClientRef(client);
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        long createdAt = Instant.now().minusSeconds(30).getEpochSecond();
        Map<String, String> labels = podLabels(userId, labId, createdAt);
        Deployment deployment =
                new DeploymentBuilder()
                        .withNewMetadata()
                        .withName("pod-cafebabe")
                        .withLabels(labels)
                        .endMetadata()
                        .withNewStatus()
                        .withReadyReplicas(1)
                        .endStatus()
                        .build();
        AdminConfig config = adminConfigWith("kubeconfig", 900);

        stubFindDeployments(client, userId, List.of(deployment));
        stubPodsForLabels(client, labels, List.of());
        stubIngressGetThrows(client, "pod-cafebabe-ingress");
        when(adminConfigurationService.getAdminConfiguration()).thenReturn(Optional.of(config));
        when(courseLabRepository.findEnrolledCourseLabSummariesForUserAndLab(userId, labId))
                .thenReturn(
                        List.<Object[]>of(
                                new Object[] {courseId, "Course title", labId, "Lab title"}));

        var pods = service.listPods(userId);

        assertThat(pods).singleElement()
                .satisfies(
                        pod -> {
                            assertThat(pod.labId()).isEqualTo(labId);
                            assertThat(pod.labTitle()).isEqualTo("Lab title");
                            assertThat(pod.courseId()).isEqualTo(courseId);
                            assertThat(pod.courseTitle()).isEqualTo("Course title");
                            assertThat(pod.pod().status()).isEqualTo(PodStatusEnum.RUNNING);
                            assertThat(pod.pod().appUrl()).isEqualTo("http://app-cafebabe.test.domain");
                        });
    }

    @Test
    void deletePod_returnsFalse_whenNoDeploymentExists() {
        KubernetesClient client = mock(KubernetesClient.class, Mockito.RETURNS_DEEP_STUBS);
        setClientRef(client);
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        stubFindDeployments(client, userId, labId, List.of());

        assertThat(service.deletePod(userId, labId)).isFalse();
    }

    @Test
    void deletePod_rejectsDeploymentWhoseLabelsDoNotMatchCaller() {
        KubernetesClient client = mock(KubernetesClient.class, Mockito.RETURNS_DEEP_STUBS);
        setClientRef(client);
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        Deployment deployment =
                new DeploymentBuilder()
                        .withNewMetadata()
                        .withName("pod-badlabel")
                        .addToLabels("istp.pm4.ch/user-id", UUID.randomUUID().toString())
                        .addToLabels("istp.pm4.ch/lab-id", labId.toString())
                        .endMetadata()
                        .build();
        stubFindDeployments(client, userId, labId, List.of(deployment));

        assertThatThrownBy(() -> service.deletePod(userId, labId))
                .isInstanceOf(LabPodException.class)
                .hasMessageContaining("Ownership check failed");
    }

    @Test
    void deletePod_deletesDeploymentServiceAndIngressByInstanceName() {
        KubernetesClient client = mock(KubernetesClient.class, Mockito.RETURNS_DEEP_STUBS);
        setClientRef(client);
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        Deployment deployment =
                new DeploymentBuilder()
                        .withNewMetadata()
                        .withName("pod-1234abcd")
                        .withLabels(podLabels(userId, labId, Instant.now().getEpochSecond()))
                        .endMetadata()
                        .build();
        NonNamespaceOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> deploymentOperation =
                stubFindDeployments(client, userId, labId, List.of(deployment));
        RollableScalableResource<Deployment> deploymentResource = mock(RollableScalableResource.class);
        when(deploymentOperation.withName("pod-1234abcd")).thenReturn(deploymentResource);
        stubDeleteResources(client, "pod-1234abcd");

        assertThat(service.deletePod(userId, labId)).isTrue();

        verify(deploymentResource).delete();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void reapExpiredPods_deletesOnlyDeploymentsOlderThanTtl() {
        KubernetesClient client = mock(KubernetesClient.class, Mockito.RETURNS_DEEP_STUBS);
        setClientRef(client);
        NonNamespaceOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> deploymentOperation =
                mock(NonNamespaceOperation.class, Mockito.RETURNS_DEEP_STUBS);
        long now = Instant.now().getEpochSecond();
        Deployment expired =
                deploymentForReap("pod-expired", Map.of("istp.pm4.ch/created-at-epoch", String.valueOf(now - 500)));
        Deployment fresh =
                deploymentForReap("pod-fresh", Map.of("istp.pm4.ch/created-at-epoch", String.valueOf(now)));
        Deployment missingLabel = deploymentForReap("pod-missing", Map.of());
        when(client.apps().deployments().inNamespace("default")).thenReturn(deploymentOperation);
        when(deploymentOperation.withLabel("app", "istp-lab-pod")).thenReturn(deploymentOperation);
        DeploymentList deploymentList = new DeploymentList();
        deploymentList.setItems(List.of(expired, fresh, missingLabel));
        RollableScalableResource<Deployment> expiredResource = mock(RollableScalableResource.class);
        when(deploymentOperation.list()).thenReturn(deploymentList);
        when(deploymentOperation.withName("pod-expired")).thenReturn(expiredResource);

        service.reapExpiredPods(300);

        verify(expiredResource).delete();
    }

    @Test
    void shutdown_closesAndClearsClient() {
        KubernetesClient mockClient = mock(KubernetesClient.class);
        setClientRef(mockClient);

        assertThatCode(() -> service.shutdown()).doesNotThrowAnyException();

        verify(mockClient).close();
        assertClientRefIsNull();
    }

    @Test
    void shutdown_doesNotThrow_whenNoClientCached() {
        assertThatCode(() -> service.shutdown()).doesNotThrowAnyException();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @Test
    void containerHasFailed_returnsTrueForImagePullWaitingReasons() {
        ContainerStatus status = new ContainerStatusBuilder()
                .withNewState()
                .withNewWaiting()
                .withReason("ImagePullBackOff")
                .endWaiting()
                .endState()
                .build();

        Boolean failed = ReflectionTestUtils.invokeMethod(service, "containerHasFailed", status);

        assertThat(failed).isTrue();
    }

    @Test
    void containerHasFailed_returnsTrueForNonZeroExitCode() {
        ContainerStatus status = new ContainerStatusBuilder()
                .withNewState()
                .withNewTerminated()
                .withExitCode(126)
                .endTerminated()
                .endState()
                .build();

        Boolean failed = ReflectionTestUtils.invokeMethod(service, "containerHasFailed", status);

        assertThat(failed).isTrue();
    }

    @Test
    void containerHasFailed_returnsFalseForCleanTermination() {
        ContainerStatus status = new ContainerStatusBuilder()
                .withNewState()
                .withNewTerminated()
                .withExitCode(0)
                .endTerminated()
                .endState()
                .build();

        Boolean failed = ReflectionTestUtils.invokeMethod(service, "containerHasFailed", status);

        assertThat(failed).isFalse();
    }

    @SuppressWarnings("unchecked")
    private void setClientRef(KubernetesClient client) {
        AtomicReference<KubernetesClient> ref =
                (AtomicReference<KubernetesClient>) ReflectionTestUtils.getField(service, "clientRef");
        assert ref != null;
        ref.set(client);
    }

    @SuppressWarnings("unchecked")
    private void assertClientRefIsNull() {
        AtomicReference<KubernetesClient> ref =
                (AtomicReference<KubernetesClient>) ReflectionTestUtils.getField(service, "clientRef");
        assert ref != null;
        assert ref.get() == null : "Expected clientRef to be null after invalidation";
    }

    // ── AdminConfig helper ───────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private NonNamespaceOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> stubFindDeployments(
            KubernetesClient client, UUID userId, UUID labId, List<Deployment> deployments) {
        NonNamespaceOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> deploymentOperation =
                mock(NonNamespaceOperation.class, Mockito.RETURNS_DEEP_STUBS);
        when(client.apps().deployments().inNamespace("default")).thenReturn(deploymentOperation);
        when(deploymentOperation.withLabel("app", "istp-lab-pod")).thenReturn(deploymentOperation);
        when(deploymentOperation.withLabel("istp.pm4.ch/user-id", userId.toString()))
                .thenReturn(deploymentOperation);
        when(deploymentOperation.withLabel("istp.pm4.ch/lab-id", labId.toString()))
                .thenReturn(deploymentOperation);
        DeploymentList deploymentList = new DeploymentList();
        deploymentList.setItems(deployments);
        when(deploymentOperation.list()).thenReturn(deploymentList);
        return deploymentOperation;
    }

    @SuppressWarnings("unchecked")
    private NonNamespaceOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> stubFindDeployments(
            KubernetesClient client, UUID userId, List<Deployment> deployments) {
        NonNamespaceOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> deploymentOperation =
                mock(NonNamespaceOperation.class, Mockito.RETURNS_DEEP_STUBS);
        when(client.apps().deployments().inNamespace("default")).thenReturn(deploymentOperation);
        when(deploymentOperation.withLabel("app", "istp-lab-pod")).thenReturn(deploymentOperation);
        when(deploymentOperation.withLabel("istp.pm4.ch/user-id", userId.toString()))
                .thenReturn(deploymentOperation);
        DeploymentList deploymentList = new DeploymentList();
        deploymentList.setItems(deployments);
        when(deploymentOperation.list()).thenReturn(deploymentList);
        return deploymentOperation;
    }

    @SuppressWarnings("unchecked")
    private void stubPodsForLabels(KubernetesClient client, Map<String, String> labels, List<Pod> pods) {
        NonNamespaceOperation<Pod, PodList, PodResource> podOperation =
                mock(NonNamespaceOperation.class, Mockito.RETURNS_DEEP_STUBS);
        PodList podList = new PodList();
        podList.setItems(pods);
        when(client.pods().inNamespace("default")).thenReturn(podOperation);
        when(podOperation.withLabels(labels)).thenReturn(podOperation);
        when(podOperation.list()).thenReturn(podList);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubDeleteResources(KubernetesClient client, String instanceName) {
        NonNamespaceOperation serviceOperation =
                mock(NonNamespaceOperation.class, Mockito.RETURNS_DEEP_STUBS);
        NonNamespaceOperation ingressOperation =
                mock(NonNamespaceOperation.class, Mockito.RETURNS_DEEP_STUBS);
        ServiceResource serviceResource = mock(ServiceResource.class);
        Resource ingressResource = mock(Resource.class);
        when(client.services().inNamespace("default")).thenReturn(serviceOperation);
        when(serviceOperation.withName(instanceName + "-svc")).thenReturn(serviceResource);
        when(client.network().v1().ingresses().inNamespace("default")).thenReturn(ingressOperation);
        when(ingressOperation.withName(instanceName + "-ingress")).thenReturn(ingressResource);
    }

    @SuppressWarnings("unchecked")
    private void stubIngressGet(KubernetesClient client, String ingressName, Ingress ingress) {
        NonNamespaceOperation<Ingress, IngressList, Resource<Ingress>> ingressOperation =
                mock(NonNamespaceOperation.class, Mockito.RETURNS_DEEP_STUBS);
        Resource<Ingress> ingressResource = mock(Resource.class);
        when(client.network().v1().ingresses().inNamespace("default")).thenReturn(ingressOperation);
        when(ingressOperation.withName(ingressName)).thenReturn(ingressResource);
        when(ingressResource.get()).thenReturn(ingress);
    }

    @SuppressWarnings("unchecked")
    private void stubIngressGetThrows(KubernetesClient client, String ingressName) {
        NonNamespaceOperation<Ingress, IngressList, Resource<Ingress>> ingressOperation =
                mock(NonNamespaceOperation.class, Mockito.RETURNS_DEEP_STUBS);
        Resource<Ingress> ingressResource = mock(Resource.class);
        when(client.network().v1().ingresses().inNamespace("default")).thenReturn(ingressOperation);
        when(ingressOperation.withName(ingressName)).thenReturn(ingressResource);
        when(ingressResource.get()).thenThrow(new RuntimeException("api unavailable"));
    }

    private Map<String, String> podLabels(UUID userId, UUID labId, long createdAtEpoch) {
        return Map.of(
                "app",
                "istp-lab-pod",
                "istp.pm4.ch/user-id",
                userId.toString(),
                "istp.pm4.ch/lab-id",
                labId.toString(),
                "istp.pm4.ch/created-at-epoch",
                String.valueOf(createdAtEpoch));
    }

    private Deployment deploymentForReap(String name, Map<String, String> labels) {
        return new DeploymentBuilder()
                .withNewMetadata()
                .withName(name)
                .withLabels(labels)
                .endMetadata()
                .build();
    }

    private AdminConfig adminConfigWith(String kubeconfig, int ttl) {
        AdminConfig cfg = new AdminConfig();
        cfg.setId(UUID.randomUUID());
        cfg.setKubeconfig(kubeconfig);
        cfg.setPodTtlSeconds(ttl);
        return cfg;
    }
}
