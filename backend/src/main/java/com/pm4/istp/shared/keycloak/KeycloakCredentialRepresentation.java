package com.pm4.istp.shared.keycloak;

import lombok.Data;

@Data
public class KeycloakCredentialRepresentation {
  private String type;
  private String value;
  private Boolean temporary;
}
