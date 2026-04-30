package com.pm4.istp.shared.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakRoleRepresentation {
  private String id;
  private String name;
}
