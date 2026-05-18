package com.pm4.istp.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.admin.services.AdminSessionService;
import com.pm4.istp.shared.keycloak.KeycloakAdminClient;
import com.pm4.istp.shared.keycloak.KeycloakAppProperties;
import com.pm4.istp.shared.keycloak.KeycloakClientRepresentation;
import com.pm4.istp.shared.keycloak.KeycloakUserRepresentation;
import com.pm4.istp.shared.keycloak.KeycloakUserSessionRepresentation;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminSessionServiceImplTest {

  @Mock private KeycloakAdminClient keycloakAdminClient;

  private KeycloakAppProperties keycloakAppProperties;
  private AdminSessionService service;

  @BeforeEach
  void setUp() {
    keycloakAppProperties = new KeycloakAppProperties();
    service = new AdminSessionService(keycloakAdminClient, keycloakAppProperties);
  }

  @Test
  void listActiveSessions_clientSessions_returnsSortedDtos() {
    keycloakAppProperties.setClientId(" istp-web ");

    KeycloakClientRepresentation matchingClient = new KeycloakClientRepresentation();
    matchingClient.setId("client-uuid");
    matchingClient.setClientId("istp-web");
    KeycloakClientRepresentation otherClient = new KeycloakClientRepresentation();
    otherClient.setId("other");
    otherClient.setClientId("other-client");

    KeycloakUserSessionRepresentation older = session("s1", "u1", "alice", "10.0.0.1", 10L);
    KeycloakUserSessionRepresentation newer = session("s2", "u2", "bob", "10.0.0.2", 20L);
    KeycloakUserSessionRepresentation noLastAccess =
        session("s3", "u3", "charlie", "10.0.0.3", null);

    when(keycloakAdminClient.listClients("istp-web")).thenReturn(List.of(otherClient, matchingClient));
    when(keycloakAdminClient.listClientUserSessions("client-uuid"))
        .thenReturn(List.of(older, noLastAccess, newer));

    var result = service.listActiveSessions();

    assertThat(result).extracting("sessionId").containsExactly("s2", "s1", "s3");
    assertThat(result.get(0).getUsername()).isEqualTo("bob");
    assertThat(result.get(0).getIpAddress()).isEqualTo("10.0.0.2");
  }

  @Test
  void listActiveSessions_clientEndpointFails_fallsBackToUserSessions() {
    keycloakAppProperties.setClientId("istp-web");
    UUID userId = UUID.randomUUID();

    KeycloakUserRepresentation user = new KeycloakUserRepresentation();
    user.setId(userId.toString());
    KeycloakUserRepresentation invalidUser = new KeycloakUserRepresentation();
    invalidUser.setId("not-a-uuid");
    KeycloakUserRepresentation userWithoutId = new KeycloakUserRepresentation();

    KeycloakUserSessionRepresentation session =
        session("s1", userId.toString(), "alice", "127.0.0.1", 42L);

    when(keycloakAdminClient.listClients("istp-web")).thenThrow(new RuntimeException("forbidden"));
    when(keycloakAdminClient.listUsers(null, 0, 200))
        .thenReturn(List.of(userWithoutId, invalidUser, user));
    when(keycloakAdminClient.listUserSessions(userId)).thenReturn(Arrays.asList(null, session));

    var result = service.listActiveSessions();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getSessionId()).isEqualTo("s1");
    verify(keycloakAdminClient).listUserSessions(userId);
  }

  @Test
  void listActiveSessions_missingClientId_throws() {
    keycloakAppProperties.setClientId(" null ");

    assertThatThrownBy(() -> service.listActiveSessions())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("keycloak.app.client-id");
  }

  @Test
  void listActiveSessions_matchingClientMissingId_fallsBackToUserSessions() {
    keycloakAppProperties.setClientId("istp-web");
    KeycloakClientRepresentation clientWithoutId = new KeycloakClientRepresentation();
    clientWithoutId.setClientId("istp-web");

    when(keycloakAdminClient.listClients("istp-web")).thenReturn(List.of(clientWithoutId));
    when(keycloakAdminClient.listUsers(null, 0, 200)).thenReturn(List.of());

    assertThat(service.listActiveSessions()).isEmpty();
  }

  @Test
  void logoutSession_validSession_deletesTrimmedValue() {
    service.logoutSession(" session-1 ");

    verify(keycloakAdminClient).deleteSession("session-1");
  }

  @Test
  void logoutSession_blankOrNullLiteral_throws() {
    assertThatThrownBy(() -> service.logoutSession(" null "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sessionId");
  }

  @Test
  void listActiveSessions_userSessionFailure_skipsUser() {
    keycloakAppProperties.setClientId("istp-web");
    UUID userId = UUID.randomUUID();
    KeycloakUserRepresentation user = new KeycloakUserRepresentation();
    user.setId(userId.toString());

    when(keycloakAdminClient.listClients("istp-web")).thenThrow(new RuntimeException("forbidden"));
    when(keycloakAdminClient.listUsers(null, 0, 200)).thenReturn(List.of(user));
    doThrow(new RuntimeException("user session denied")).when(keycloakAdminClient).listUserSessions(userId);

    assertThat(service.listActiveSessions()).isEmpty();
  }

  private static KeycloakUserSessionRepresentation session(
      String id, String userId, String username, String ipAddress, Long lastAccess) {
    KeycloakUserSessionRepresentation session = new KeycloakUserSessionRepresentation();
    session.setId(id);
    session.setUserId(userId);
    session.setUsername(username);
    session.setIpAddress(ipAddress);
    session.setStart(1L);
    session.setLastAccess(lastAccess);
    return session;
  }
}
