package com.pm4.istp.challengepod.services;

import com.pm4.istp.admin.db.AdminConfig;
import com.pm4.istp.admin.services.AdminConfigurationService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChallengePodScheduler {

  private final AdminConfigurationService adminConfigurationService;
  private final ChallengePodService challengePodService;

  public ChallengePodScheduler(
      @NonNull AdminConfigurationService adminConfigurationService,
      @NonNull ChallengePodService challengePodService) {
    this.adminConfigurationService = adminConfigurationService;
    this.challengePodService = challengePodService;
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
      challengePodService.reapExpiredPods(cfg.getPodTtlSeconds());
    } catch (Exception e) {
      log.error("Pod reaper failed", e);
    }
  }
}
