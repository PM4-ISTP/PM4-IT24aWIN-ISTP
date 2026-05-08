package com.pm4.istp.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminConfigRequest {
  @Pattern(regexp = "^\\d+$", message = "CPU limit must be a positive integer")
  private String cpuLimit;

  @Pattern(
      regexp = "^\\d+(Mi|Gi|Ti)?$",
      message = "Memory limit must be a number followed by optional unit (Mi, Gi, or Ti)")
  private String memoryLimit;

  @Pattern(
      regexp = "^$|[a-z0-9]([-a-z0-9]*[a-z0-9])?$",
      message =
          "Image pull secret name must be a valid Kubernetes resource name "
              + "(lowercase letters, numbers, and '-')")
  private String imagePullSecretName;

  private String kubeconfig;

  @Min(value = 60, message = "Pod TTL must be at least 60 seconds")
  @Max(value = 86400, message = "Pod TTL must not exceed 86400 seconds (24h)")
  private Integer podTtlSeconds;

  public AdminConfigRequest(
      String cpuLimit, String memoryLimit, String kubeconfig, Integer podTtlSeconds) {
    this(cpuLimit, memoryLimit, null, kubeconfig, podTtlSeconds);
  }
}
