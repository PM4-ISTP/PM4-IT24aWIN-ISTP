package com.pm4.istp.challengepod.controllers;

import com.pm4.istp.challengepod.dto.PodStatusResponse;
import com.pm4.istp.challengepod.dto.RunningPodResponse;
import com.pm4.istp.challengepod.services.LabPodService;
import com.pm4.istp.shared.dto.ErrorDto;
import com.pm4.istp.shared.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Lab Pod", description = "Lifecycle endpoints for per-user lab pods")
@Slf4j
@RestController
@RequestMapping("/api/v1/lab-pods")
public class LabPodController {

  private final LabPodService labPodService;

  public LabPodController(@NonNull LabPodService labPodService) {
    this.labPodService = labPodService;
  }

  @Operation(
      summary = "List my running lab pods",
      description = "Returns all currently running lab pods owned by the authenticated user.")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Pods retrieved successfully")})
  @GetMapping
  public ResponseEntity<List<RunningPodResponse>> listMyPods(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = JwtUtil.parseUserId(jwt);
    return ResponseEntity.ok(labPodService.listPods(userId));
  }

  @Operation(
      summary = "Start a lab pod",
      description =
          "Starts a pod for the given lab, or returns the existing pod if one is already running.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Pod created successfully",
            content = @Content(schema = @Schema(implementation = PodStatusResponse.class))),
        @ApiResponse(
            responseCode = "200",
            description = "Pod already running",
            content = @Content(schema = @Schema(implementation = PodStatusResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Kubernetes operation failed",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PostMapping("/{labId}")
  public ResponseEntity<PodStatusResponse> startPod(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID labId) {
    UUID userId = JwtUtil.parseUserId(jwt);
    Pair<PodStatusResponse, Boolean> result = labPodService.startPod(userId, labId);
    HttpStatus status =
        Boolean.TRUE.equals(result.getSecond()) ? HttpStatus.CREATED : HttpStatus.OK;
    return ResponseEntity.status(status).body(result.getFirst());
  }

  @Operation(
      summary = "Get a lab pod status",
      description = "Returns the current status of the authenticated user's pod for the given lab.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Pod status retrieved successfully",
            content = @Content(schema = @Schema(implementation = PodStatusResponse.class)))
      })
  @GetMapping("/{labId}")
  public ResponseEntity<PodStatusResponse> getPod(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID labId) {
    UUID userId = JwtUtil.parseUserId(jwt);
    PodStatusResponse response = labPodService.getPod(userId, labId);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Extend a lab pod",
      description = "Extends the time-to-live of the authenticated user's pod for the given lab.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Pod extended successfully",
            content = @Content(schema = @Schema(implementation = PodStatusResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Kubernetes operation failed",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PostMapping("/{labId}/extend")
  public ResponseEntity<PodStatusResponse> extendPod(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID labId) {
    UUID userId = JwtUtil.parseUserId(jwt);
    PodStatusResponse response = labPodService.extendPod(userId, labId);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Stop a lab pod",
      description = "Stops and removes the authenticated user's pod for the given lab.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Pod stopped successfully"),
        @ApiResponse(responseCode = "404", description = "No pod found for the given lab")
      })
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
