package com.pm4.istp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PodCreationResponse {
  private String status;
  private String podName; // Or rather deploymentId
  private String namespace;
  private String message;

  private String appUrl;
  private String terminalUrl;
  private String terminalPassword;
}
