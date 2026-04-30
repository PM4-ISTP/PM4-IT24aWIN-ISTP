package com.pm4.istp.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminActiveSessionDto {
  private String sessionId;
  private String userId;
  private String username;
  private String ipAddress;
  private Long start;
  private Long lastAccess;
}
