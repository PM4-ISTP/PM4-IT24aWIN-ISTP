package com.pm4.istp.controller;

import com.pm4.istp.domain.AdminConfig;
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
  public ResponseEntity<AdminConfig> uploadAndStoreAdminConfig(
      @RequestParam("kubeconfig") MultipartFile kubeconfig,
      @RequestParam(value = "cpuLimit", required = false) String cpuLimit,
      @RequestParam(value = "memoryLimit", required = false) String memoryLimit) {
    AdminConfig adminConfig =
        adminConfigurationService.saveConfiguration(kubeconfig, cpuLimit, memoryLimit);
    return ResponseEntity.status(HttpStatus.CREATED).body(adminConfig);
  }

  @GetMapping
  public ResponseEntity<?> getAdminConfig() {
    Optional<AdminConfig> config = adminConfigurationService.getAdminConfiguration();
    if (config.isPresent()) {
      return ResponseEntity.ok(config.get());
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No admin configuration found");
    }
  }

  @PutMapping
  public ResponseEntity<AdminConfig> updateAdminConfig(
      @RequestParam(value = "kubeconfig", required = false) MultipartFile kubeconfig,
      @RequestParam(value = "cpuLimit", required = false) String cpuLimit,
      @RequestParam(value = "memoryLimit", required = false) String memoryLimit) {
    AdminConfig adminConfig =
        adminConfigurationService.saveConfiguration(kubeconfig, cpuLimit, memoryLimit);
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
