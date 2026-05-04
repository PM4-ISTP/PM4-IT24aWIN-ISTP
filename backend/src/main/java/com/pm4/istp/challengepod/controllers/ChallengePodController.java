package com.pm4.istp.challengepod.controllers;

import com.pm4.istp.challengepod.dto.PodStatusResponse;
import com.pm4.istp.challengepod.services.ChallengePodService;
import com.pm4.istp.shared.util.JwtUtil;
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
@RequestMapping("/api/v1/challenge-pods")
public class ChallengePodController {

  private final ChallengePodService challengePodService;

  public ChallengePodController(@NonNull ChallengePodService challengePodService) {
    this.challengePodService = challengePodService;
  }

  @PostMapping("/{challengeId}")
  public ResponseEntity<PodStatusResponse> startPod(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID challengeId) {
    UUID userId = JwtUtil.parseUserId(jwt);
    Pair<PodStatusResponse, Boolean> result = challengePodService.startPod(userId, challengeId);
    HttpStatus status =
        Boolean.TRUE.equals(result.getSecond()) ? HttpStatus.CREATED : HttpStatus.OK;
    return ResponseEntity.status(status).body(result.getFirst());
  }

  @GetMapping("/{challengeId}")
  public ResponseEntity<PodStatusResponse> getPod(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID challengeId) {
    UUID userId = JwtUtil.parseUserId(jwt);
    PodStatusResponse response = challengePodService.getPod(userId, challengeId);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{challengeId}")
  public ResponseEntity<Void> stopPod(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID challengeId) {
    UUID userId = JwtUtil.parseUserId(jwt);
    boolean deleted = challengePodService.deletePod(userId, challengeId);
    if (!deleted) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.noContent().build();
  }
}
