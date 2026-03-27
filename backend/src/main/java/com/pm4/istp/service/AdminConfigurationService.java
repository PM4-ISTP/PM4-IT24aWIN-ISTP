package com.pm4.istp.service;

import com.pm4.istp.domain.AdminConfig;
import com.pm4.istp.exception.StorageException;
import com.pm4.istp.repositories.AdminConfigRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class AdminConfigurationService {

  private static final Logger LOG = LoggerFactory.getLogger(AdminConfigurationService.class);

  private static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  private final AdminConfigRepository adminConfigRepository;
  private final KubeconfigStorageService kubeconfigStorageService;

  public AdminConfigurationService(
      @NonNull AdminConfigRepository adminConfigRepository,
      @NonNull KubeconfigStorageService kubeconfigStorageService) {
    this.adminConfigRepository = adminConfigRepository;
    this.kubeconfigStorageService = kubeconfigStorageService;
  }

  @Transactional(readOnly = true)
  public Optional<AdminConfig> getAdminConfiguration() {
    return adminConfigRepository.findById(SINGLETON_ID);
  }

  public AdminConfig saveConfiguration(
      MultipartFile kubeconfig, String cpuLimit, String memoryLimit) {
    try {
      AdminConfig adminConfig = getOrCreateAdminConfig();

      if (kubeconfig != null && !kubeconfig.isEmpty()) {
        String storedPath = kubeconfigStorageService.storeKubeconfig(kubeconfig);
        adminConfig.setKubeconfig(storedPath);
        LOG.info("Kubeconfig stored at {}", storedPath);
      }

      if (cpuLimit != null && !cpuLimit.isBlank()) {
        adminConfig.setCpuLimit(cpuLimit);
      }

      if (memoryLimit != null && !memoryLimit.isBlank()) {
        adminConfig.setMemoryLimit(memoryLimit);
      }

      adminConfig.setUpdatedAt(LocalDateTime.now());

      AdminConfig savedConfig = adminConfigRepository.save(adminConfig);
      LOG.info("Saved admin configuration with ID {}", savedConfig.getId());

      return savedConfig;
    } catch (StorageException e) {
      LOG.error("Failed to save admin configuration: {}", e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      LOG.error("Unexpected error during admin configuration save: {}", e.getMessage(), e);
      throw new StorageException("Failed to save admin configuration: " + e.getMessage(), e);
    }
  }

  public void deleteAdminConfiguration() {
    if (adminConfigRepository.existsById(SINGLETON_ID)) {
      adminConfigRepository.deleteById(SINGLETON_ID);
      LOG.info("Deleted admin configuration");
    } else {
      LOG.warn("No admin configuration found to delete");
    }
  }

  private AdminConfig getOrCreateAdminConfig() {
    return adminConfigRepository.findById(SINGLETON_ID).orElseGet(this::createNewAdminConfig);
  }

  private AdminConfig createNewAdminConfig() {
    AdminConfig adminConfig = new AdminConfig();
    adminConfig.setId(SINGLETON_ID);
    adminConfig.setUpdatedAt(LocalDateTime.now());
    return adminConfig;
  }
}
