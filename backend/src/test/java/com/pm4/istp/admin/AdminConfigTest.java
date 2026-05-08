package com.pm4.istp.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.pm4.istp.admin.db.AdminConfig;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminConfigTest {

  @Test
  void constructorsEqualsAndHashCode_useIdOnlyAndDefaultPullSecret() {
    UUID id = UUID.randomUUID();
    LocalDateTime updatedAt = LocalDateTime.now();

    AdminConfig oldConstructor = new AdminConfig(id, "500m", "256Mi", "kube", 1800, updatedAt);
    AdminConfig fullConstructor =
        new AdminConfig(id, "1", "512Mi", "pull-secret", "kube-2", 3600, updatedAt);

    assertThat(oldConstructor)
        .returns(null, AdminConfig::getImagePullSecretName)
        .returns(1800, AdminConfig::getPodTtlSeconds)
        .isEqualTo(fullConstructor)
        .hasSameHashCodeAs(fullConstructor)
        .isNotEqualTo(new AdminConfig(UUID.randomUUID(), null, null, null, 3600, updatedAt))
        .isNotEqualTo("not-config");
  }
}
