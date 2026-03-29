package com.pm4.istp.controller;

import com.pm4.istp.domain.AdminConfig;
import com.pm4.istp.dto.AdminConfigResponse;
import com.pm4.istp.exception.StorageException;
import com.pm4.istp.service.AdminConfigurationService;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = "/api/admin/config")
public class AdminConfigurationController {

  private final AdminConfigurationService adminConfigurationService;

  private final long MAX_FILE_SIZE = 1_048_576; // 1 MB

  public AdminConfigurationController(
      @NonNull AdminConfigurationService adminConfigurationService) {
    this.adminConfigurationService = adminConfigurationService;
  }

  /**
   * Uploads a kubeconfig file and stores admin configuration in the database.
   *
   * @param kubeconfig the kubeconfig
   * @param cpuLimit the CPU limit for each pod
   * @param memoryLimit the memory limit for each pod
   * @return ResponseEntity containing the stored AdminConfig
   */
  @PostMapping
  public ResponseEntity<?> uploadAndStoreAdminConfig(
      @RequestParam("kubeconfig") MultipartFile kubeconfig,
      @RequestParam(value = "cpuLimit", required = false) String cpuLimit,
      @RequestParam(value = "memoryLimit", required = false) String memoryLimit) {

    if (kubeconfig.getSize() > MAX_FILE_SIZE) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Kubeconfig file size exceeds 1 MB limit.");
    }

    AdminConfig adminConfig =
        adminConfigurationService.createConfiguration(kubeconfig, cpuLimit, memoryLimit);

    return ResponseEntity.status(HttpStatus.CREATED).body(adminConfig);
  }

  @GetMapping
  public ResponseEntity<?> getAdminConfig() {
    Optional<AdminConfig> config = adminConfigurationService.getAdminConfiguration();
    if (config.isPresent()) {
      AdminConfig adminConfig = config.get();
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
  public ResponseEntity<AdminConfig> updateAdminConfig(
      @RequestParam(value = "kubeconfig", required = false) MultipartFile kubeconfig,
      @RequestParam(value = "cpuLimit", required = false) String cpuLimit,
      @RequestParam(value = "memoryLimit", required = false) String memoryLimit) {

    AdminConfig adminConfig =
        adminConfigurationService.updateConfiguration(kubeconfig, cpuLimit, memoryLimit);

    return ResponseEntity.ok(adminConfig);
  }

  @DeleteMapping
  public ResponseEntity<String> deleteAdminConfig() {
    adminConfigurationService.deleteAdminConfiguration();
    return ResponseEntity.ok("Admin configuration deleted successfully");
  }

  @ExceptionHandler(StorageException.class)
  public ResponseEntity<String> handleStorageException(StorageException storageException) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("Storage error: " + storageException.getMessage());
  }
}
