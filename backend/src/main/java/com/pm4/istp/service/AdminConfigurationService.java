package com.pm4.istp.service;

import com.pm4.istp.domain.AdminConfig;
import com.pm4.istp.exception.StorageException;
import com.pm4.istp.repositories.AdminConfigRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Transactional
public class AdminConfigurationService {

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

    public AdminConfig createConfiguration(
            MultipartFile kubeconfig, String cpuLimit, String memoryLimit) {

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

    public AdminConfig updateConfiguration(
            MultipartFile kubeconfig, String cpuLimit, String memoryLimit) {

        AdminConfig adminConfig = adminConfigRepository
                .findById(SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException(
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
            AdminConfig adminConfig,
            MultipartFile kubeconfig,
            String cpuLimit,
            String memoryLimit) {
        try {
            if (kubeconfig != null && !kubeconfig.isEmpty()) {
                String storedPath = kubeconfigStorageService.storeKubeconfig(kubeconfig);
                adminConfig.setKubeconfig(storedPath);
                log.info("Kubeconfig stored at {}", storedPath);
            }

            if (cpuLimit != null && !cpuLimit.isBlank()) {
                adminConfig.setCpuLimit(cpuLimit);
            }

            if (memoryLimit != null && !memoryLimit.isBlank()) {
                adminConfig.setMemoryLimit(memoryLimit);
            }

            adminConfig.setUpdatedAt(LocalDateTime.now());

        } catch (StorageException e) {
            log.error("Failed to apply admin configuration updates: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during admin configuration update: {}", e.getMessage(), e);
            throw new StorageException("Failed to save admin configuration: " + e.getMessage(), e);
        }
    }
}