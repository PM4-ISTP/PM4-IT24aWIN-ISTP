package com.pm4.istp.controller;

import com.pm4.istp.dto.KeycloakSessionDto;
import com.pm4.istp.dto.KeycloakUserDto;
import com.pm4.istp.service.KeycloakAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes read-only statistics from Keycloak (users and active sessions).
 *
 * <p>All endpoints require the {@code ROLE_ADMINISTRATOR} authority and delegate to {@link
 * KeycloakAdminService} for communication with the Keycloak Admin REST API.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/keycloak")
@RequiredArgsConstructor
@Tag(name = "Keycloak Admin", description = "Query authentication statistics from Keycloak")
public class KeycloakAdminController {

  private final KeycloakAdminService keycloakAdminService;

  @Operation(
      summary = "Get total user count",
      description = "Returns the total number of users registered in the Keycloak realm.")
  @ApiResponse(responseCode = "200", description = "Total user count")
  @ApiResponse(responseCode = "503", description = "Keycloak Admin API unavailable")
  @GetMapping("/users/count")
  public ResponseEntity<Integer> getUserCount() {
    return ResponseEntity.ok(keycloakAdminService.getUserCount());
  }

  @Operation(
      summary = "List users",
      description = "Returns a paginated list of users in the Keycloak realm.")
  @ApiResponse(responseCode = "200", description = "List of users")
  @ApiResponse(responseCode = "503", description = "Keycloak Admin API unavailable")
  @GetMapping("/users")
  public ResponseEntity<List<KeycloakUserDto>> getUsers(
      @RequestParam(defaultValue = "0") int first, @RequestParam(defaultValue = "50") int max) {
    return ResponseEntity.ok(keycloakAdminService.getUsers(first, max));
  }

  @Operation(
      summary = "Get active session count",
      description =
          "Returns the total number of active client sessions across all clients in the realm.")
  @ApiResponse(responseCode = "200", description = "Active session count")
  @ApiResponse(responseCode = "503", description = "Keycloak Admin API unavailable")
  @GetMapping("/sessions/count")
  public ResponseEntity<Integer> getActiveSessionCount() {
    return ResponseEntity.ok(keycloakAdminService.getActiveSessionCount());
  }

  @Operation(
      summary = "List sessions for a user",
      description = "Returns all active sessions for the given Keycloak user ID.")
  @ApiResponse(responseCode = "200", description = "List of active sessions for the user")
  @ApiResponse(responseCode = "503", description = "Keycloak Admin API unavailable")
  @GetMapping("/sessions/{userId}")
  public ResponseEntity<List<KeycloakSessionDto>> getUserSessions(@PathVariable String userId) {
    return ResponseEntity.ok(keycloakAdminService.getUserSessions(userId));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleException(Exception ex) {
    log.error("Keycloak Admin API error: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("Keycloak Admin API unavailable: " + ex.getMessage());
  }
}
