package com.pm4.istp.dto;

import lombok.Data;

/** Basic user information returned by the Keycloak Admin REST API. */
@Data
public class KeycloakUserDto {

  private String id;
  private String username;
  private String email;
  private String firstName;
  private String lastName;
  private boolean enabled;
  private Long createdTimestamp;
}
