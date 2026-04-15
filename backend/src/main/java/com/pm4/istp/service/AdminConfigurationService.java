package com.pm4.istp.service;

import com.pm4.istp.domain.AdminConfig;
import com.pm4.istp.repositories.AdminConfigRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class AdminConfigurationService {

  private static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  private final AdminConfigRepository adminConfigRepository;

  public AdminConfigurationService(@NonNull AdminConfigRepository adminConfigRepository) {
    this.adminConfigRepository = adminConfigRepository;
  }

  @Transactional(readOnly = true)
  public Optional<AdminConfig> getAdminConfiguration() {
    return adminConfigRepository.findById(SINGLETON_ID);
  }

  public AdminConfig createConfiguration(byte[] kubeconfig, String cpuLimit, String memoryLimit) {

    if (adminConfigRepository.existsById(SINGLETON_ID)) {
      throw new IllegalStateException("Admin configuration already exists");
    }

    AdminConfig adminConfig = new AdminConfig();
    adminConfig.setId(SINGLETON_ID);

    applyUpdates(adminConfig, kubeconfig, cpuLimit, memoryLimit);

    AdminConfig savedConfig = adminConfigRepository.save(adminConfig);
    log.info("Created admin configuration with ID {}", savedConfig.getId());
    return savedConfig;
  }

  public AdminConfig updateConfiguration(byte[] kubeconfig, String cpuLimit, String memoryLimit) {

    AdminConfig adminConfig =
        adminConfigRepository
            .findById(SINGLETON_ID)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Admin configuration does not exist. Create it first."));

    applyUpdates(adminConfig, kubeconfig, cpuLimit, memoryLimit);

    AdminConfig savedConfig = adminConfigRepository.save(adminConfig);
    log.info("Updated admin configuration with ID {}", savedConfig.getId());
    return savedConfig;
  }

  public void deleteAdminConfiguration() {
    if (adminConfigRepository.existsById(SINGLETON_ID)) {
      adminConfigRepository.deleteById(SINGLETON_ID);
      log.info("Deleted admin configuration");
    } else {
      log.warn("No admin configuration found to delete");
    }
  }

  private void applyUpdates(
      AdminConfig adminConfig, byte[] kubeconfig, String cpuLimit, String memoryLimit) {
    if (kubeconfig != null && kubeconfig.length > 0) {
      adminConfig.setKubeconfig(new String(kubeconfig, StandardCharsets.UTF_8));
    }

    if (cpuLimit != null && !cpuLimit.isBlank()) {
      adminConfig.setCpuLimit(cpuLimit);
    }

    if (memoryLimit != null && !memoryLimit.isBlank()) {
      adminConfig.setMemoryLimit(memoryLimit);
    }

    adminConfig.setUpdatedAt(LocalDateTime.now());
  }
}
