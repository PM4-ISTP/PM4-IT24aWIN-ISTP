package com.pm4.istp.dto;

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

  private String kubeconfig;
}
