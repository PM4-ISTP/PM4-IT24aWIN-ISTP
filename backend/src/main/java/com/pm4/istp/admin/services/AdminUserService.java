package com.pm4.istp.admin.services;

import com.pm4.istp.admin.dto.AdminCreateUserRequestDto;
import com.pm4.istp.admin.dto.AdminCreateUserResponseDto;
import com.pm4.istp.admin.dto.AdminProvisionUserResponseDto;
import com.pm4.istp.admin.dto.AdminSetUserPasswordRequestDto;
import com.pm4.istp.admin.dto.AdminUpdateUserRoleRequestDto;
import com.pm4.istp.admin.dto.AdminUserDetailDto;
import com.pm4.istp.admin.dto.AdminUserDirectoryItemDto;
import com.pm4.istp.admin.dto.AdminUserListItemDto;
import com.pm4.istp.shared.keycloak.KeycloakUserSessionRepresentation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
  List<AdminUserDirectoryItemDto> listUserDirectory(String search, Integer first, Integer max);

  Page<AdminUserListItemDto> listUsers(String query, Pageable pageable);

  AdminUserDetailDto getUser(UUID userId);

  AdminUserDetailDto updateUserRole(UUID userId, AdminUpdateUserRoleRequestDto request);

  AdminCreateUserResponseDto createUser(AdminCreateUserRequestDto request);

  AdminProvisionUserResponseDto provisionUser(UUID userId);

  void disableUser(UUID userId);

  void restoreUser(UUID userId);

  void softDeleteUser(UUID userId);

  List<KeycloakUserSessionRepresentation> listUserSessions(UUID userId);

  void logoutUser(UUID userId);

  void sendPasswordResetEmail(UUID userId);

  void setUserPassword(UUID userId, AdminSetUserPasswordRequestDto request);
}
