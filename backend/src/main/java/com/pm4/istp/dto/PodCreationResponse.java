package com.pm4.istp.dto;

import lombok.Data;

@Data
public class PodCreationResponse {
  private String status;
  private String podName;
  private String namespace;
  private String message;

  public PodCreationResponse(String status, String podName, String namespace, String message) {
    this.status = status;
    this.podName = podName;
    this.namespace = namespace;
    this.message = message;
  }
}
