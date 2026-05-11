package com.pm4.istp.admin.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminConfigEntityTest {

  @Test
  void compatibilityConstructorKeepsImagePullSecretUnset() {
    UUID id = UUID.randomUUID();
    LocalDateTime updatedAt = LocalDateTime.now();

    AdminConfig config =
        new AdminConfig(id, "500m", "512Mi", "kubeconfig", 1800, updatedAt);

    assertThat(config.getId()).isEqualTo(id);
    assertThat(config.getCpuLimit()).isEqualTo("500m");
    assertThat(config.getMemoryLimit()).isEqualTo("512Mi");
    assertThat(config.getImagePullSecretName()).isNull();
    assertThat(config.getKubeconfig()).isEqualTo("kubeconfig");
    assertThat(config.getPodTtlSeconds()).isEqualTo(1800);
    assertThat(config.getUpdatedAt()).isEqualTo(updatedAt);
  }

  @Test
  void equalsAndHashCodeUseIdOnly() {
    UUID id = UUID.randomUUID();
    AdminConfig first = new AdminConfig();
    first.setId(id);
    first.setCpuLimit("500m");

    AdminConfig second = new AdminConfig();
    second.setId(id);
    second.setCpuLimit("1000m");

    AdminConfig different = new AdminConfig();
    different.setId(UUID.randomUUID());

    assertThat(first)
        .isEqualTo(first)
        .isEqualTo(second)
        .hasSameHashCodeAs(second)
        .isNotEqualTo(different)
        .isNotEqualTo("not config");
  }
}
