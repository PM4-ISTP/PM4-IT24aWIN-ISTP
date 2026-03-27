package com.pm4.istp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code keycloak.admin.*} properties for the Keycloak Admin REST API service account. */
@Getter
@Setter
@ConfigurationProperties(prefix = "keycloak.admin")
public class KeycloakAdminProperties {

  /** Base URL of the Keycloak server (e.g. {@code http://localhost:9090}). */
  private String serverUrl;

  /** Realm name that the backend connects to. */
  private String realm;

  /** Client ID of the service account used for the Admin REST API. */
  private String clientId;

  /** Client secret of the service account. */
  private String clientSecret;
}
