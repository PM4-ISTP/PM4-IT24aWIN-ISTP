package com.pm4.istp.shared.keycloak;

import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakServiceAccountTokenProvider {
  private static final long REFRESH_SAFETY_SECONDS = 10;

  private final KeycloakAdminProperties properties;
  private final ObjectMapper objectMapper;
  private final Clock clock = Clock.systemUTC();

  private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

  public String getAccessToken() {
    CachedToken current = cachedToken.get();
    Instant now = Instant.now(clock);

    if (current != null && current.expiresAt().isAfter(now.plusSeconds(REFRESH_SAFETY_SECONDS))) {
      return current.accessToken();
    }

    synchronized (this) {
      current = cachedToken.get();
      now = Instant.now(clock);
      if (current != null && current.expiresAt().isAfter(now.plusSeconds(REFRESH_SAFETY_SECONDS))) {
        return current.accessToken();
      }

      CachedToken refreshed = fetchToken(now);
      cachedToken.set(refreshed);
      return refreshed.accessToken();
    }
  }

  private CachedToken fetchToken(Instant now) {
    String tokenUrl =
        normalizeBaseUrl(properties.getBaseUrl())
            + "/realms/"
            + properties.getRealm()
            + "/protocol/openid-connect/token";

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "client_credentials");
    form.add("client_id", properties.getClientId());
    form.add("client_secret", properties.getClientSecret());

    try {
      TokenResponse response =
          RestClient.create()
              .post()
              .uri(tokenUrl)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
              .body(form)
              .retrieve()
              .body(TokenResponse.class);

      if (response == null || response.accessToken() == null || response.expiresIn() == null) {
        throw new KeycloakAdminApiException("Keycloak token endpoint returned an empty response");
      }

      logServiceAccountTokenClaims(response.accessToken());
      Instant expiresAt = now.plusSeconds(Math.max(0, response.expiresIn()));
      return new CachedToken(response.accessToken(), expiresAt);
    } catch (RestClientException ex) {
      throw new KeycloakAdminApiException("Failed to fetch Keycloak service account token", ex);
    }
  }

  private void logServiceAccountTokenClaims(String accessToken) {
    if (!log.isDebugEnabled()) {
      return;
    }
    try {
      String[] parts = accessToken == null ? new String[0] : accessToken.split("\\.");
      if (parts.length < 2) {
        return;
      }
      byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
      Map<String, Object> claims = objectMapper.readValue(decoded, new TypeReference<>() {});
      Object azp = claims.get("azp");
      Object realmAccess = claims.get("realm_access");
      Object resourceAccess = claims.get("resource_access");
      log.debug(
          "Keycloak service account token acquired (azp={}, realm_access={}, resource_access={})",
          azp,
          realmAccess,
          resourceAccess);
    } catch (Exception ex) {
      log.debug("Failed to decode Keycloak service account token claims", ex);
    }
  }

  private String normalizeBaseUrl(String baseUrl) {
    if (baseUrl == null) {
      return "";
    }
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  private record TokenResponse(
      @com.fasterxml.jackson.annotation.JsonProperty("access_token") String accessToken,
      @com.fasterxml.jackson.annotation.JsonProperty("expires_in") Long expiresIn) {}

  private record CachedToken(String accessToken, Instant expiresAt) {}
}
