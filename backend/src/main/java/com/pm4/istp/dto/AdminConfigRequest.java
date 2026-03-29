package com.pm4.istp.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminConfigRequest {
  @Pattern(regexp = "^[0-9]+$", message = "CPU limit must be a positive integer")
  private String cpuLimit;

  @Pattern(regexp = "^[0-9]+(Mi|Gi|Ti)?$", message = "Memory limit must be a number followed by optional unit (Mi, Gi, or Ti)")
  private String memoryLimit;

  private String kubeconfig;
}
