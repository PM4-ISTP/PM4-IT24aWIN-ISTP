package com.pm4.istp.shared.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class KeycloakAdminRestClientTest {

  private HttpServer server;
  private KeycloakAdminRestClient client;
  private final List<String> requests = new ArrayList<>();

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    TestKeycloakHandler handler = new TestKeycloakHandler(requests);
    server.createContext("/", handler::handle);
    server.start();

    KeycloakAdminProperties properties = new KeycloakAdminProperties();
    properties.setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/");
    properties.setRealm("istp");
    properties.setUserByIdPath("/users/{id}");
    properties.setUserRealmRoleMappingsPath("/users/{id}/role-mappings/realm");

    KeycloakServiceAccountTokenProvider tokenProvider =
        Mockito.mock(KeycloakServiceAccountTokenProvider.class);
    when(tokenProvider.getAccessToken()).thenReturn("admin-token");
    client = new KeycloakAdminRestClient(properties, tokenProvider);
  }

  @AfterEach
  void tearDown() {
    server.stop(0);
  }

  @Test
  void userAndRoleWriteOperations_callExpectedEndpoints() {
    UUID userId = UUID.randomUUID();
    KeycloakUserRepresentation user = client.getUser(userId);
    user.setEnabled(false);
    client.updateUser(userId, user);
    UUID createdId = client.createUser(new KeycloakUserRepresentation());
    client.deleteUser(userId);
    client.resetPassword(userId, "secret", true);
    KeycloakRoleRepresentation role = client.getRealmRoleByName("ROLE_STUDENT");
    client.addRealmRoles(userId, List.of(role));
    client.removeRealmRoles(userId, List.of(role));

    assertThat(user.getId()).isEqualTo(userId.toString());
    assertThat(createdId).isEqualTo(TestKeycloakHandler.CREATED_ID);
    assertThat(role.getName()).isEqualTo("ROLE_STUDENT");
    assertThat(requests)
        .contains(
            "GET /admin/realms/istp/users/" + userId,
            "PUT /admin/realms/istp/users/" + userId,
            "POST /admin/realms/istp/users",
            "DELETE /admin/realms/istp/users/" + userId,
            "PUT /admin/realms/istp/users/" + userId + "/reset-password",
            "GET /admin/realms/istp/roles/ROLE_STUDENT",
            "POST /admin/realms/istp/users/" + userId + "/role-mappings/realm",
            "DELETE /admin/realms/istp/users/" + userId + "/role-mappings/realm");
  }

  @Test
  void listAndSessionOperations_returnArraysAndNormalizeQueryParameters() {
    UUID userId = UUID.randomUUID();

    assertThat(client.listUsers(" alice ", 1, 5)).hasSize(1);
    assertThat(client.listUserRealmRoles(userId)).extracting(KeycloakRoleRepresentation::getName).contains("ROLE_STUDENT");
    assertThat(client.listUserSessions(userId)).extracting(KeycloakUserSessionRepresentation::getId).contains("session-1");
    client.logoutUser(userId);
    client.executeActionsEmail(userId, null);
    assertThat(client.listClients(" istp-web ")).extracting(KeycloakClientRepresentation::getClientId).contains("istp-web");
    assertThat(client.listClientUserSessions("client-uuid")).hasSize(1);
    client.deleteSession("session-1");

    assertThat(requests)
        .contains(
            "GET /admin/realms/istp/users?search=alice&first=1&max=5",
            "GET /admin/realms/istp/users/" + userId + "/role-mappings/realm",
            "GET /admin/realms/istp/users/" + userId + "/sessions",
            "POST /admin/realms/istp/users/" + userId + "/logout",
            "PUT /admin/realms/istp/users/" + userId + "/execute-actions-email",
            "GET /admin/realms/istp/clients?clientId=istp-web",
            "GET /admin/realms/istp/clients/client-uuid/user-sessions",
            "DELETE /admin/realms/istp/sessions/session-1");
  }

  @Test
  void notFoundLookupsReturnNullAndOtherErrorsWrap() {
    assertThat(client.getUser(TestKeycloakHandler.NOT_FOUND_ID)).isNull();
    assertThat(client.getRealmRoleByName("missing")).isNull();

    assertThatThrownBy(() -> client.getUser(TestKeycloakHandler.ERROR_ID))
        .isInstanceOf(KeycloakAdminApiException.class)
        .hasMessageContaining("Failed to read user");
    assertThatThrownBy(() -> client.deleteSession("error"))
        .isInstanceOf(KeycloakAdminApiException.class)
        .hasMessageContaining("Failed to delete Keycloak session");
  }

  @Test
  void createUserWithoutValidLocation_throws() {
    assertThatThrownBy(() -> client.createUser(userWithUsername("invalid-location")))
        .isInstanceOf(KeycloakAdminApiException.class)
        .hasMessageContaining("Location header");
  }

  private static KeycloakUserRepresentation userWithUsername(String username) {
    KeycloakUserRepresentation user = new KeycloakUserRepresentation();
    user.setUsername(username);
    return user;
  }

  private static class TestKeycloakHandler {
    static final UUID CREATED_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final UUID NOT_FOUND_ID = UUID.fromString("40440440-4040-4040-4040-404404404040");
    static final UUID ERROR_ID = UUID.fromString("50050050-5000-5000-5000-500500500500");

    private final List<String> requests;

    TestKeycloakHandler(List<String> requests) {
      this.requests = requests;
    }

    void handle(HttpExchange exchange) throws IOException {
      String path = exchange.getRequestURI().getPath();
      String query = exchange.getRequestURI().getRawQuery();
      String methodAndPath = exchange.getRequestMethod() + " " + path + (query == null ? "" : "?" + query);
      requests.add(methodAndPath);

      if (!"Bearer admin-token".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
        write(exchange, 401, "{}");
        return;
      }
      if (path.contains(ERROR_ID.toString()) || path.endsWith("/sessions/error")) {
        write(exchange, 500, "{}");
        return;
      }
      if (path.contains(NOT_FOUND_ID.toString()) || path.endsWith("/roles/missing")) {
        write(exchange, 404, "{}");
        return;
      }
      if ("POST".equals(exchange.getRequestMethod()) && path.endsWith("/users")) {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        exchange
            .getResponseHeaders()
            .add(
                "Location",
                body.contains("invalid-location")
                    ? "/admin/realms/istp/users/not-a-uuid"
                    : "/admin/realms/istp/users/" + CREATED_ID);
        write(exchange, 201, "");
        return;
      }
      if ("GET".equals(exchange.getRequestMethod()) && path.matches(".*/users/[0-9a-f-]+$")) {
        String id = path.substring(path.lastIndexOf('/') + 1);
        write(exchange, 200, "{\"id\":\"" + id + "\",\"username\":\"alice\",\"enabled\":true}");
        return;
      }
      if (path.endsWith("/roles/ROLE_STUDENT")) {
        write(exchange, 200, "{\"id\":\"role-id\",\"name\":\"ROLE_STUDENT\"}");
        return;
      }
      if (path.endsWith("/role-mappings/realm") && "GET".equals(exchange.getRequestMethod())) {
        write(exchange, 200, "[{\"id\":\"role-id\",\"name\":\"ROLE_STUDENT\"}]");
        return;
      }
      if (path.endsWith("/users") && "GET".equals(exchange.getRequestMethod())) {
        write(exchange, 200, "[{\"id\":\"33333333-3333-3333-3333-333333333333\",\"username\":\"alice\"}]");
        return;
      }
      if (path.endsWith("/sessions") && "GET".equals(exchange.getRequestMethod())) {
        write(exchange, 200, "[{\"id\":\"session-1\",\"username\":\"alice\"}]");
        return;
      }
      if (path.endsWith("/clients") && "GET".equals(exchange.getRequestMethod())) {
        write(exchange, 200, "[{\"id\":\"client-uuid\",\"clientId\":\"istp-web\"}]");
        return;
      }
      if (path.endsWith("/clients/client-uuid/user-sessions")) {
        write(exchange, 200, "[{\"id\":\"session-1\",\"username\":\"alice\"}]");
        return;
      }
      write(exchange, 204, "");
    }

    private static void write(HttpExchange exchange, int status, String body) throws IOException {
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(status, bytes.length);
      exchange.getResponseBody().write(bytes);
      exchange.close();
    }
  }
}
