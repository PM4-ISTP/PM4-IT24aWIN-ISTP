package com.pm4.istp.shared.keycloak;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "keycloak.admin")
public class KeycloakAdminProperties {
  private String baseUrl;
  private String realm;
  private String clientId;
  private String clientSecret;
}

