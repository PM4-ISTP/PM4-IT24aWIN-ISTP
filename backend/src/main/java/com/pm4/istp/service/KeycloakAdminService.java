package com.pm4.istp.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pm4.istp.config.KeycloakAdminProperties;
import com.pm4.istp.dto.KeycloakSessionDto;
import com.pm4.istp.dto.KeycloakUserDto;
import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Communicates with the Keycloak Admin REST API using the client-credentials flow.
 *
 * <p>An admin access token is obtained once and cached until it is about to expire (30-second
 * safety margin). All token handling is synchronized to be safe under concurrent requests.
 */
@Slf4j
@Service
public class KeycloakAdminService {

  private final KeycloakAdminProperties properties;
  private final RestClient restClient;

  private volatile String cachedToken;
  private volatile Instant tokenExpiry = Instant.EPOCH;

  public KeycloakAdminService(KeycloakAdminProperties properties) {
    this.properties = properties;
    this.restClient = RestClient.create();
  }

  // ─── Internal response shapes ──────────────────────────────────────────────

  @Data
  private static class TokenResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private int expiresIn;
  }

  @Data
  private static class ClientSessionStats {
    private String id;
    private Integer active;
    private Integer offline;
  }

  // ─── Token management ──────────────────────────────────────────────────────

  private synchronized String getAdminToken() {
    if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
      return cachedToken;
    }

    String tokenUrl =
        properties.getServerUrl()
            + "/realms/"
            + properties.getRealm()
            + "/protocol/openid-connect/token";

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("grant_type", "client_credentials");
    formData.add("client_id", properties.getClientId());
    formData.add("client_secret", properties.getClientSecret());

    TokenResponse response =
        restClient
            .post()
            .uri(tokenUrl)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(formData)
            .retrieve()
            .body(TokenResponse.class);

    if (response == null || response.getAccessToken() == null) {
      throw new IllegalStateException("Failed to obtain Keycloak admin token");
    }

    cachedToken = response.getAccessToken();
    tokenExpiry = Instant.now().plusSeconds(response.getExpiresIn() - 30L);
    log.debug("Obtained new Keycloak admin token, valid for {}s", response.getExpiresIn());

    return cachedToken;
  }

  // ─── Public API ────────────────────────────────────────────────────────────

  /**
   * Returns the total number of users registered in the realm.
   *
   * @return user count
   */
  public int getUserCount() {
    String url =
        properties.getServerUrl() + "/admin/realms/" + properties.getRealm() + "/users/count";

    Integer count =
        restClient
            .get()
            .uri(url)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminToken())
            .retrieve()
            .body(Integer.class);

    return count != null ? count : 0;
  }

  /**
   * Returns a paginated list of users in the realm.
   *
   * @param first zero-based offset
   * @param max maximum number of results
   * @return list of users
   */
  public List<KeycloakUserDto> getUsers(int first, int max) {
    String url =
        properties.getServerUrl()
            + "/admin/realms/"
            + properties.getRealm()
            + "/users?first="
            + first
            + "&max="
            + max;

    List<KeycloakUserDto> users =
        restClient
            .get()
            .uri(url)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminToken())
            .retrieve()
            .body(new ParameterizedTypeReference<List<KeycloakUserDto>>() {});

    return users != null ? users : List.of();
  }

  /**
   * Returns the total number of active client sessions across all clients in the realm.
   *
   * <p>The value is computed by summing the {@code active} field from the {@code
   * /client-session-stats} endpoint. In a typical single-client setup this equals the number of
   * currently logged-in users.
   *
   * @return active client session count
   */
  public int getActiveSessionCount() {
    String url =
        properties.getServerUrl()
            + "/admin/realms/"
            + properties.getRealm()
            + "/client-session-stats";

    List<ClientSessionStats> stats =
        restClient
            .get()
            .uri(url)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminToken())
            .retrieve()
            .body(new ParameterizedTypeReference<List<ClientSessionStats>>() {});

    if (stats == null) {
      return 0;
    }

    return stats.stream().mapToInt(s -> s.getActive() != null ? s.getActive() : 0).sum();
  }

  /**
   * Returns all active sessions for the given user.
   *
   * @param userId Keycloak user UUID
   * @return list of active sessions
   */
  public List<KeycloakSessionDto> getUserSessions(String userId) {
    String url =
        properties.getServerUrl()
            + "/admin/realms/"
            + properties.getRealm()
            + "/users/"
            + userId
            + "/sessions";

    List<KeycloakSessionDto> sessions =
        restClient
            .get()
            .uri(url)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminToken())
            .retrieve()
            .body(new ParameterizedTypeReference<List<KeycloakSessionDto>>() {});

    return sessions != null ? sessions : List.of();
  }
}
