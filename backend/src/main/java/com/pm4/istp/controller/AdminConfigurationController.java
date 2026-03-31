package com.pm4.istp.controller;

import com.pm4.istp.dto.AdminConfigRequest;
import com.pm4.istp.dto.AdminConfigResponse;
import com.pm4.istp.exception.StorageException;
import com.pm4.istp.service.AdminConfigurationService;
import jakarta.validation.Valid;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/admin/config")
public class AdminConfigurationController {

  private final AdminConfigurationService adminConfigurationService;

  private static final long MAX_FILE_SIZE = 1_048_576;

  public AdminConfigurationController(
      @NonNull AdminConfigurationService adminConfigurationService) {
    this.adminConfigurationService = adminConfigurationService;
  }

  /**
   * Uploads a kubeconfig file (base64-encoded) and stores admin configuration in the database.
   *
   * @param request the admin config request containing base64-encoded kubeconfig and optional limits
   * @return ResponseEntity containing the stored AdminConfigResponse
   */
  @PostMapping
  public ResponseEntity<?> uploadAndStoreAdminConfig(
      @Valid @RequestBody AdminConfigRequest request) {

    if (request.getKubeconfig() == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Kubeconfig is required.");
    }

    byte[] kubeconfigBytes;
    try {
      kubeconfigBytes = Base64.getDecoder().decode(request.getKubeconfig());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Kubeconfig is not valid base64.");
    }

    if (kubeconfigBytes.length > MAX_FILE_SIZE) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Kubeconfig file size exceeds 1 MB limit.");
    }

    com.pm4.istp.domain.AdminConfig adminConfig =
        adminConfigurationService.createConfiguration(
            kubeconfigBytes, request.getCpuLimit(), request.getMemoryLimit());

    AdminConfigResponse response =
        new AdminConfigResponse(
            true, adminConfig.getCpuLimit(), adminConfig.getMemoryLimit(), adminConfig.getUpdatedAt());

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  public ResponseEntity<AdminConfigResponse> getAdminConfig() {
    Optional<com.pm4.istp.domain.AdminConfig> config =
        adminConfigurationService.getAdminConfiguration();
    if (config.isPresent()) {
      com.pm4.istp.domain.AdminConfig adminConfig = config.get();
      AdminConfigResponse response =
          new AdminConfigResponse(
              true,
              adminConfig.getCpuLimit(),
              adminConfig.getMemoryLimit(),
              adminConfig.getUpdatedAt());
      return ResponseEntity.ok(response);
    } else {
      AdminConfigResponse response = new AdminConfigResponse(false, null, null, null);
      return ResponseEntity.ok(response);
    }
  }

  @PutMapping
  public ResponseEntity<?> updateAdminConfig(@Valid @RequestBody AdminConfigRequest request) {

    byte[] kubeconfigBytes = null;
    if (request.getKubeconfig() != null) {
      try {
        kubeconfigBytes = Base64.getDecoder().decode(request.getKubeconfig());
      } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("Kubeconfig is not valid base64.");
      }

      if (kubeconfigBytes.length > MAX_FILE_SIZE) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("Kubeconfig file size exceeds 1 MB limit.");
      }
    }

    com.pm4.istp.domain.AdminConfig adminConfig =
        adminConfigurationService.updateConfiguration(
            kubeconfigBytes, request.getCpuLimit(), request.getMemoryLimit());

    AdminConfigResponse response =
        new AdminConfigResponse(
            true, adminConfig.getCpuLimit(), adminConfig.getMemoryLimit(), adminConfig.getUpdatedAt());

    return ResponseEntity.ok(response);
  }

  @DeleteMapping
  public ResponseEntity<?> deleteAdminConfig() {
    adminConfigurationService.deleteAdminConfiguration();
    return ResponseEntity.ok(Map.of("message", "Admin configuration deleted successfully"));
  }

  @ExceptionHandler(StorageException.class)
  public ResponseEntity<String> handleStorageException(StorageException storageException) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("Storage error: " + storageException.getMessage());
  }
}
