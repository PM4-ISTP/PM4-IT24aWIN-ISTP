package com.pm4.istp.challengepod.controllers;

import com.pm4.istp.challengepod.dto.PodStatusResponse;
import com.pm4.istp.challengepod.dto.RunningPodResponse;
import com.pm4.istp.challengepod.services.LabPodService;
import com.pm4.istp.shared.util.JwtUtil;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/lab-pods")
public class LabPodController {

  private final LabPodService labPodService;

  public LabPodController(@NonNull LabPodService labPodService) {
    this.labPodService = labPodService;
  }

  @GetMapping
  public ResponseEntity<List<RunningPodResponse>> listMyPods(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = JwtUtil.parseUserId(jwt);
    return ResponseEntity.ok(labPodService.listPods(userId));
  }

  @PostMapping("/{labId}")
  public ResponseEntity<PodStatusResponse> startPod(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID labId) {
    UUID userId = JwtUtil.parseUserId(jwt);
    Pair<PodStatusResponse, Boolean> result = labPodService.startPod(userId, labId);
    HttpStatus status =
        Boolean.TRUE.equals(result.getSecond()) ? HttpStatus.CREATED : HttpStatus.OK;
    return ResponseEntity.status(status).body(result.getFirst());
  }

  @GetMapping("/{labId}")
  public ResponseEntity<PodStatusResponse> getPod(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID labId) {
    UUID userId = JwtUtil.parseUserId(jwt);
    PodStatusResponse response = labPodService.getPod(userId, labId);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{labId}")
  public ResponseEntity<Void> stopPod(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID labId) {
    UUID userId = JwtUtil.parseUserId(jwt);
    boolean deleted = labPodService.deletePod(userId, labId);
    if (!deleted) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.noContent().build();
  }
}
