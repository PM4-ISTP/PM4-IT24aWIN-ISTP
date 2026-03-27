package com.pm4.istp.dto;

import java.util.Map;
import lombok.Data;

/** An active user session returned by the Keycloak Admin REST API. */
@Data
public class KeycloakSessionDto {

  private String id;
  private String userId;
  private String username;
  private String ipAddress;
  private Long start;
  private Long lastAccess;

  /** Map of client-id → client-name for all clients active in this session. */
  private Map<String, String> clients;
}
