package com.pm4.istp.challengepod.dto;

import java.time.Instant;

public record PodStatusResponse(
    PodStatusEnum status,
    String podName,
    String appUrl,
    String terminalUrl,
    String terminalPassword,
    Instant createdAt,
    Instant expiresAt) {

  public static PodStatusResponse notFound() {
    return new PodStatusResponse(PodStatusEnum.NOT_FOUND, null, null, null, null, null, null);
  }
}
