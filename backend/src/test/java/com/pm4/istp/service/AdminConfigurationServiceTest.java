package com.pm4.istp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.domain.AdminConfig;
import com.pm4.istp.exception.StorageException;
import com.pm4.istp.repositories.AdminConfigRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminConfigurationServiceTest {

    private static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private AdminConfigRepository adminConfigRepository;

    @Mock
    private KubeconfigStorageService kubeconfigStorageService;

    @InjectMocks
    private AdminConfigurationService adminConfigurationService;

    private byte[] kubeconfig;

    @BeforeEach
    void setUp() {
        kubeconfig = "kube-content".getBytes();
    }

    @Test
    void testGetAdminConfiguration_WhenPresent_ReturnsConfig() {
        AdminConfig adminConfig = new AdminConfig();
        adminConfig.setId(SINGLETON_ID);

        when(adminConfigRepository.findById(SINGLETON_ID)).thenReturn(Optional.of(adminConfig));

        Optional<AdminConfig> result = adminConfigurationService.getAdminConfiguration();

        assertTrue(result.isPresent());
        assertSame(adminConfig, result.get());
        verify(adminConfigRepository).findById(SINGLETON_ID);
    }

    @Test
    void testGetAdminConfiguration_WhenAbsent_ReturnsEmpty() {
        when(adminConfigRepository.findById(SINGLETON_ID)).thenReturn(Optional.empty());

        Optional<AdminConfig> result = adminConfigurationService.getAdminConfiguration();

        assertTrue(result.isEmpty());
        verify(adminConfigRepository).findById(SINGLETON_ID);
    }

    @Test
    void testCreateConfiguration_Success_WithAllFields() {
        when(adminConfigRepository.existsById(SINGLETON_ID)).thenReturn(false);
        when(kubeconfigStorageService.storeKubeconfig(kubeconfig)).thenReturn("/stored/config.yml");
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminConfig result = adminConfigurationService.createConfiguration(kubeconfig, "1", "1Gi");

        assertNotNull(result);
        assertEquals(SINGLETON_ID, result.getId());
        assertEquals("1", result.getCpuLimit());
        assertEquals("1Gi", result.getMemoryLimit());
        assertEquals("/stored/config.yml", result.getKubeconfig());
        assertNotNull(result.getUpdatedAt());

        verify(adminConfigRepository).existsById(SINGLETON_ID);
        verify(kubeconfigStorageService).storeKubeconfig(kubeconfig);
        verify(adminConfigRepository).save(any(AdminConfig.class));
    }

    @Test
    void testCreateConfiguration_ThrowsWhenAlreadyExists() {
        when(adminConfigRepository.existsById(SINGLETON_ID)).thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> adminConfigurationService.createConfiguration(kubeconfig, "1", "1Gi"));

        assertEquals("Admin configuration already exists", exception.getMessage());
        verify(adminConfigRepository).existsById(SINGLETON_ID);
        verify(adminConfigRepository, never()).save(any());
        verify(kubeconfigStorageService, never()).storeKubeconfig(any());
    }

    @Test
    void testCreateConfiguration_WithoutKubeconfig_DoesNotStoreFile() {
        when(adminConfigRepository.existsById(SINGLETON_ID)).thenReturn(false);
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminConfig result = adminConfigurationService.createConfiguration(null, "2", "2Gi");

        assertEquals(SINGLETON_ID, result.getId());
        assertEquals("2", result.getCpuLimit());
        assertEquals("2Gi", result.getMemoryLimit());
        assertNull(result.getKubeconfig());
        assertNotNull(result.getUpdatedAt());

        verify(kubeconfigStorageService, never()).storeKubeconfig(any());
        verify(adminConfigRepository).save(any(AdminConfig.class));
    }

    @Test
    void testCreateConfiguration_WithEmptyKubeconfig_DoesNotStoreFile() {
        byte[] emptyFile = new byte[0];

        when(adminConfigRepository.existsById(SINGLETON_ID)).thenReturn(false);
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminConfig result = adminConfigurationService.createConfiguration(emptyFile, "2", "2Gi");

        assertEquals("2", result.getCpuLimit());
        assertEquals("2Gi", result.getMemoryLimit());
        assertNull(result.getKubeconfig());
        assertNotNull(result.getUpdatedAt());

        verify(kubeconfigStorageService, never()).storeKubeconfig(any());
        verify(adminConfigRepository).save(any(AdminConfig.class));
    }

    @Test
    void testCreateConfiguration_BlankCpuAndMemory_DoNotOverwrite() {
        when(adminConfigRepository.existsById(SINGLETON_ID)).thenReturn(false);
        when(kubeconfigStorageService.storeKubeconfig(kubeconfig)).thenReturn("/stored/config.yml");
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminConfig result = adminConfigurationService.createConfiguration(kubeconfig, " ", "");

        assertEquals(SINGLETON_ID, result.getId());
        assertNull(result.getCpuLimit());
        assertNull(result.getMemoryLimit());
        assertEquals("/stored/config.yml", result.getKubeconfig());
        assertNotNull(result.getUpdatedAt());

        verify(kubeconfigStorageService).storeKubeconfig(kubeconfig);
        verify(adminConfigRepository).save(any(AdminConfig.class));
    }

    @Test
    void testUpdateConfiguration_Success_WithAllFields() {
        AdminConfig existing = new AdminConfig();
        existing.setId(SINGLETON_ID);
        existing.setCpuLimit("1");
        existing.setMemoryLimit("1Gi");
        existing.setKubeconfig("/old/config.yml");
        existing.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));

        when(adminConfigRepository.findById(SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(kubeconfigStorageService.storeKubeconfig(kubeconfig)).thenReturn("/new/config.yml");
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminConfig result = adminConfigurationService.updateConfiguration(kubeconfig, "3", "3Gi");

        assertEquals(SINGLETON_ID, result.getId());
        assertEquals("3", result.getCpuLimit());
        assertEquals("3Gi", result.getMemoryLimit());
        assertEquals("/new/config.yml", result.getKubeconfig());
        assertNotNull(result.getUpdatedAt());

        verify(adminConfigRepository).findById(SINGLETON_ID);
        verify(kubeconfigStorageService).storeKubeconfig(kubeconfig);
        verify(adminConfigRepository).save(existing);
    }

    @Test
    void testUpdateConfiguration_ThrowsWhenMissing() {
        when(adminConfigRepository.findById(SINGLETON_ID)).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> adminConfigurationService.updateConfiguration(kubeconfig, "3", "3Gi"));

        assertEquals("Admin configuration does not exist. Create it first.", exception.getMessage());
        verify(adminConfigRepository).findById(SINGLETON_ID);
        verify(adminConfigRepository, never()).save(any());
        verify(kubeconfigStorageService, never()).storeKubeconfig(any());
    }

    @Test
    void testUpdateConfiguration_WithNullFields_LeavesExistingValuesUnchanged() {
        AdminConfig existing = new AdminConfig();
        existing.setId(SINGLETON_ID);
        existing.setCpuLimit("2");
        existing.setMemoryLimit("2Gi");
        existing.setKubeconfig("/existing/config.yml");
        existing.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));

        when(adminConfigRepository.findById(SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminConfig result = adminConfigurationService.updateConfiguration(null, null, null);

        assertEquals("2", result.getCpuLimit());
        assertEquals("2Gi", result.getMemoryLimit());
        assertEquals("/existing/config.yml", result.getKubeconfig());
        assertNotNull(result.getUpdatedAt());

        verify(kubeconfigStorageService, never()).storeKubeconfig(any());
        verify(adminConfigRepository).save(existing);
    }

    @Test
    void testUpdateConfiguration_WithBlankFields_LeavesExistingValuesUnchanged() {
        AdminConfig existing = new AdminConfig();
        existing.setId(SINGLETON_ID);
        existing.setCpuLimit("2");
        existing.setMemoryLimit("2Gi");
        existing.setKubeconfig("/existing/config.yml");
        existing.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));

        when(adminConfigRepository.findById(SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminConfig result = adminConfigurationService.updateConfiguration(null, "   ", "");

        assertEquals("2", result.getCpuLimit());
        assertEquals("2Gi", result.getMemoryLimit());
        assertEquals("/existing/config.yml", result.getKubeconfig());
        assertNotNull(result.getUpdatedAt());

        verify(kubeconfigStorageService, never()).storeKubeconfig(any());
        verify(adminConfigRepository).save(existing);
    }

    @Test
    void testUpdateConfiguration_OnlyCpuProvided_UpdatesOnlyCpu() {
        AdminConfig existing = new AdminConfig();
        existing.setId(SINGLETON_ID);
        existing.setCpuLimit("1");
        existing.setMemoryLimit("1Gi");
        existing.setKubeconfig("/existing/config.yml");

        when(adminConfigRepository.findById(SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminConfig result = adminConfigurationService.updateConfiguration(null, "4", null);

        assertEquals("4", result.getCpuLimit());
        assertEquals("1Gi", result.getMemoryLimit());
        assertEquals("/existing/config.yml", result.getKubeconfig());

        verify(kubeconfigStorageService, never()).storeKubeconfig(any());
        verify(adminConfigRepository).save(existing);
    }

    @Test
    void testUpdateConfiguration_OnlyMemoryProvided_UpdatesOnlyMemory() {
        AdminConfig existing = new AdminConfig();
        existing.setId(SINGLETON_ID);
        existing.setCpuLimit("1");
        existing.setMemoryLimit("1Gi");
        existing.setKubeconfig("/existing/config.yml");

        when(adminConfigRepository.findById(SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminConfig result = adminConfigurationService.updateConfiguration(null, null, "4Gi");

        assertEquals("1", result.getCpuLimit());
        assertEquals("4Gi", result.getMemoryLimit());
        assertEquals("/existing/config.yml", result.getKubeconfig());

        verify(kubeconfigStorageService, never()).storeKubeconfig(any());
        verify(adminConfigRepository).save(existing);
    }

    @Test
    void testUpdateConfiguration_OnlyKubeconfigProvided_UpdatesOnlyKubeconfig() {
        AdminConfig existing = new AdminConfig();
        existing.setId(SINGLETON_ID);
        existing.setCpuLimit("1");
        existing.setMemoryLimit("1Gi");
        existing.setKubeconfig("/old/config.yml");

        when(adminConfigRepository.findById(SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(kubeconfigStorageService.storeKubeconfig(kubeconfig)).thenReturn("/new/config.yml");
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminConfig result = adminConfigurationService.updateConfiguration(kubeconfig, null, null);

        assertEquals("1", result.getCpuLimit());
        assertEquals("1Gi", result.getMemoryLimit());
        assertEquals("/new/config.yml", result.getKubeconfig());

        verify(kubeconfigStorageService).storeKubeconfig(kubeconfig);
        verify(adminConfigRepository).save(existing);
    }

    @Test
    void testDeleteAdminConfiguration_WhenExists_Deletes() {
        when(adminConfigRepository.existsById(SINGLETON_ID)).thenReturn(true);

        adminConfigurationService.deleteAdminConfiguration();

        verify(adminConfigRepository).existsById(SINGLETON_ID);
        verify(adminConfigRepository).deleteById(SINGLETON_ID);
    }

    @Test
    void testDeleteAdminConfiguration_WhenMissing_DoesNothing() {
        when(adminConfigRepository.existsById(SINGLETON_ID)).thenReturn(false);

        adminConfigurationService.deleteAdminConfiguration();

        verify(adminConfigRepository).existsById(SINGLETON_ID);
        verify(adminConfigRepository, never()).deleteById(any());
    }

    @Test
    void testCreateConfiguration_WhenStorageThrowsStorageException_RethrowsIt() {
        when(adminConfigRepository.existsById(SINGLETON_ID)).thenReturn(false);
        when(kubeconfigStorageService.storeKubeconfig(kubeconfig))
                .thenThrow(new StorageException("Failed to store", new RuntimeException()));

        StorageException exception = assertThrows(
                StorageException.class,
                () -> adminConfigurationService.createConfiguration(kubeconfig, "1", "1Gi"));

        assertEquals("Failed to store", exception.getMessage());
        verify(adminConfigRepository, never()).save(any());
    }

    @Test
    void testCreateConfiguration_WhenUnexpectedExceptionOccurs_WrapsInStorageException() {
        when(adminConfigRepository.existsById(SINGLETON_ID)).thenReturn(false);
        when(kubeconfigStorageService.storeKubeconfig(kubeconfig))
                .thenThrow(new RuntimeException("boom"));

        StorageException exception = assertThrows(
                StorageException.class,
                () -> adminConfigurationService.createConfiguration(kubeconfig, "1", "1Gi"));

        assertTrue(exception.getMessage().contains("Failed to save admin configuration: boom"));
        verify(adminConfigRepository, never()).save(any());
    }

    @Test
    void testUpdateConfiguration_WhenStorageThrowsStorageException_RethrowsIt() {
        AdminConfig existing = new AdminConfig();
        existing.setId(SINGLETON_ID);

        when(adminConfigRepository.findById(SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(kubeconfigStorageService.storeKubeconfig(kubeconfig))
                .thenThrow(new StorageException("Failed to update", new RuntimeException()));

        StorageException exception = assertThrows(
                StorageException.class,
                () -> adminConfigurationService.updateConfiguration(kubeconfig, "2", "2Gi"));

        assertEquals("Failed to update", exception.getMessage());
        verify(adminConfigRepository, never()).save(any());
    }

    @Test
    void testUpdateConfiguration_WhenUnexpectedExceptionOccurs_WrapsInStorageException() {
        AdminConfig existing = new AdminConfig();
        existing.setId(SINGLETON_ID);

        when(adminConfigRepository.findById(SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(kubeconfigStorageService.storeKubeconfig(kubeconfig))
                .thenThrow(new RuntimeException("boom"));

        StorageException exception = assertThrows(
                StorageException.class,
                () -> adminConfigurationService.updateConfiguration(kubeconfig, "2", "2Gi"));

        assertTrue(exception.getMessage().contains("Failed to save admin configuration: boom"));
        verify(adminConfigRepository, never()).save(any());
    }

    @Test
    void testCreateConfiguration_SetsUpdatedAtToNowish() {
        when(adminConfigRepository.existsById(SINGLETON_ID)).thenReturn(false);
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        AdminConfig result = adminConfigurationService.createConfiguration(null, "1", "1Gi");
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(result.getUpdatedAt());
        assertTrue(!result.getUpdatedAt().isBefore(before));
        assertTrue(!result.getUpdatedAt().isAfter(after));
    }

    @Test
    void testUpdateConfiguration_SetsUpdatedAtToNowish() {
        AdminConfig existing = new AdminConfig();
        existing.setId(SINGLETON_ID);
        existing.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 0, 0));

        when(adminConfigRepository.findById(SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        AdminConfig result = adminConfigurationService.updateConfiguration(null, "5", "5Gi");
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(result.getUpdatedAt());
        assertTrue(!result.getUpdatedAt().isBefore(before));
        assertTrue(!result.getUpdatedAt().isAfter(after));
    }

    @Test
    void testCreateConfiguration_SavesExpectedEntity() {
        when(adminConfigRepository.existsById(SINGLETON_ID)).thenReturn(false);
        when(kubeconfigStorageService.storeKubeconfig(kubeconfig)).thenReturn("/stored/config.yml");
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        adminConfigurationService.createConfiguration(kubeconfig, "6", "6Gi");

        ArgumentCaptor<AdminConfig> captor = ArgumentCaptor.forClass(AdminConfig.class);
        verify(adminConfigRepository).save(captor.capture());

        AdminConfig saved = captor.getValue();
        assertEquals(SINGLETON_ID, saved.getId());
        assertEquals("6", saved.getCpuLimit());
        assertEquals("6Gi", saved.getMemoryLimit());
        assertEquals("/stored/config.yml", saved.getKubeconfig());
        assertNotNull(saved.getUpdatedAt());
    }
}
