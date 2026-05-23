package com.pm4.istp.user.controllers;

import static com.pm4.istp.shared.util.JwtUtil.parseUserId;

import com.pm4.istp.shared.keycloak.KeycloakAdminClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User Password", description = "Self-service password endpoints for the API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserPasswordController {
  private final KeycloakAdminClient keycloakAdminClient;

  @Operation(
      summary = "Request a password reset email",
      description =
          "Triggers a Keycloak password reset email for the authenticated user's own account.")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "204", description = "Password reset email sent")})
  @PostMapping("/me/password-reset-email")
  public ResponseEntity<Void> sendMyPasswordResetEmail(@AuthenticationPrincipal Jwt jwt) {
    keycloakAdminClient.executeActionsEmail(parseUserId(jwt), List.of("UPDATE_PASSWORD"));
    return ResponseEntity.noContent().build();
  }
}
