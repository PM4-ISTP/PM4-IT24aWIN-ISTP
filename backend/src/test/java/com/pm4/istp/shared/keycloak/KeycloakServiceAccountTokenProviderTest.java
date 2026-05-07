package com.pm4.istp.shared.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class KeycloakServiceAccountTokenProviderTest {

  private HttpServer server;

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void getAccessToken_fetchesTokenAndCachesIt() throws Exception {
    AtomicInteger calls = new AtomicInteger();
    startServer(
        exchange -> {
          calls.incrementAndGet();
          assertThat(exchange.getRequestMethod()).isEqualTo("POST");
          String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          assertThat(body).contains("grant_type=client_credentials");
          write(exchange, 200, "{\"access_token\":\"token-1\",\"expires_in\":120}");
        });

    KeycloakServiceAccountTokenProvider provider = providerForServer();

    assertThat(provider.getAccessToken()).isEqualTo("token-1");
    assertThat(provider.getAccessToken()).isEqualTo("token-1");
    assertThat(calls).hasValue(1);
  }

  @Test
  void getAccessToken_emptyResponseAndHttpError_throwKeycloakException() throws Exception {
    startServer(exchange -> write(exchange, 200, "{}"));
    assertThatThrownBy(() -> providerForServer().getAccessToken())
        .isInstanceOf(KeycloakAdminApiException.class)
        .hasMessageContaining("empty response");
    server.stop(0);

    startServer(exchange -> write(exchange, 500, "{}"));
    assertThatThrownBy(() -> providerForServer().getAccessToken())
        .isInstanceOf(KeycloakAdminApiException.class)
        .hasMessageContaining("Failed to fetch");
  }

  @Test
  void getAccessToken_refreshesWhenCachedTokenExpiresWithinSafetyWindow() throws Exception {
    AtomicInteger calls = new AtomicInteger();
    startServer(
        exchange -> {
          int call = calls.incrementAndGet();
          write(exchange, 200, "{\"access_token\":\"token-" + call + "\",\"expires_in\":5}");
        });

    KeycloakServiceAccountTokenProvider provider = providerForServer();

    assertThat(provider.getAccessToken()).isEqualTo("token-1");
    assertThat(provider.getAccessToken()).isEqualTo("token-2");
    assertThat(calls).hasValue(2);
  }

  private KeycloakServiceAccountTokenProvider providerForServer() {
    KeycloakAdminProperties properties = new KeycloakAdminProperties();
    properties.setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/");
    properties.setRealm("istp");
    properties.setClientId("svc");
    properties.setClientSecret("secret");
    return new KeycloakServiceAccountTokenProvider(properties, new ObjectMapper());
  }

  private void startServer(ThrowingHandler handler) throws IOException {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext("/realms/istp/protocol/openid-connect/token", exchange -> handler.handle(exchange));
    server.start();
  }

  private static void write(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }

  private interface ThrowingHandler {
    void handle(HttpExchange exchange) throws IOException;
  }
}
