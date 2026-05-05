package com.pm4.istp.challengepod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.challengepod.controllers.LabPodController;
import com.pm4.istp.challengepod.dto.PodStatusEnum;
import com.pm4.istp.challengepod.dto.PodStatusResponse;
import com.pm4.istp.challengepod.exceptions.LabPodException;
import com.pm4.istp.challengepod.services.LabPodService;
import com.pm4.istp.course.exceptions.LabAccessDeniedException;
import com.pm4.istp.course.exceptions.LabNotFoundException;

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
class LabPodControllerTest {

    @Mock
    private LabPodService labPodService;

    @InjectMocks
    private LabPodController controller;

    private Jwt jwtFor(UUID userId) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(userId.toString());
        return jwt;
    }

    private PodStatusResponse runningResponse() {
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

    // ── POST /{labId} ──────────────────────────────────────────────────

    @Test
    void startPod_returnsCreated_whenNewPodSpawned() {
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);
        PodStatusResponse response = runningResponse();

        when(labPodService.startPod(userId, labId)).thenReturn(Pair.of(response, true));

        ResponseEntity<PodStatusResponse> result = controller.startPod(jwt, labId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(response);
        verify(labPodService).startPod(userId, labId);
    }

    @Test
    void startPod_returnsOk_whenPodAlreadyExists() {
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);
        PodStatusResponse response = runningResponse();

        when(labPodService.startPod(userId, labId)).thenReturn(Pair.of(response, false));

        ResponseEntity<PodStatusResponse> result = controller.startPod(jwt, labId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    void startPod_propagatesChallengeNotFoundException() {
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);

        when(labPodService.startPod(userId, labId))
                .thenThrow(new LabNotFoundException("not found"));

        assertThatThrownBy(() -> controller.startPod(jwt, labId))
                .isInstanceOf(LabNotFoundException.class);
    }

    @Test
    void startPod_propagatesChallengeAccessDeniedException() {
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);

        when(labPodService.startPod(userId, labId))
                .thenThrow(new LabAccessDeniedException("denied"));

        assertThatThrownBy(() -> controller.startPod(jwt, labId))
                .isInstanceOf(LabAccessDeniedException.class);
    }

    @Test
    void startPod_propagatesLabPodException() {
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);

        when(labPodService.startPod(userId, labId))
                .thenThrow(new LabPodException("k8s error"));

        assertThatThrownBy(() -> controller.startPod(jwt, labId))
                .isInstanceOf(LabPodException.class);
    }

    // ── GET /{labId} ───────────────────────────────────────────────────

    @Test
    void getPod_returnsOk_withStatusFromService() {
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);
        PodStatusResponse response = runningResponse();

        when(labPodService.getPod(userId, labId)).thenReturn(response);

        ResponseEntity<PodStatusResponse> result = controller.getPod(jwt, labId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
        verify(labPodService).getPod(userId, labId);
    }

    @Test
    void getPod_returnsOk_withNotFoundStatus_whenPodAbsent() {
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);

        when(labPodService.getPod(userId, labId)).thenReturn(PodStatusResponse.notFound());

        ResponseEntity<PodStatusResponse> result = controller.getPod(jwt, labId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().status()).isEqualTo(PodStatusEnum.NOT_FOUND);
    }

    // ── DELETE /{labId} ────────────────────────────────────────────────

    @Test
    void stopPod_returnsNoContent_whenPodDeleted() {
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);

        when(labPodService.deletePod(userId, labId)).thenReturn(true);

        ResponseEntity<Void> result = controller.stopPod(jwt, labId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(labPodService).deletePod(userId, labId);
    }

    @Test
    void stopPod_returnsNotFound_whenNoPodExists() {
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);

        when(labPodService.deletePod(userId, labId)).thenReturn(false);

        ResponseEntity<Void> result = controller.stopPod(jwt, labId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void stopPod_propagatesLabPodException() {
        UUID userId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);

        when(labPodService.deletePod(userId, labId))
                .thenThrow(new LabPodException("ownership check failed"));

        assertThatThrownBy(() -> controller.stopPod(jwt, labId))
                .isInstanceOf(LabPodException.class);
    }
}
