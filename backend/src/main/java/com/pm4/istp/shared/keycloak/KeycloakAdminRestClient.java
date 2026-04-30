package com.pm4.istp.shared.keycloak;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class KeycloakAdminRestClient implements KeycloakAdminClient {
  private final KeycloakAdminProperties properties;
  private final KeycloakServiceAccountTokenProvider tokenProvider;

  @Override
  public KeycloakUserRepresentation getUser(UUID userId) {
    try {
      return restClient()
          .get()
          .uri("/users/{id}", userId)
          .retrieve()
          .body(KeycloakUserRepresentation.class);
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() == 404) {
        return null;
      }
      throw new KeycloakAdminApiException("Failed to read user from Keycloak Admin API", ex);
    } catch (RestClientException ex) {
      throw new KeycloakAdminApiException("Failed to read user from Keycloak Admin API", ex);
    }
  }

  @Override
  public void updateUser(UUID userId, KeycloakUserRepresentation updatedUser) {
    try {
      restClient()
          .put()
          .uri("/users/{id}", userId)
          .contentType(MediaType.APPLICATION_JSON)
          .body(updatedUser)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException ex) {
      throw new KeycloakAdminApiException("Failed to update user via Keycloak Admin API", ex);
    }
  }

  @Override
  public UUID createUser(KeycloakUserRepresentation newUser) {
    try {
      ResponseEntity<Void> response =
          restClient()
              .post()
              .uri("/users")
              .contentType(MediaType.APPLICATION_JSON)
              .body(newUser)
              .retrieve()
              .toBodilessEntity();

      URI location = response == null ? null : response.getHeaders().getLocation();
      UUID createdId = parseCreatedUserId(location);
      if (createdId == null) {
        throw new KeycloakAdminApiException(
            "Keycloak did not return a Location header for created user");
      }
      return createdId;
    } catch (RestClientException ex) {
      throw new KeycloakAdminApiException("Failed to create user via Keycloak Admin API", ex);
    }
  }

  @Override
  public void deleteUser(UUID userId) {
    try {
      restClient().delete().uri("/users/{id}", userId).retrieve().toBodilessEntity();
    } catch (RestClientException ex) {
      throw new KeycloakAdminApiException("Failed to delete user via Keycloak Admin API", ex);
    }
  }

  @Override
  public void resetPassword(UUID userId, String password, boolean temporary) {
    KeycloakCredentialRepresentation credential = new KeycloakCredentialRepresentation();
    credential.setType("password");
    credential.setValue(password);
    credential.setTemporary(temporary);
    try {
      restClient()
          .put()
          .uri("/users/{id}/reset-password", userId)
          .contentType(MediaType.APPLICATION_JSON)
          .body(credential)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException ex) {
      throw new KeycloakAdminApiException("Failed to reset Keycloak user password", ex);
    }
  }

  @Override
  public KeycloakRoleRepresentation getRealmRoleByName(String roleName) {
    try {
      return restClient()
          .get()
          .uri("/roles/{roleName}", roleName)
          .retrieve()
          .body(KeycloakRoleRepresentation.class);
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() == 404) {
        return null;
      }
      throw new KeycloakAdminApiException("Failed to read Keycloak realm role", ex);
    } catch (RestClientException ex) {
      throw new KeycloakAdminApiException("Failed to read Keycloak realm role", ex);
    }
  }

  @Override
  public void addRealmRoles(UUID userId, List<KeycloakRoleRepresentation> roles) {
    try {
      restClient()
          .post()
          .uri("/users/{id}/role-mappings/realm", userId)
          .contentType(MediaType.APPLICATION_JSON)
          .body(roles)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException ex) {
      throw new KeycloakAdminApiException("Failed to assign Keycloak realm roles to user", ex);
    }
  }

  @Override
  public List<KeycloakRoleRepresentation> listUserRealmRoles(UUID userId) {
    try {
      KeycloakRoleRepresentation[] body =
          restClient()
              .get()
              .uri("/users/{id}/role-mappings/realm", userId)
              .retrieve()
              .body(KeycloakRoleRepresentation[].class);
      return body == null ? List.of() : List.of(body);
    } catch (RestClientException ex) {
      throw new KeycloakAdminApiException("Failed to list Keycloak user realm roles", ex);
    }
  }

  @Override
  public void removeRealmRoles(UUID userId, List<KeycloakRoleRepresentation> roles) {
    try {
      restClient()
          .method(org.springframework.http.HttpMethod.DELETE)
          .uri("/users/{id}/role-mappings/realm", userId)
          .contentType(MediaType.APPLICATION_JSON)
          .body(roles)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException ex) {
      throw new KeycloakAdminApiException("Failed to remove Keycloak user realm roles", ex);
    }
  }

  @Override
  public List<KeycloakUserRepresentation> listUsers(String search, Integer first, Integer max) {
    try {
      String uri =
          UriComponentsBuilder.fromPath("/users")
              .queryParamIfPresent("search", java.util.Optional.ofNullable(normalize(search)))
              .queryParamIfPresent("first", java.util.Optional.ofNullable(first))
              .queryParamIfPresent("max", java.util.Optional.ofNullable(max))
              .build()
              .toUriString();

      KeycloakUserRepresentation[] body =
          restClient().get().uri(uri).retrieve().body(KeycloakUserRepresentation[].class);
      return body == null ? List.of() : List.of(body);
    } catch (RestClientException ex) {
      throw new KeycloakAdminApiException("Failed to list users via Keycloak Admin API", ex);
    }
  }

  @Override
  public List<KeycloakUserSessionRepresentation> listUserSessions(UUID userId) {
    try {
      KeycloakUserSessionRepresentation[] body =
          restClient()
              .get()
              .uri("/users/{id}/sessions", userId)
              .retrieve()
              .body(KeycloakUserSessionRepresentation[].class);
      return body == null ? List.of() : List.of(body);
    } catch (RestClientException ex) {
      throw new KeycloakAdminApiException("Failed to list Keycloak user sessions", ex);
    }
  }

  @Override
  public void logoutUser(UUID userId) {
    try {
      restClient().post().uri("/users/{id}/logout", userId).retrieve().toBodilessEntity();
    } catch (RestClientException ex) {
      throw new KeycloakAdminApiException("Failed to logout Keycloak user sessions", ex);
    }
  }

  @Override
  public void executeActionsEmail(UUID userId, List<String> actions) {
    List<String> safeActions = actions == null ? List.of() : actions;
    try {
      restClient()
          .put()
          .uri("/users/{id}/execute-actions-email", userId)
          .contentType(MediaType.APPLICATION_JSON)
          .body(safeActions)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException ex) {
      throw new KeycloakAdminApiException("Failed to execute Keycloak email actions", ex);
    }
  }

  @Override
  public List<KeycloakClientRepresentation> listClients(String clientId) {
    try {
      String uri =
          UriComponentsBuilder.fromPath("/clients")
              .queryParamIfPresent("clientId", java.util.Optional.ofNullable(normalize(clientId)))
              .build()
              .toUriString();
      KeycloakClientRepresentation[] body =
          restClient().get().uri(uri).retrieve().body(KeycloakClientRepresentation[].class);
      return body == null ? List.of() : List.of(body);
    } catch (RestClientException ex) {
      throw new KeycloakAdminApiException("Failed to list Keycloak clients", ex);
    }
  }

  @Override
  public List<KeycloakUserSessionRepresentation> listClientUserSessions(String clientUuid) {
    try {
      KeycloakUserSessionRepresentation[] body =
          restClient()
              .get()
              .uri("/clients/{id}/user-sessions", clientUuid)
              .retrieve()
              .body(KeycloakUserSessionRepresentation[].class);
      return body == null ? List.of() : List.of(body);
    } catch (RestClientException ex) {
      throw new KeycloakAdminApiException("Failed to list Keycloak client user sessions", ex);
    }
  }

  @Override
  public void deleteSession(String sessionId) {
    try {
      restClient().delete().uri("/sessions/{id}", sessionId).retrieve().toBodilessEntity();
    } catch (RestClientException ex) {
      throw new KeycloakAdminApiException("Failed to delete Keycloak session", ex);
    }
  }

  private RestClient restClient() {
    String baseUrl = normalizeBaseUrl(properties.getBaseUrl());
    String realm = properties.getRealm();
    String adminBase = baseUrl + "/admin/realms/" + realm;
    String token = tokenProvider.getAccessToken();

    return RestClient.builder()
        .baseUrl(adminBase)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .build();
  }

  private String normalizeBaseUrl(String baseUrl) {
    if (baseUrl == null) {
      return "";
    }
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  private UUID parseCreatedUserId(URI location) {
    if (location == null) {
      return null;
    }
    String path = location.getPath();
    String raw = path == null ? location.toString() : path;
    if (raw == null) {
      return null;
    }
    String trimmed = raw.endsWith("/") ? raw.substring(0, raw.length() - 1) : raw;
    int lastSlash = trimmed.lastIndexOf('/');
    String idPart = lastSlash >= 0 ? trimmed.substring(lastSlash + 1) : trimmed;
    try {
      return UUID.fromString(idPart);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
