package com.pm4.istp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminConfigRequest {
  private String cpuLimit;
  private String memoryLimit;
  private String kubeconfig;
}
