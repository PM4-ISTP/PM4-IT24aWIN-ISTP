package com.pm4.istp.challengepod.services;

import com.pm4.istp.admin.db.AdminConfig;
import com.pm4.istp.admin.services.AdminConfigurationService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LabPodScheduler {

  private final AdminConfigurationService adminConfigurationService;
  private final LabPodService labPodService;

  public LabPodScheduler(
      @NonNull AdminConfigurationService adminConfigurationService,
      @NonNull LabPodService labPodService) {
    this.adminConfigurationService = adminConfigurationService;
    this.labPodService = labPodService;
  }

  @Scheduled(
      fixedDelayString = "${istp.pod-reaper.interval-ms:60000}",
      initialDelayString = "${istp.pod-reaper.initial-delay-ms:30000}")
  public void reap() {
    try {
      AdminConfig cfg = adminConfigurationService.getAdminConfiguration().orElse(null);
      if (cfg == null || cfg.getKubeconfig() == null || cfg.getKubeconfig().isBlank()) {
        log.debug("Pod reaper skipped: no admin config / kubeconfig");
        return;
      }
      labPodService.reapExpiredPods(cfg.getPodTtlSeconds());
    } catch (Exception e) {
      log.error("Pod reaper failed", e);
    }
  }
}
