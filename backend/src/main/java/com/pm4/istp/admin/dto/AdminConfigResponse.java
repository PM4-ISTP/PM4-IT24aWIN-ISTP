package com.pm4.istp.admin.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminConfigResponse {
  private boolean kubeconfigUploaded;
  private String cpuLimit;
  private String memoryLimit;
  private Integer podTtlSeconds;
  private LocalDateTime updatedAt;
}
