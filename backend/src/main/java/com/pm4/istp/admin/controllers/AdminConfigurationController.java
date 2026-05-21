package com.pm4.istp.admin.controllers;

import com.pm4.istp.admin.dto.AdminConfigRequest;
import com.pm4.istp.admin.dto.AdminConfigResponse;
import com.pm4.istp.admin.services.AdminConfigurationService;
import com.pm4.istp.shared.dto.ErrorDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(
    name = "Admin Configuration",
    description = "Administrative endpoints for the Kubernetes/platform configuration")
@RestController
@RequestMapping(path = "/api/admin/config")
public class AdminConfigurationController {

  private final AdminConfigurationService adminConfigurationService;

  private static final long MAX_FILE_SIZE = 1_048_576;

  public AdminConfigurationController(
      @NonNull AdminConfigurationService adminConfigurationService) {
    this.adminConfigurationService = adminConfigurationService;
  }

  @Operation(
      summary = "Create the platform configuration",
      description =
          "Uploads a base64-encoded kubeconfig together with optional pod resource limits and"
              + " stores the initial admin configuration.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Configuration created successfully",
            content = @Content(schema = @Schema(implementation = AdminConfigResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Missing kubeconfig, invalid base64, or file exceeds the 1 MB limit",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PostMapping
  public ResponseEntity<AdminConfigResponse> uploadAndStoreAdminConfig(
      @Valid @RequestBody AdminConfigRequest request) {

    if (request.getKubeconfig() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kubeconfig is required.");
    }

    byte[] kubeconfigBytes;
    try {
      kubeconfigBytes = Base64.getDecoder().decode(request.getKubeconfig());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kubeconfig is not valid base64.");
    }

    if (kubeconfigBytes.length > MAX_FILE_SIZE) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Kubeconfig file size exceeds 1 MB limit.");
    }

    com.pm4.istp.admin.db.AdminConfig adminConfig =
        adminConfigurationService.createConfiguration(
            kubeconfigBytes,
            request.getCpuLimit(),
            request.getMemoryLimit(),
            request.getImagePullSecretName(),
            request.getPodTtlSeconds());

    AdminConfigResponse response =
        new AdminConfigResponse(
            true,
            adminConfig.getCpuLimit(),
            adminConfig.getMemoryLimit(),
            adminConfig.getImagePullSecretName(),
            adminConfig.getPodTtlSeconds(),
            adminConfig.getUpdatedAt());

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(
      summary = "Get the platform configuration",
      description =
          "Returns the current admin configuration. When none is stored yet, a response with"
              + " kubeconfigUploaded=false and empty limits is returned.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Configuration retrieved successfully",
            content = @Content(schema = @Schema(implementation = AdminConfigResponse.class)))
      })
  @GetMapping
  public ResponseEntity<AdminConfigResponse> getAdminConfig() {
    Optional<com.pm4.istp.admin.db.AdminConfig> config =
        adminConfigurationService.getAdminConfiguration();
    if (config.isPresent()) {
      com.pm4.istp.admin.db.AdminConfig adminConfig = config.get();
      AdminConfigResponse response =
          new AdminConfigResponse(
              true,
              adminConfig.getCpuLimit(),
              adminConfig.getMemoryLimit(),
              adminConfig.getImagePullSecretName(),
              adminConfig.getPodTtlSeconds(),
              adminConfig.getUpdatedAt());
      return ResponseEntity.ok(response);
    } else {
      AdminConfigResponse response = new AdminConfigResponse(false, null, null, null, null, null);
      return ResponseEntity.ok(response);
    }
  }

  @Operation(
      summary = "Update the platform configuration",
      description =
          "Updates the pod resource limits and optionally replaces the kubeconfig. Omitting the"
              + " kubeconfig keeps the previously stored one.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Configuration updated successfully",
            content = @Content(schema = @Schema(implementation = AdminConfigResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid base64 kubeconfig or file exceeds the 1 MB limit",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PutMapping
  public ResponseEntity<AdminConfigResponse> updateAdminConfig(
      @Valid @RequestBody AdminConfigRequest request) {

    byte[] kubeconfigBytes = null;
    if (request.getKubeconfig() != null) {
      try {
        kubeconfigBytes = Base64.getDecoder().decode(request.getKubeconfig());
      } catch (IllegalArgumentException e) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Kubeconfig is not valid base64.");
      }

      if (kubeconfigBytes.length > MAX_FILE_SIZE) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Kubeconfig file size exceeds 1 MB limit.");
      }
    }

    com.pm4.istp.admin.db.AdminConfig adminConfig =
        adminConfigurationService.updateConfiguration(
            kubeconfigBytes,
            request.getCpuLimit(),
            request.getMemoryLimit(),
            request.getImagePullSecretName(),
            request.getPodTtlSeconds());

    AdminConfigResponse response =
        new AdminConfigResponse(
            true,
            adminConfig.getCpuLimit(),
            adminConfig.getMemoryLimit(),
            adminConfig.getImagePullSecretName(),
            adminConfig.getPodTtlSeconds(),
            adminConfig.getUpdatedAt());

    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Delete the platform configuration",
      description = "Removes the stored admin configuration, including the kubeconfig.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Configuration deleted successfully")
      })
  @DeleteMapping
  public ResponseEntity<Map<String, String>> deleteAdminConfig() {
    adminConfigurationService.deleteAdminConfiguration();
    return ResponseEntity.ok(Map.of("message", "Admin configuration deleted successfully"));
  }
}
