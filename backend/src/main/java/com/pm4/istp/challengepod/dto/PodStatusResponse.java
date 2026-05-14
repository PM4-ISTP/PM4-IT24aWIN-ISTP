package com.pm4.istp.challengepod.dto;

import java.time.Instant;

public record PodStatusResponse(
    PodStatusEnum status,
    String podName,
    String appUrl,
    String terminalUrl,
    String terminalPassword,
    Instant createdAt,
    Instant expiresAt,
    Instant lastActivityAt,
    Integer ttlSeconds,
    Integer extensionCount,
    Integer maxExtensionCount,
    Boolean canExtend) {

  public static PodStatusResponse notFound() {
    return new PodStatusResponse(
        PodStatusEnum.NOT_FOUND, null, null, null, null, null, null, null, null, null, null, null);
  }
}
