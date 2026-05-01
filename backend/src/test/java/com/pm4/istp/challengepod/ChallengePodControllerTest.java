package com.pm4.istp.challengepod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.challengepod.controllers.ChallengePodController;
import com.pm4.istp.challengepod.dto.PodStatusEnum;
import com.pm4.istp.challengepod.dto.PodStatusResponse;
import com.pm4.istp.challengepod.exceptions.ChallengePodException;
import com.pm4.istp.challengepod.services.ChallengePodService;
import com.pm4.istp.course.exceptions.ChallengeAccessDeniedException;
import com.pm4.istp.course.exceptions.ChallengeNotFoundException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class ChallengePodControllerTest {

    @Mock
    private ChallengePodService challengePodService;

    @InjectMocks
    private ChallengePodController controller;

    private Jwt jwtFor(UUID userId) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(userId.toString());
        return jwt;
    }

    private PodStatusResponse runningResponse(UUID challengeId) {
        Instant now = Instant.now();
        return new PodStatusResponse(
                PodStatusEnum.RUNNING,
                "pod-abc12345",
                "http://app-abc12345.test.domain",
                "http://term-abc12345.test.domain",
                "secret123",
                now,
                now.plusSeconds(3600));
    }

    // ── POST /{challengeId} ──────────────────────────────────────────────────

    @Test
    void startPod_returnsCreated_whenNewPodSpawned() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);
        PodStatusResponse response = runningResponse(challengeId);

        when(challengePodService.startPod(userId, challengeId)).thenReturn(Pair.of(response, true));

        ResponseEntity<PodStatusResponse> result = controller.startPod(jwt, challengeId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
        verify(challengePodService).startPod(userId, challengeId);
    }

    @Test
    void startPod_returnsOk_whenPodAlreadyExists() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);
        PodStatusResponse response = runningResponse(challengeId);

        when(challengePodService.startPod(userId, challengeId)).thenReturn(Pair.of(response, false));

        ResponseEntity<PodStatusResponse> result = controller.startPod(jwt, challengeId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    void startPod_propagatesChallengeNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);

        when(challengePodService.startPod(userId, challengeId))
                .thenThrow(new ChallengeNotFoundException("not found"));

        assertThatThrownBy(() -> controller.startPod(jwt, challengeId))
                .isInstanceOf(ChallengeNotFoundException.class);
    }

    @Test
    void startPod_propagatesChallengeAccessDeniedException() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);

        when(challengePodService.startPod(userId, challengeId))
                .thenThrow(new ChallengeAccessDeniedException("denied"));

        assertThatThrownBy(() -> controller.startPod(jwt, challengeId))
                .isInstanceOf(ChallengeAccessDeniedException.class);
    }

    @Test
    void startPod_propagatesChallengePodException() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);

        when(challengePodService.startPod(userId, challengeId))
                .thenThrow(new ChallengePodException("k8s error"));

        assertThatThrownBy(() -> controller.startPod(jwt, challengeId))
                .isInstanceOf(ChallengePodException.class);
    }

    // ── GET /{challengeId} ───────────────────────────────────────────────────

    @Test
    void getPod_returnsOk_withStatusFromService() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);
        PodStatusResponse response = runningResponse(challengeId);

        when(challengePodService.getPod(userId, challengeId)).thenReturn(response);

        ResponseEntity<PodStatusResponse> result = controller.getPod(jwt, challengeId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(challengePodService).getPod(userId, challengeId);
    }

    @Test
    void getPod_returnsOk_withNotFoundStatus_whenPodAbsent() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);

        when(challengePodService.getPod(userId, challengeId)).thenReturn(PodStatusResponse.notFound());

        ResponseEntity<PodStatusResponse> result = controller.getPod(jwt, challengeId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().status()).isEqualTo(PodStatusEnum.NOT_FOUND);
    }

    // ── DELETE /{challengeId} ────────────────────────────────────────────────

    @Test
    void stopPod_returnsNoContent_whenPodDeleted() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);

        when(challengePodService.deletePod(userId, challengeId)).thenReturn(true);

        ResponseEntity<Void> result = controller.stopPod(jwt, challengeId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(challengePodService).deletePod(userId, challengeId);
    }

    @Test
    void stopPod_returnsNotFound_whenNoPodExists() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);

        when(challengePodService.deletePod(userId, challengeId)).thenReturn(false);

        ResponseEntity<Void> result = controller.stopPod(jwt, challengeId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void stopPod_propagatesChallengePodException() {
        UUID userId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);

        when(challengePodService.deletePod(userId, challengeId))
                .thenThrow(new ChallengePodException("ownership check failed"));

        assertThatThrownBy(() -> controller.stopPod(jwt, challengeId))
                .isInstanceOf(ChallengePodException.class);
    }
}
