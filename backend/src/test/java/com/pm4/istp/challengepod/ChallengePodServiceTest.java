package com.pm4.istp.challengepod;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.admin.db.AdminConfig;
import com.pm4.istp.admin.services.AdminConfigurationService;
import com.pm4.istp.challengepod.exceptions.ChallengePodException;
import com.pm4.istp.challengepod.events.KubeconfigChangedEvent;
import com.pm4.istp.challengepod.services.ChallengePodService;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.exceptions.ChallengeAccessDeniedException;
import com.pm4.istp.course.exceptions.ChallengeNotFoundException;
import com.pm4.istp.course.services.DockerImageAvailabilityService;
import com.pm4.istp.course.services.ChallengeService;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChallengePodServiceTest {

    @Mock
    private AdminConfigurationService adminConfigurationService;

    @Mock
    private ChallengeService challengeService;

    @Mock
    private DockerImageAvailabilityService dockerImageAvailabilityService;

    private ChallengePodService service;

    private Challenge buildChallenge() {
        Challenge challenge = new Challenge();
        challenge.setDockerImage("ghcr.io/pm4-istp/test:latest");
        return challenge;
    }

    @BeforeEach
    void setUp() {
        service = new ChallengePodService(
                adminConfigurationService,
                challengeService,
                dockerImageAvailabilityService,
                "default",
                "test.domain",
                false);
    }

    // ── startPod: early-exit paths ───────────────────────────────────────────

    @Test
    void startPod_propagatesChallengeNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();

        when(challengeService.getChallenge(userId, challengeId))
                .thenThrow(new ChallengeNotFoundException("not found"));

        assertThatThrownBy(() -> service.startPod(userId, challengeId))
                .isInstanceOf(ChallengeNotFoundException.class);
    }

    @Test
    void startPod_propagatesChallengeAccessDeniedException() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();

        when(challengeService.getChallenge(userId, challengeId))
                .thenThrow(new ChallengeAccessDeniedException("denied"));

        assertThatThrownBy(() -> service.startPod(userId, challengeId))
                .isInstanceOf(ChallengeAccessDeniedException.class);
    }

    @Test
    void startPod_throwsChallengePodException_whenNoAdminConfig() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();

        // challenge check passes
        when(challengeService.getChallenge(userId, challengeId)).thenReturn(buildChallenge());
        // admin config missing
        when(adminConfigurationService.getAdminConfiguration()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startPod(userId, challengeId))
                .isInstanceOf(ChallengePodException.class)
                .hasMessageContaining("No admin configuration found");
    }

    // ── Client cache: onKubeconfigChanged ────────────────────────────────────

    @Test
    void onKubeconfigChanged_invalidatesAndClosesExistingClient() {
        KubernetesClient mockClient = mock(KubernetesClient.class);
        setClientRef(mockClient);

        service.onKubeconfigChanged(new KubeconfigChangedEvent());

        verify(mockClient).close();
        assertClientRefIsNull();
    }

    @Test
    void onKubeconfigChanged_doesNotThrow_whenNoClientCached() {
        // clientRef is null by default — should be a no-op
        service.onKubeconfigChanged(new KubeconfigChangedEvent());
    }

    // ── Client cache: shutdown ───────────────────────────────────────────────

    @Test
    void shutdown_closesAndClearsClient() {
        KubernetesClient mockClient = mock(KubernetesClient.class);
        setClientRef(mockClient);

        service.shutdown();

        verify(mockClient).close();
        assertClientRefIsNull();
    }

    @Test
    void shutdown_doesNotThrow_whenNoClientCached() {
        service.shutdown();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

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
