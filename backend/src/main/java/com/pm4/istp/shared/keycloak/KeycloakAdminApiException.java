package com.pm4.istp.shared.keycloak;

public class KeycloakAdminApiException extends RuntimeException {
  public KeycloakAdminApiException(String message) {
    super(message);
  }

  public KeycloakAdminApiException(String message, Throwable cause) {
    super(message, cause);
  }
}

