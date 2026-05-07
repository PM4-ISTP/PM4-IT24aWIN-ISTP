package com.pm4.istp.challengepod;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.admin.db.AdminConfig;
import com.pm4.istp.admin.services.AdminConfigurationService;
import com.pm4.istp.challengepod.services.LabPodScheduler;
import com.pm4.istp.challengepod.services.LabPodService;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LabPodSchedulerTest {

    @Mock
    private AdminConfigurationService adminConfigurationService;

    @Mock
    private LabPodService labPodService;

    @InjectMocks
    private LabPodScheduler scheduler;

    private AdminConfig configWith(String kubeconfig, int ttl) {
        AdminConfig cfg = new AdminConfig();
        cfg.setId(UUID.randomUUID());
        cfg.setKubeconfig(kubeconfig);
        cfg.setPodTtlSeconds(ttl);
        return cfg;
    }

    @Test
    void reap_skipsWhenNoAdminConfig() {
        when(adminConfigurationService.getAdminConfiguration()).thenReturn(Optional.empty());

        scheduler.reap();

        verify(labPodService, never()).reapExpiredPods(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void reap_skipsWhenKubeconfigIsNull() {
        when(adminConfigurationService.getAdminConfiguration())
                .thenReturn(Optional.of(configWith(null, 3600)));

        scheduler.reap();

        verify(labPodService, never()).reapExpiredPods(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void reap_skipsWhenKubeconfigIsBlank() {
        when(adminConfigurationService.getAdminConfiguration())
                .thenReturn(Optional.of(configWith("   ", 3600)));

        scheduler.reap();

        verify(labPodService, never()).reapExpiredPods(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void reap_callsReapWithTtlFromAdminConfig() {
        when(adminConfigurationService.getAdminConfiguration())
                .thenReturn(Optional.of(configWith("kubeconfig-content", 1800)));

        scheduler.reap();

        verify(labPodService).reapExpiredPods(1800);
    }

    @Test
    void reap_doesNotThrowWhenServiceThrows() {
        when(adminConfigurationService.getAdminConfiguration())
                .thenReturn(Optional.of(configWith("kubeconfig-content", 3600)));
        org.mockito.Mockito.doThrow(new RuntimeException("k8s unavailable"))
                .when(labPodService)
                .reapExpiredPods(3600);

        assertThatCode(() -> scheduler.reap()).doesNotThrowAnyException();
    }
}
