package com.pm4.istp.admin.dto;

import com.pm4.istp.shared.keycloak.KeycloakUserRepresentation;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminUserDetailDto {
  private UUID id;
  private String name;
  private String email;
  private String username;
  private String firstName;
  private String lastName;
  private String title;
  private String picture;
  private Set<String> roles;
  private LocalDateTime deletedAt;
  private LocalDateTime anonymizedAt;
  private boolean provisioned;

  // Keycloak snapshot (source of truth for enabled + base profile)
  private KeycloakUserRepresentation keycloak;
}
