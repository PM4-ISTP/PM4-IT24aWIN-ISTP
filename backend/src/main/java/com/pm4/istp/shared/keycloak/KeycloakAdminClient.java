package com.pm4.istp.shared.keycloak;

import java.util.UUID;

public interface KeycloakAdminClient {
  KeycloakUserRepresentation getUser(UUID userId);

  void updateUser(UUID userId, KeycloakUserRepresentation updatedUser);

  UUID createUser(KeycloakUserRepresentation newUser);

  void deleteUser(UUID userId);

  void resetPassword(UUID userId, String password, boolean temporary);

  KeycloakRoleRepresentation getRealmRoleByName(String roleName);

  void addRealmRoles(UUID userId, java.util.List<KeycloakRoleRepresentation> roles);

  java.util.List<KeycloakUserRepresentation> listUsers(String search, Integer first, Integer max);

  java.util.List<KeycloakRoleRepresentation> listUserRealmRoles(UUID userId);

  void removeRealmRoles(UUID userId, java.util.List<KeycloakRoleRepresentation> roles);

  java.util.List<KeycloakUserSessionRepresentation> listUserSessions(UUID userId);

  void logoutUser(UUID userId);

  void executeActionsEmail(UUID userId, java.util.List<String> actions);

  java.util.List<KeycloakClientRepresentation> listClients(String clientId);

  java.util.List<KeycloakUserSessionRepresentation> listClientUserSessions(String clientUuid);

  void deleteSession(String sessionId);
}
