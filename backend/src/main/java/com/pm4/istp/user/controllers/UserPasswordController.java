package com.pm4.istp.user.controllers;

import static com.pm4.istp.shared.util.JwtUtil.parseUserId;

import com.pm4.istp.shared.keycloak.KeycloakAdminClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserPasswordController {
  private final KeycloakAdminClient keycloakAdminClient;

  @PostMapping("/me/password-reset-email")
  public ResponseEntity<Void> sendMyPasswordResetEmail(@AuthenticationPrincipal Jwt jwt) {
    keycloakAdminClient.executeActionsEmail(parseUserId(jwt), List.of("UPDATE_PASSWORD"));
    return ResponseEntity.noContent().build();
  }
}

