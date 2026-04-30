package com.pm4.istp.user.controllers;

import com.pm4.istp.shared.keycloak.KeycloakAdminClient;
import com.pm4.istp.shared.keycloak.KeycloakUserRepresentation;
import com.pm4.istp.user.dto.ForgotPasswordRequestDto;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthPasswordController {
  private final KeycloakAdminClient keycloakAdminClient;

  @PostMapping("/forgot-password")
  public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
    String identifier = normalize(request == null ? null : request.getIdentifier());
    if (identifier == null) {
      return ResponseEntity.noContent().build();
    }

    try {
      List<KeycloakUserRepresentation> candidates =
          keycloakAdminClient.listUsers(identifier, 0, 20);
      KeycloakUserRepresentation match =
          candidates.stream().filter(u -> isMatch(u, identifier)).findFirst().orElse(null);
      if (match != null && match.getId() != null) {
        keycloakAdminClient.executeActionsEmail(
            java.util.UUID.fromString(match.getId()), List.of("UPDATE_PASSWORD"));
      }
      // Always return 204 to avoid user enumeration.
      return ResponseEntity.noContent().build();
    } catch (RuntimeException ex) {
      log.warn("Failed to request password reset email via Keycloak", ex);
      return ResponseEntity.status(502).build();
    }
  }

  private boolean isMatch(KeycloakUserRepresentation user, String identifier) {
    if (user == null) {
      return false;
    }
    String normalizedIdentifier = normalizeLower(identifier);
    if (normalizedIdentifier == null) {
      return false;
    }
    String email = normalizeLower(user.getEmail());
    String username = normalizeLower(user.getUsername());
    return normalizedIdentifier.equals(email) || normalizedIdentifier.equals(username);
  }

  private String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String normalizeLower(String value) {
    String normalized = normalize(value);
    return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
  }
}
