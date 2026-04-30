package com.pm4.istp.shared.keycloak;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "keycloak.app")
public class KeycloakAppProperties {
  private String clientId;
}

