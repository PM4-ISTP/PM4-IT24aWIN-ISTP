package com.pm4.istp.admin.controllers;

import com.pm4.istp.admin.dto.AdminCreateUserRequestDto;
import com.pm4.istp.admin.dto.AdminCreateUserResponseDto;
import com.pm4.istp.admin.dto.AdminProvisionUserResponseDto;
import com.pm4.istp.admin.dto.AdminSetUserPasswordRequestDto;
import com.pm4.istp.admin.dto.AdminUpdateUserRoleRequestDto;
import com.pm4.istp.admin.dto.AdminUserDetailDto;
import com.pm4.istp.admin.dto.AdminUserDirectoryItemDto;
import com.pm4.istp.admin.dto.AdminUserListItemDto;
import com.pm4.istp.admin.services.AdminUserService;
import com.pm4.istp.shared.dto.ErrorDto;
import com.pm4.istp.shared.keycloak.KeycloakUserSessionRepresentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Users", description = "Administrative endpoints for managing users")
@RestController
@RequestMapping(path = "/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
  private final AdminUserService adminUserService;

  @Operation(
      summary = "List the user directory",
      description =
          "Returns Keycloak user directory entries, optionally filtered by a search query and"
              + " paged via first/max offsets.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "User directory retrieved successfully")
      })
  @GetMapping("/directory")
  public ResponseEntity<List<AdminUserDirectoryItemDto>> listUserDirectory(
      @RequestParam(name = "q", required = false) String query,
      @RequestParam(name = "first", required = false) Integer first,
      @RequestParam(name = "max", required = false) Integer max) {
    return ResponseEntity.ok(adminUserService.listUserDirectory(query, first, max));
  }

  @Operation(
      summary = "List users",
      description =
          "Returns a paginated list of provisioned ISTP users, optionally filtered by a search"
              + " query.")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Users retrieved successfully")})
  @GetMapping
  public ResponseEntity<Page<AdminUserListItemDto>> listUsers(
      @RequestParam(name = "q", required = false) String query, Pageable pageable) {
    return ResponseEntity.ok(adminUserService.listUsers(query, pageable));
  }

  @Operation(summary = "Get a user", description = "Returns the full admin detail for one user.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User found",
            content = @Content(schema = @Schema(implementation = AdminUserDetailDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @GetMapping("/{userId}")
  public ResponseEntity<AdminUserDetailDto> getUser(@PathVariable UUID userId) {
    return ResponseEntity.ok(adminUserService.getUser(userId));
  }

  @Operation(
      summary = "Update a user's role",
      description =
          "Normalises the user to a single application role and returns the updated user.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role updated successfully",
            content = @Content(schema = @Schema(implementation = AdminUserDetailDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid role request",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PutMapping("/{userId}/roles")
  public ResponseEntity<AdminUserDetailDto> updateUserRole(
      @PathVariable UUID userId, @Valid @RequestBody AdminUpdateUserRoleRequestDto request) {
    return ResponseEntity.ok(adminUserService.updateUserRole(userId, request));
  }

  @Operation(
      summary = "Create a user",
      description =
          "Creates a new Keycloak user and returns the generated user ID and temporary password.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "User created successfully",
            content =
                @Content(schema = @Schema(implementation = AdminCreateUserResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PostMapping
  public ResponseEntity<AdminCreateUserResponseDto> createUser(
      @Valid @RequestBody AdminCreateUserRequestDto request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.createUser(request));
  }

  @Operation(
      summary = "Provision a user",
      description =
          "Creates the ISTP database record for an existing Keycloak account. Idempotent.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User provisioned successfully",
            content =
                @Content(schema = @Schema(implementation = AdminProvisionUserResponseDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PostMapping("/{userId}/provision")
  public ResponseEntity<AdminProvisionUserResponseDto> provisionUser(@PathVariable UUID userId) {
    return ResponseEntity.ok(adminUserService.provisionUser(userId));
  }

  @Operation(
      summary = "Disable a user",
      description = "Blocks the user from logging in and marks the account as disabled.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "User disabled successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PostMapping("/{userId}/disable")
  public ResponseEntity<Void> disableUser(@PathVariable UUID userId) {
    adminUserService.disableUser(userId);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Soft-delete a user",
      description =
          "Anonymises the user's email/username and disables the account to free the identifiers"
              + " for reuse. Cannot be undone.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "User soft-deleted successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PostMapping("/{userId}/soft-delete")
  public ResponseEntity<Void> softDeleteUser(@PathVariable UUID userId) {
    adminUserService.softDeleteUser(userId);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Restore a user",
      description = "Re-enables a previously disabled user account.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "User restored successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PostMapping("/{userId}/restore")
  public ResponseEntity<Void> restoreUser(@PathVariable UUID userId) {
    adminUserService.restoreUser(userId);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "List a user's sessions",
      description = "Returns the active Keycloak sessions for one user.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Sessions retrieved successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @GetMapping("/{userId}/sessions")
  public ResponseEntity<List<KeycloakUserSessionRepresentation>> listUserSessions(
      @PathVariable UUID userId) {
    return ResponseEntity.ok(adminUserService.listUserSessions(userId));
  }

  @Operation(
      summary = "Log out a user",
      description = "Terminates all active sessions of the given user.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "User logged out successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PostMapping("/{userId}/logout")
  public ResponseEntity<Void> logoutUser(@PathVariable UUID userId) {
    adminUserService.logoutUser(userId);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Send a password reset email",
      description = "Triggers a Keycloak password reset email for the given user.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Password reset email sent"),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PostMapping("/{userId}/password-reset-email")
  public ResponseEntity<Void> sendPasswordResetEmail(@PathVariable UUID userId) {
    adminUserService.sendPasswordResetEmail(userId);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Set a user's password",
      description = "Sets a new (optionally temporary) password for the given user.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Password set successfully"),
        @ApiResponse(
            responseCode = "400",
            description = "Password rejected by the Keycloak password policy",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PutMapping("/{userId}/password")
  public ResponseEntity<Void> setUserPassword(
      @PathVariable UUID userId, @Valid @RequestBody AdminSetUserPasswordRequestDto request) {
    adminUserService.setUserPassword(userId, request);
    return ResponseEntity.noContent().build();
  }
}
