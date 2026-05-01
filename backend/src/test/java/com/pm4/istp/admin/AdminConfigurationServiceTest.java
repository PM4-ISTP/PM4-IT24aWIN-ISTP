package com.pm4.istp.admin;

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

import com.pm4.istp.admin.db.AdminConfig;
import com.pm4.istp.admin.repositories.AdminConfigRepository;
import com.pm4.istp.admin.services.AdminConfigurationService;
import org.springframework.context.ApplicationEventPublisher;

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
    private ApplicationEventPublisher eventPublisher;

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
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminConfig result = adminConfigurationService.createConfiguration(kubeconfig, "1", "1Gi", null);

        assertNotNull(result);
        assertEquals(SINGLETON_ID, result.getId());
        assertEquals("1", result.getCpuLimit());
        assertEquals("1Gi", result.getMemoryLimit());
        assertEquals("kube-content", result.getKubeconfig());
        assertNotNull(result.getUpdatedAt());

        verify(adminConfigRepository).existsById(SINGLETON_ID);
        verify(adminConfigRepository).save(any(AdminConfig.class));
    }

    @Test
    void testCreateConfiguration_ThrowsWhenAlreadyExists() {
        when(adminConfigRepository.existsById(SINGLETON_ID)).thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> adminConfigurationService.createConfiguration(kubeconfig, "1", "1Gi", null));

        assertEquals("Admin configuration already exists", exception.getMessage());
        verify(adminConfigRepository).existsById(SINGLETON_ID);
        verify(adminConfigRepository, never()).save(any());
    }

    @Test
    void testCreateConfiguration_WithoutKubeconfig_DoesNotStoreKubeconfig() {
        when(adminConfigRepository.existsById(SINGLETON_ID)).thenReturn(false);
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminConfig result = adminConfigurationService.createConfiguration(null, "2", "2Gi", null);

        assertEquals(SINGLETON_ID, result.getId());
        assertEquals("2", result.getCpuLimit());
        assertEquals("2Gi", result.getMemoryLimit());
        assertNull(result.getKubeconfig());
        assertNotNull(result.getUpdatedAt());

        verify(adminConfigRepository).save(any(AdminConfig.class));
    }

    @Test
    void testCreateConfiguration_WithEmptyKubeconfig_DoesNotStoreKubeconfig() {
        byte[] emptyFile = new byte[0];

        when(adminConfigRepository.existsById(SINGLETON_ID)).thenReturn(false);
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminConfig result = adminConfigurationService.createConfiguration(emptyFile, "2", "2Gi", null);

        assertEquals("2", result.getCpuLimit());
        assertEquals("2Gi", result.getMemoryLimit());
        assertNull(result.getKubeconfig());
        assertNotNull(result.getUpdatedAt());

        verify(adminConfigRepository).save(any(AdminConfig.class));
    }

    @Test
    void testCreateConfiguration_BlankCpuAndMemory_DoNotOverwrite() {
        when(adminConfigRepository.existsById(SINGLETON_ID)).thenReturn(false);
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminConfig result = adminConfigurationService.createConfiguration(kubeconfig, " ", "", null);

        assertEquals(SINGLETON_ID, result.getId());
        assertNull(result.getCpuLimit());
        assertNull(result.getMemoryLimit());
        assertEquals("kube-content", result.getKubeconfig());
        assertNotNull(result.getUpdatedAt());

        verify(adminConfigRepository).save(any(AdminConfig.class));
    }

    @Test
    void testUpdateConfiguration_Success_WithAllFields() {
        AdminConfig existing = new AdminConfig();
        existing.setId(SINGLETON_ID);
        existing.setCpuLimit("1");
        existing.setMemoryLimit("1Gi");
        existing.setKubeconfig("old-kube-content");
        existing.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));

        when(adminConfigRepository.findById(SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminConfig result = adminConfigurationService.updateConfiguration(kubeconfig, "3", "3Gi", null);

        assertEquals(SINGLETON_ID, result.getId());
        assertEquals("3", result.getCpuLimit());
        assertEquals("3Gi", result.getMemoryLimit());
        assertEquals("kube-content", result.getKubeconfig());
        assertNotNull(result.getUpdatedAt());

        verify(adminConfigRepository).findById(SINGLETON_ID);
        verify(adminConfigRepository).save(existing);
    }

    @Test
    void testUpdateConfiguration_ThrowsWhenMissing() {
        when(adminConfigRepository.findById(SINGLETON_ID)).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> adminConfigurationService.updateConfiguration(kubeconfig, "3", "3Gi", null));

        assertEquals("Admin configuration does not exist. Create it first.", exception.getMessage());
        verify(adminConfigRepository).findById(SINGLETON_ID);
        verify(adminConfigRepository, never()).save(any());
    }

    @Test
    void testUpdateConfiguration_WithNullFields_LeavesExistingValuesUnchanged() {
        AdminConfig existing = new AdminConfig();
        existing.setId(SINGLETON_ID);
        existing.setCpuLimit("2");
        existing.setMemoryLimit("2Gi");
        existing.setKubeconfig("existing-kube-content");
        existing.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));

        when(adminConfigRepository.findById(SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminConfig result = adminConfigurationService.updateConfiguration(null, null, null, null);

        assertEquals("2", result.getCpuLimit());
        assertEquals("2Gi", result.getMemoryLimit());
        assertEquals("existing-kube-content", result.getKubeconfig());
        assertNotNull(result.getUpdatedAt());

        verify(adminConfigRepository).save(existing);
    }

    @Test
    void testUpdateConfiguration_WithBlankFields_LeavesExistingValuesUnchanged() {
        AdminConfig existing = new AdminConfig();
        existing.setId(SINGLETON_ID);
        existing.setCpuLimit("2");
        existing.setMemoryLimit("2Gi");
        existing.setKubeconfig("existing-kube-content");
        existing.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 10, 0));

        when(adminConfigRepository.findById(SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminConfig result = adminConfigurationService.updateConfiguration(null, "   ", "", null);

        assertEquals("2", result.getCpuLimit());
        assertEquals("2Gi", result.getMemoryLimit());
        assertEquals("existing-kube-content", result.getKubeconfig());
        assertNotNull(result.getUpdatedAt());

        verify(adminConfigRepository).save(existing);
    }

    @Test
    void testUpdateConfiguration_OnlyCpuProvided_UpdatesOnlyCpu() {
        AdminConfig existing = new AdminConfig();
        existing.setId(SINGLETON_ID);
        existing.setCpuLimit("1");
        existing.setMemoryLimit("1Gi");
        existing.setKubeconfig("existing-kube-content");

        when(adminConfigRepository.findById(SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminConfig result = adminConfigurationService.updateConfiguration(null, "4", null, null);

        assertEquals("4", result.getCpuLimit());
        assertEquals("1Gi", result.getMemoryLimit());
        assertEquals("existing-kube-content", result.getKubeconfig());

        verify(adminConfigRepository).save(existing);
    }

    @Test
    void testUpdateConfiguration_OnlyMemoryProvided_UpdatesOnlyMemory() {
        AdminConfig existing = new AdminConfig();
        existing.setId(SINGLETON_ID);
        existing.setCpuLimit("1");
        existing.setMemoryLimit("1Gi");
        existing.setKubeconfig("existing-kube-content");

        when(adminConfigRepository.findById(SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminConfig result = adminConfigurationService.updateConfiguration(null, null, "4Gi", null);

        assertEquals("1", result.getCpuLimit());
        assertEquals("4Gi", result.getMemoryLimit());
        assertEquals("existing-kube-content", result.getKubeconfig());

        verify(adminConfigRepository).save(existing);
    }

    @Test
    void testUpdateConfiguration_OnlyKubeconfigProvided_UpdatesOnlyKubeconfig() {
        AdminConfig existing = new AdminConfig();
        existing.setId(SINGLETON_ID);
        existing.setCpuLimit("1");
        existing.setMemoryLimit("1Gi");
        existing.setKubeconfig("old-kube-content");

        when(adminConfigRepository.findById(SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminConfig result = adminConfigurationService.updateConfiguration(kubeconfig, null, null, null);

        assertEquals("1", result.getCpuLimit());
        assertEquals("1Gi", result.getMemoryLimit());
        assertEquals("kube-content", result.getKubeconfig());

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
    void testCreateConfiguration_SetsUpdatedAtToNowish() {
        when(adminConfigRepository.existsById(SINGLETON_ID)).thenReturn(false);
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        AdminConfig result = adminConfigurationService.createConfiguration(null, "1", "1Gi", null);
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
        AdminConfig result = adminConfigurationService.updateConfiguration(null, "5", "5Gi", null);
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(result.getUpdatedAt());
        assertTrue(!result.getUpdatedAt().isBefore(before));
        assertTrue(!result.getUpdatedAt().isAfter(after));
    }

    @Test
    void testCreateConfiguration_SavesExpectedEntity() {
        when(adminConfigRepository.existsById(SINGLETON_ID)).thenReturn(false);
        when(adminConfigRepository.save(any(AdminConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        adminConfigurationService.createConfiguration(kubeconfig, "6", "6Gi", null);

        ArgumentCaptor<AdminConfig> captor = ArgumentCaptor.forClass(AdminConfig.class);
        verify(adminConfigRepository).save(captor.capture());

        AdminConfig saved = captor.getValue();
        assertEquals(SINGLETON_ID, saved.getId());
        assertEquals("6", saved.getCpuLimit());
        assertEquals("6Gi", saved.getMemoryLimit());
        assertEquals("kube-content", saved.getKubeconfig());
        assertNotNull(saved.getUpdatedAt());
    }
}
