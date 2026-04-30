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
import com.pm4.istp.shared.keycloak.KeycloakUserSessionRepresentation;
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

@RestController
@RequestMapping(path = "/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
  private final AdminUserService adminUserService;

  @GetMapping("/directory")
  public ResponseEntity<List<AdminUserDirectoryItemDto>> listUserDirectory(
      @RequestParam(name = "q", required = false) String query,
      @RequestParam(name = "first", required = false) Integer first,
      @RequestParam(name = "max", required = false) Integer max) {
    return ResponseEntity.ok(adminUserService.listUserDirectory(query, first, max));
  }

  @GetMapping
  public ResponseEntity<Page<AdminUserListItemDto>> listUsers(
      @RequestParam(name = "q", required = false) String query, Pageable pageable) {
    return ResponseEntity.ok(adminUserService.listUsers(query, pageable));
  }

  @GetMapping("/{userId}")
  public ResponseEntity<AdminUserDetailDto> getUser(@PathVariable UUID userId) {
    return ResponseEntity.ok(adminUserService.getUser(userId));
  }

  @PutMapping("/{userId}/roles")
  public ResponseEntity<AdminUserDetailDto> updateUserRole(
      @PathVariable UUID userId, @Valid @RequestBody AdminUpdateUserRoleRequestDto request) {
    return ResponseEntity.ok(adminUserService.updateUserRole(userId, request));
  }

  @PostMapping
  public ResponseEntity<AdminCreateUserResponseDto> createUser(
      @Valid @RequestBody AdminCreateUserRequestDto request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.createUser(request));
  }

  @PostMapping("/{userId}/provision")
  public ResponseEntity<AdminProvisionUserResponseDto> provisionUser(@PathVariable UUID userId) {
    return ResponseEntity.ok(adminUserService.provisionUser(userId));
  }

  @PostMapping("/{userId}/disable")
  public ResponseEntity<Void> disableUser(@PathVariable UUID userId) {
    adminUserService.disableUser(userId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{userId}/soft-delete")
  public ResponseEntity<Void> softDeleteUser(@PathVariable UUID userId) {
    adminUserService.softDeleteUser(userId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{userId}/restore")
  public ResponseEntity<Void> restoreUser(@PathVariable UUID userId) {
    adminUserService.restoreUser(userId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{userId}/sessions")
  public ResponseEntity<List<KeycloakUserSessionRepresentation>> listUserSessions(
      @PathVariable UUID userId) {
    return ResponseEntity.ok(adminUserService.listUserSessions(userId));
  }

  @PostMapping("/{userId}/logout")
  public ResponseEntity<Void> logoutUser(@PathVariable UUID userId) {
    adminUserService.logoutUser(userId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{userId}/password-reset-email")
  public ResponseEntity<Void> sendPasswordResetEmail(@PathVariable UUID userId) {
    adminUserService.sendPasswordResetEmail(userId);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{userId}/password")
  public ResponseEntity<Void> setUserPassword(
      @PathVariable UUID userId, @Valid @RequestBody AdminSetUserPasswordRequestDto request) {
    adminUserService.setUserPassword(userId, request);
    return ResponseEntity.noContent().build();
  }
}
