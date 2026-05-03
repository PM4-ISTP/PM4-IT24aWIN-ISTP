package com.pm4.istp.admin.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "admin_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminConfig {

  @Id
  @Column(name = "id", updatable = false, nullable = false, unique = true)
  private UUID id;

  @Column(name = "cpu_limit")
  private String cpuLimit;

  @Column(name = "memory_limit")
  private String memoryLimit;

  @Column(name = "image_pull_secret_name")
  private String imagePullSecretName;

  @Lob
  @Column(name = "kubeconfig", columnDefinition = "TEXT", nullable = false)
  private String kubeconfig;

  @Column(
      name = "pod_ttl_seconds",
      nullable = false,
      columnDefinition = "INT NOT NULL DEFAULT 3600")
  private int podTtlSeconds = 3600;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public AdminConfig(
      UUID id,
      String cpuLimit,
      String memoryLimit,
      String kubeconfig,
      int podTtlSeconds,
      LocalDateTime updatedAt) {
    this(id, cpuLimit, memoryLimit, null, kubeconfig, podTtlSeconds, updatedAt);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AdminConfig other)) {
      return false;
    }
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
