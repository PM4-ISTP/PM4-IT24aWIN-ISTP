package com.pm4.istp.shared.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakUserSessionRepresentation {
  private String id;
  private String ipAddress;
  private Long start;
  private Long lastAccess;
  private Boolean rememberMe;
  private String username;
  private String userId;
  private Map<String, String> clients;
}

