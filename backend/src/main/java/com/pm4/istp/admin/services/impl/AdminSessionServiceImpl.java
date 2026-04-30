package com.pm4.istp.admin.services.impl;

import com.pm4.istp.admin.dto.AdminActiveSessionDto;
import com.pm4.istp.admin.services.AdminSessionService;
import com.pm4.istp.shared.keycloak.KeycloakAdminClient;
import com.pm4.istp.shared.keycloak.KeycloakAppProperties;
import com.pm4.istp.shared.keycloak.KeycloakClientRepresentation;
import com.pm4.istp.shared.keycloak.KeycloakUserSessionRepresentation;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminSessionServiceImpl implements AdminSessionService {
  private final KeycloakAdminClient keycloakAdminClient;
  private final KeycloakAppProperties keycloakAppProperties;

  @Override
  public List<AdminActiveSessionDto> listActiveSessions() {
    String clientId = normalize(keycloakAppProperties.getClientId());
    if (clientId == null) {
      throw new IllegalStateException("keycloak.app.client-id is not configured");
    }

    try {
      return listActiveSessionsViaClientUserSessions(clientId);
    } catch (RuntimeException ex) {
      // Some Keycloak setups require extra permissions (e.g. view-clients) for /clients/* endpoints.
      // Fallback: iterate users and aggregate /users/{id}/sessions (requires manage-users).
      return listActiveSessionsViaUserSessions(clientId);
    }
  }

  @Override
  public void logoutSession(String sessionId) {
    String normalized = normalize(sessionId);
    if (normalized == null) {
      throw new IllegalArgumentException("sessionId is required");
    }
    keycloakAdminClient.deleteSession(normalized);
  }

  private String normalize(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    if (trimmed.isEmpty()) return null;
    return trimmed.toLowerCase(Locale.ROOT).equals("null") ? null : trimmed;
  }

  private List<AdminActiveSessionDto> listActiveSessionsViaClientUserSessions(String clientId) {
    List<KeycloakClientRepresentation> clients = keycloakAdminClient.listClients(clientId);
    KeycloakClientRepresentation client =
        clients.stream()
            .filter(c -> clientId.equals(normalize(c.getClientId())))
            .findFirst()
            .orElse(null);
    if (client == null || client.getId() == null) {
      throw new IllegalStateException("Keycloak client not found for clientId=" + clientId);
    }

    List<KeycloakUserSessionRepresentation> sessions =
        keycloakAdminClient.listClientUserSessions(client.getId());

    return sessions.stream()
        .map(
            s ->
                new AdminActiveSessionDto(
                    s.getId(),
                    s.getUserId(),
                    s.getUsername(),
                    s.getIpAddress(),
                    s.getStart(),
                    s.getLastAccess()))
        .sorted(
            Comparator.comparing(
                AdminActiveSessionDto::getLastAccess,
                Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
  }

  private List<AdminActiveSessionDto> listActiveSessionsViaUserSessions(String clientId) {
    List<com.pm4.istp.shared.keycloak.KeycloakUserRepresentation> users =
        keycloakAdminClient.listUsers(null, 0, 200);

    // NOTE: In Keycloak, UserSessionRepresentation.getClients() keys are often CLIENT UUIDs
    // (not clientIds). If we can't resolve the UUID (missing view-clients permission), we
    // return all sessions rather than incorrectly filtering to an empty set.
    return users.stream()
        .filter(u -> u != null && u.getId() != null)
        .flatMap(
            u -> {
              try {
                java.util.UUID userId = java.util.UUID.fromString(u.getId());
                List<KeycloakUserSessionRepresentation> sessions =
                    keycloakAdminClient.listUserSessions(userId);
                return sessions.stream()
                    .filter(s -> s != null)
                    .map(
                        s ->
                            new AdminActiveSessionDto(
                                s.getId(),
                                s.getUserId(),
                                s.getUsername(),
                                s.getIpAddress(),
                                s.getStart(),
                                s.getLastAccess()));
              } catch (RuntimeException ex) {
                return java.util.stream.Stream.empty();
              }
            })
        .sorted(
            Comparator.comparing(
                AdminActiveSessionDto::getLastAccess,
                Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
  }
}
