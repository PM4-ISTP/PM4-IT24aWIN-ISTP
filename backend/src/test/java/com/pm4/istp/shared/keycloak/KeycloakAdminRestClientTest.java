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
import tools.jackson.databind.ObjectMapper;

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
    client = new KeycloakAdminRestClient(properties, tokenProvider, new ObjectMapper());
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
    assertThat(user.getAttributes())
        .containsEntry("cibaBackchannelTokenDeliveryMode", List.of("poll"));
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
  void writeAndSessionOperations_wrapServerErrors() {
    UUID userId = TestKeycloakHandler.ERROR_ID;
    KeycloakRoleRepresentation role = new KeycloakRoleRepresentation();
    role.setName("ROLE_STUDENT");

    assertThatThrownBy(() -> client.updateUser(userId, new KeycloakUserRepresentation()))
        .isInstanceOf(KeycloakAdminApiException.class)
        .hasMessageContaining("Failed to update user");
    assertThatThrownBy(() -> client.createUser(userWithUsername("server-error")))
        .isInstanceOf(KeycloakAdminApiException.class)
        .hasMessageContaining("Failed to create user");
    assertThatThrownBy(() -> client.deleteUser(userId))
        .isInstanceOf(KeycloakAdminApiException.class)
        .hasMessageContaining("Failed to delete user");
    assertThatThrownBy(() -> client.resetPassword(userId, "secret", false))
        .isInstanceOf(KeycloakAdminApiException.class)
        .hasMessageContaining("Failed to reset");
    assertThatThrownBy(() -> client.getRealmRoleByName("error"))
        .isInstanceOf(KeycloakAdminApiException.class)
        .hasMessageContaining("Failed to read Keycloak realm role");
    assertThatThrownBy(() -> client.addRealmRoles(userId, List.of(role)))
        .isInstanceOf(KeycloakAdminApiException.class)
        .hasMessageContaining("Failed to assign");
    assertThatThrownBy(() -> client.listUserRealmRoles(userId))
        .isInstanceOf(KeycloakAdminApiException.class)
        .hasMessageContaining("Failed to list Keycloak user realm roles");
    assertThatThrownBy(() -> client.removeRealmRoles(userId, List.of(role)))
        .isInstanceOf(KeycloakAdminApiException.class)
        .hasMessageContaining("Failed to remove");
    assertThatThrownBy(() -> client.listUserSessions(userId))
        .isInstanceOf(KeycloakAdminApiException.class)
        .hasMessageContaining("Failed to list Keycloak user sessions");
    assertThatThrownBy(() -> client.logoutUser(userId))
        .isInstanceOf(KeycloakAdminApiException.class)
        .hasMessageContaining("Failed to logout");
    assertThatThrownBy(() -> client.executeActionsEmail(userId, List.of("UPDATE_PASSWORD")))
        .isInstanceOf(KeycloakAdminApiException.class)
        .hasMessageContaining("Failed to execute");
    assertThatThrownBy(() -> client.listClients("server-error"))
        .isInstanceOf(KeycloakAdminApiException.class)
        .hasMessageContaining("Failed to list Keycloak clients");
    assertThatThrownBy(() -> client.listClientUserSessions("error"))
        .isInstanceOf(KeycloakAdminApiException.class)
        .hasMessageContaining("Failed to list Keycloak client user sessions");
  }

  @Test
  void listOperations_returnEmptyListsWhenKeycloakReturnsNoBody() {
    UUID userId = TestKeycloakHandler.EMPTY_BODY_ID;

    assertThat(client.listUsers("empty-body", null, null)).isEmpty();
    assertThat(client.listUserRealmRoles(userId)).isEmpty();
    assertThat(client.listUserSessions(userId)).isEmpty();
    assertThat(client.listClients("empty-body")).isEmpty();
    assertThat(client.listClientUserSessions("empty-body")).isEmpty();
  }

  @Test
  void createUserWithoutValidLocation_throws() {
    KeycloakUserRepresentation user = userWithUsername("invalid-location");

    assertThatThrownBy(() -> client.createUser(user))
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
    static final UUID EMPTY_BODY_ID = UUID.fromString("20420420-2040-2040-2040-204204204020");

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
      if (path.endsWith("/roles/error")
          || path.endsWith("/clients/error/user-sessions")
          || "server-error".equals(queryValue(exchange, "search"))
          || "server-error".equals(queryValue(exchange, "clientId"))) {
        write(exchange, 500, "{}");
        return;
      }
      if (path.contains(EMPTY_BODY_ID.toString())
          || "empty-body".equals(queryValue(exchange, "search"))
          || "empty-body".equals(queryValue(exchange, "clientId"))
          || path.endsWith("/clients/empty-body/user-sessions")) {
        write(exchange, 204, "");
        return;
      }
      if (path.contains(NOT_FOUND_ID.toString()) || path.endsWith("/roles/missing")) {
        write(exchange, 404, "{}");
        return;
      }
      if ("POST".equals(exchange.getRequestMethod()) && path.endsWith("/users")) {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (body.contains("server-error")) {
          write(exchange, 500, "{}");
          return;
        }
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
        write(
            exchange,
            200,
            "{\"id\":\""
                + id
                + "\",\"username\":\"alice\",\"enabled\":true,"
                + "\"attributes\":{\"cibaBackchannelTokenDeliveryMode\":\"poll\"}}");
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

    private static String queryValue(HttpExchange exchange, String name) {
      String query = exchange.getRequestURI().getRawQuery();
      if (query == null) {
        return null;
      }
      for (String parameter : query.split("&")) {
        String[] parts = parameter.split("=", 2);
        if (parts.length == 2 && name.equals(parts[0])) {
          return parts[1];
        }
      }
      return null;
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
