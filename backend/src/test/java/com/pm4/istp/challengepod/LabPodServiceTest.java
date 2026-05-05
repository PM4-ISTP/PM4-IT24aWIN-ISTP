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
import com.pm4.istp.course.services.LabService;
import com.pm4.istp.course.services.DockerImageAvailabilityService;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.ContainerStatusBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
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

    private LabPodService service;

    private Lab buildChallenge() {
        Lab lab = new Lab();
        lab.setDockerImage("ghcr.io/pm4-istp/test:latest");
        return lab;
    }

    @BeforeEach
    void setUp() {
        service = new LabPodService(
                adminConfigurationService,
                labService,
                dockerImageAvailabilityService,
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
    @SuppressWarnings({"rawtypes", "unchecked"})
    void createResources_createsOnlyAppContainerAppPortAndAppIngressRule() {
        KubernetesClient client = mock(KubernetesClient.class, Mockito.RETURNS_DEEP_STUBS);
        NonNamespaceOperation deploymentOperation =
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
        Deployment deployment = deploymentCaptor.getValue();

        assertThat(deployment.getSpec().getTemplate().getSpec().getContainers())
                .singleElement()
                .satisfies(
                        container -> {
                            assertThat(container.getName()).isEqualTo("app");
                            assertThat(container.getImage()).isEqualTo("ghcr.io/pm4-istp/test:latest");
                            assertThat(container.getPorts())
                                    .singleElement()
                                    .satisfies(port -> assertThat(port.getContainerPort()).isEqualTo(80));
                            assertThat(container.getResources().getLimits()).containsKeys("cpu", "memory");
                            assertThat(container.getSecurityContext()).isNull();
                        });
        assertThat(deployment.getSpec().getTemplate().getSpec().getAutomountServiceAccountToken())
                .isFalse();
        assertThat(deployment.getSpec().getTemplate().getSpec().getImagePullSecrets())
                .singleElement()
                .satisfies(secret -> assertThat(secret.getName()).isEqualTo("ghcr-pull-secret"));
        assertThat(deployment.getMetadata().getAnnotations()).isNullOrEmpty();

        ArgumentCaptor<Service> serviceCaptor = ArgumentCaptor.forClass(Service.class);
        verify(serviceOperation).resource(serviceCaptor.capture());
        assertThat(serviceCaptor.getValue().getSpec().getPorts())
                .singleElement()
                .satisfies(
                        port -> {
                            assertThat(port.getName()).isEqualTo("app-port");
                            assertThat(port.getPort()).isEqualTo(80);
                        });

        ArgumentCaptor<Ingress> ingressCaptor = ArgumentCaptor.forClass(Ingress.class);
        verify(ingressOperation).resource(ingressCaptor.capture());
        assertThat(ingressCaptor.getValue().getSpec().getRules())
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

    private AdminConfig adminConfigWith(String kubeconfig, int ttl) {
        AdminConfig cfg = new AdminConfig();
        cfg.setId(UUID.randomUUID());
        cfg.setKubeconfig(kubeconfig);
        cfg.setPodTtlSeconds(ttl);
        return cfg;
    }
}
