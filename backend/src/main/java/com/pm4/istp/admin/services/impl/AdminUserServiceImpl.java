package com.pm4.istp.admin.services.impl;

import com.pm4.istp.admin.dto.AdminCreateUserRequestDto;
import com.pm4.istp.admin.dto.AdminCreateUserResponseDto;
import com.pm4.istp.admin.dto.AdminProvisionUserResponseDto;
import com.pm4.istp.admin.dto.AdminSetUserPasswordRequestDto;
import com.pm4.istp.admin.dto.AdminUpdateUserRoleRequestDto;
import com.pm4.istp.admin.dto.AdminUserDetailDto;
import com.pm4.istp.admin.dto.AdminUserDirectoryItemDto;
import com.pm4.istp.admin.dto.AdminUserListItemDto;
import com.pm4.istp.admin.services.AdminUserService;
import com.pm4.istp.shared.keycloak.KeycloakAdminClient;
import com.pm4.istp.shared.keycloak.KeycloakRoleRepresentation;
import com.pm4.istp.shared.keycloak.KeycloakUserRepresentation;
import com.pm4.istp.shared.keycloak.KeycloakUserSessionRepresentation;
import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.db.entities.UserRoleEnum;
import com.pm4.istp.user.exceptions.UserNotFoundException;
import com.pm4.istp.user.exceptions.UserProfileSyncException;
import com.pm4.istp.user.exceptions.UserSoftDeletedException;
import com.pm4.istp.user.repositories.UserRepository;
import com.pm4.istp.user.services.UserService;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {
  private static final String DEFAULT_NEW_USER_ROLE = "ROLE_STUDENT";
  private static final Set<String> MANAGED_APP_ROLES =
      Set.of("ROLE_STUDENT", "ROLE_INSTRUCTOR", "ROLE_ADMINISTRATOR");
  private static final String USER_NOT_FOUND_MSG = "User with ID '%s' not found";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final String TEMP_CREDENTIAL_LOWER = "abcdefghjkmnpqrstuvwxyz";
  private static final String TEMP_CREDENTIAL_UPPER = "ABCDEFGHJKMNPQRSTUVWXYZ";
  private static final String TEMP_CREDENTIAL_DIGIT = "23456789";
  // avoid ambiguous/shell-problematic chars; still counts as "special" for most policies
  private static final String TEMP_CREDENTIAL_SPECIAL = "!@$%*_-+";
  private static final DateTimeFormatter SOFT_DELETE_TS_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final KeycloakAdminClient keycloakAdminClient;
  private final UserRepository userRepository;
  private final UserService userService;

  @Override
  public List<AdminUserDirectoryItemDto> listUserDirectory(
      String search, Integer first, Integer max) {
    String normalizedSearch = normalizeOptional(search);
    int safeFirst = first == null || first < 0 ? 0 : first;
    int safeMax = max == null || max <= 0 ? 50 : Math.min(max, 200);

    List<KeycloakUserRepresentation> keycloakUsers =
        keycloakAdminClient.listUsers(normalizedSearch, safeFirst, safeMax);

    return keycloakUsers.stream()
        .map(this::toDirectoryItem)
        .filter(java.util.Objects::nonNull)
        .toList();
  }

  @Override
  public Page<AdminUserListItemDto> listUsers(String query, Pageable pageable) {
    String normalizedQuery = normalizeOptional(query);
    Page<User> page = userRepository.searchUsers(normalizedQuery, pageable);
    return page.map(this::toListItem);
  }

  @Override
  public AdminUserDetailDto getUser(UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }

    User user = userRepository.findById(userId).orElse(null);

    KeycloakUserRepresentation keycloakUser = keycloakAdminClient.getUser(userId);
    if (keycloakUser == null) {
      throw new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId));
    }

    String title =
        user != null ? user.getTitle() : getSingleAttribute(keycloakUser.getAttributes(), "title");
    String picture =
        user != null
            ? user.getPicture()
            : getSingleAttribute(keycloakUser.getAttributes(), "picture");

    Set<String> roles =
        user != null
            ? toRoleStrings(user.getRoles())
            : keycloakAdminClient.listUserRealmRoles(userId).stream()
                .map(KeycloakRoleRepresentation::getName)
                .filter(r -> r != null && MANAGED_APP_ROLES.contains(r))
                .collect(Collectors.toSet());

    String firstName =
        user != null ? user.getFirstName() : normalizeOptional(keycloakUser.getFirstName());
    String lastName =
        user != null ? user.getLastName() : normalizeOptional(keycloakUser.getLastName());
    String username =
        user != null ? user.getUsername() : normalizeOptional(keycloakUser.getUsername());
    String email = user != null ? user.getEmail() : normalizeOptional(keycloakUser.getEmail());
    String name =
        user != null ? user.getName() : buildDisplayName(firstName, lastName, username, email);

    return new AdminUserDetailDto(
        userId,
        name,
        email,
        username,
        firstName,
        lastName,
        title,
        picture,
        roles,
        user == null ? null : user.getDeletedAt(),
        user == null ? null : user.getAnonymizedAt(),
        user != null,
        keycloakUser);
  }

  @Override
  @Transactional
  public AdminUserDetailDto updateUserRole(UUID userId, AdminUpdateUserRoleRequestDto request) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }
    if (request == null || request.getRoles() == null || request.getRoles().isEmpty()) {
      throw new IllegalArgumentException("roles is required");
    }
    if (request.getRoles().size() != 1) {
      throw new IllegalArgumentException("Exactly one role must be provided");
    }

    Set<String> desired =
        request.getRoles().stream()
            .map(this::normalizeOptional)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());

    if (desired.size() != 1) {
      throw new IllegalArgumentException("Exactly one role must be provided");
    }
    if (desired.isEmpty() || !MANAGED_APP_ROLES.containsAll(desired)) {
      throw new IllegalArgumentException("Invalid app roles");
    }

    // Keycloak is source of truth for roles. Snapshot current app roles for rollback.
    List<KeycloakRoleRepresentation> currentRoleReps =
        keycloakAdminClient.listUserRealmRoles(userId);
    Set<String> current =
        currentRoleReps.stream()
            .map(KeycloakRoleRepresentation::getName)
            .filter(name -> name != null && MANAGED_APP_ROLES.contains(name))
            .collect(Collectors.toSet());

    Set<String> toAdd =
        desired.stream().filter(r -> !current.contains(r)).collect(Collectors.toSet());
    Set<String> toRemove =
        current.stream().filter(r -> !desired.contains(r)).collect(Collectors.toSet());

    List<KeycloakRoleRepresentation> addReps =
        toAdd.stream()
            .map(keycloakAdminClient::getRealmRoleByName)
            .filter(java.util.Objects::nonNull)
            .toList();
    List<KeycloakRoleRepresentation> removeReps =
        toRemove.stream()
            .map(keycloakAdminClient::getRealmRoleByName)
            .filter(java.util.Objects::nonNull)
            .toList();

    if (!removeReps.isEmpty()) {
      keycloakAdminClient.removeRealmRoles(userId, removeReps);
    }
    if (!addReps.isEmpty()) {
      keycloakAdminClient.addRealmRoles(userId, addReps);
    }

    // Sync DB projection (create if missing)
    User dbUser = userRepository.findById(userId).orElse(null);
    if (dbUser == null) {
      KeycloakUserRepresentation keycloakUser = keycloakAdminClient.getUser(userId);
      if (keycloakUser == null) {
        throw new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId));
      }

      dbUser = new User();
      dbUser.setId(userId);
      dbUser.setEmail(normalizeRequired(keycloakUser.getEmail(), "email"));
      dbUser.setUsername(normalizeOptional(keycloakUser.getUsername()));
      dbUser.setFirstName(normalizeOptional(keycloakUser.getFirstName()));
      dbUser.setLastName(normalizeOptional(keycloakUser.getLastName()));
      dbUser.setName(
          buildDisplayName(
              dbUser.getFirstName(),
              dbUser.getLastName(),
              dbUser.getUsername(),
              dbUser.getEmail()));
      dbUser.setTitle(getSingleAttribute(keycloakUser.getAttributes(), "title"));
      dbUser.setPicture(getSingleAttribute(keycloakUser.getAttributes(), "picture"));
    }

    try {
      dbUser.setRoles(
          desired.stream()
              .map(UserRoleEnum::fromString)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toSet()));
      userRepository.save(dbUser);
    } catch (RuntimeException ex) {
      // Rollback Keycloak roles best-effort
      try {
        // revert: remove newly added, re-add removed
        if (!addReps.isEmpty()) {
          keycloakAdminClient.removeRealmRoles(userId, addReps);
        }
        if (!removeReps.isEmpty()) {
          keycloakAdminClient.addRealmRoles(userId, removeReps);
        }
      } catch (RuntimeException rollbackEx) {
        log.error("Failed to rollback Keycloak role update for user {}", userId, rollbackEx);
      }
      throw new UserProfileSyncException("Failed to update user roles in application database", ex);
    }

    return getUser(userId);
  }

  @Override
  @Transactional
  public AdminCreateUserResponseDto createUser(AdminCreateUserRequestDto request) {
    String email = normalizeRequired(request.getEmail(), "email");
    String username = normalizeRequired(request.getUsername(), "username");
    String firstName = normalizeRequired(request.getFirstName(), "firstName");
    String lastName = normalizeRequired(request.getLastName(), "lastName");
    String title = normalizeOptional(request.getTitle());
    String pictureUrl = normalizeOptional(request.getPictureUrl());

    if (!userRepository.findAllByEmailIgnoreCaseAndDeletedAtIsNull(email).isEmpty()) {
      throw new IllegalArgumentException("Email is already in use");
    }
    if (!userRepository.findAllByUsernameIgnoreCaseAndDeletedAtIsNull(username).isEmpty()) {
      throw new IllegalArgumentException("Username is already in use");
    }

    KeycloakUserRepresentation keycloakUser = new KeycloakUserRepresentation();
    keycloakUser.setEnabled(true);
    keycloakUser.setEmail(email);
    keycloakUser.setUsername(username);
    keycloakUser.setFirstName(firstName);
    keycloakUser.setLastName(lastName);
    keycloakUser.setAttributes(buildAttributes(title, pictureUrl));

    UUID createdUserId = keycloakAdminClient.createUser(keycloakUser);

    String tempPassword = generateTemporaryCredential();
    try {
      keycloakAdminClient.resetPassword(createdUserId, tempPassword, true);

      KeycloakRoleRepresentation role =
          keycloakAdminClient.getRealmRoleByName(DEFAULT_NEW_USER_ROLE);
      if (role != null) {
        keycloakAdminClient.addRealmRoles(createdUserId, List.of(role));
      }

      User user = new User();
      user.setId(createdUserId);
      user.setEmail(email);
      user.setUsername(username);
      user.setFirstName(firstName);
      user.setLastName(lastName);
      user.setName(firstName + " " + lastName);
      user.setTitle(title);
      user.setPicture(pictureUrl);
      user.setRoles(Set.of(UserRoleEnum.ROLE_STUDENT));
      userRepository.save(user);

      return new AdminCreateUserResponseDto(createdUserId, tempPassword);
    } catch (RuntimeException ex) {
      try {
        keycloakAdminClient.deleteUser(createdUserId);
      } catch (RuntimeException cleanupEx) {
        log.error(
            "Failed to cleanup Keycloak user {} after provisioning failure",
            createdUserId,
            cleanupEx);
      }
      throw ex;
    }
  }

  @Override
  @Transactional
  public AdminProvisionUserResponseDto provisionUser(UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }

    User existing = userRepository.findById(userId).orElse(null);
    if (existing != null) {
      if (existing.getAnonymizedAt() != null) {
        throw new UserSoftDeletedException("User is soft-deleted and cannot be provisioned");
      }
      if (existing.getDeletedAt() != null) {
        throw new UserSoftDeletedException(
            "User is disabled/deleted. Restore the user instead of provisioning.");
      }
      return new AdminProvisionUserResponseDto(userId, false);
    }

    KeycloakUserRepresentation keycloakUser = keycloakAdminClient.getUser(userId);
    if (keycloakUser == null) {
      throw new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId));
    }

    // Provisioning means: shadow DB row + app roles in Keycloak
    try {
      KeycloakRoleRepresentation role =
          keycloakAdminClient.getRealmRoleByName(DEFAULT_NEW_USER_ROLE);
      if (role != null) {
        keycloakAdminClient.addRealmRoles(userId, List.of(role));
      }
    } catch (RuntimeException ex) {
      throw new UserProfileSyncException("Failed to assign Keycloak role during provisioning", ex);
    }

    String email = normalizeRequired(keycloakUser.getEmail(), "email");
    String username = normalizeOptional(keycloakUser.getUsername());
    String firstName = normalizeOptional(keycloakUser.getFirstName());
    String lastName = normalizeOptional(keycloakUser.getLastName());

    String title = getSingleAttribute(keycloakUser.getAttributes(), "title");
    String picture = getSingleAttribute(keycloakUser.getAttributes(), "picture");

    User user = new User();
    user.setId(userId);
    user.setEmail(email);
    user.setUsername(username);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setName(buildDisplayName(firstName, lastName, username, email));
    user.setTitle(title);
    user.setPicture(picture);
    user.setRoles(Set.of(UserRoleEnum.ROLE_STUDENT));
    userRepository.save(user);

    return new AdminProvisionUserResponseDto(userId, true);
  }

  @Override
  @Transactional
  public void disableUser(UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }

    KeycloakUserRepresentation before = keycloakAdminClient.getUser(userId);
    if (before == null) {
      throw new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId));
    }

    KeycloakUserRepresentation after = deepCopy(before);
    after.setEnabled(false);

    keycloakAdminClient.updateUser(userId, after);
    try {
      userService.softDeleteUser(userId);
    } catch (RuntimeException ex) {
      try {
        keycloakAdminClient.updateUser(userId, before);
      } catch (RuntimeException rollbackEx) {
        log.error("Failed to rollback Keycloak disable for user {}", userId, rollbackEx);
      }
      throw new UserProfileSyncException("Failed to soft-delete user in application database", ex);
    }
  }

  @Override
  @Transactional
  public void restoreUser(UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }

    User dbUser = userRepository.findById(userId).orElse(null);
    if (dbUser != null && dbUser.getAnonymizedAt() != null) {
      throw new UserSoftDeletedException("User is soft-deleted and cannot be restored");
    }

    KeycloakUserRepresentation before = keycloakAdminClient.getUser(userId);
    if (before == null) {
      throw new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId));
    }

    KeycloakUserRepresentation after = deepCopy(before);
    after.setEnabled(true);

    keycloakAdminClient.updateUser(userId, after);
    try {
      userService.restoreUser(userId);
    } catch (RuntimeException ex) {
      try {
        keycloakAdminClient.updateUser(userId, before);
      } catch (RuntimeException rollbackEx) {
        log.error("Failed to rollback Keycloak restore for user {}", userId, rollbackEx);
      }
      throw new UserProfileSyncException("Failed to restore user in application database", ex);
    }
  }

  @Override
  @Transactional
  public void softDeleteUser(UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }

    User existing = userRepository.findById(userId).orElse(null);
    if (existing != null && existing.getAnonymizedAt() != null) {
      // Idempotent: already soft-deleted
      return;
    }

    KeycloakUserRepresentation before = keycloakAdminClient.getUser(userId);
    if (before == null) {
      throw new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId));
    }

    String timestamp = SOFT_DELETE_TS_FORMAT.format(Instant.now().atOffset(ZoneOffset.UTC));
    String anonymizedEmail = toSoftDeletedEmail(before.getEmail(), timestamp, userId);
    String anonymizedUsername = toSoftDeletedUsername(before.getUsername(), timestamp, userId);

    KeycloakUserRepresentation after = deepCopy(before);
    after.setEmail(anonymizedEmail);
    after.setUsername(anonymizedUsername);
    after.setEnabled(false);

    keycloakAdminClient.updateUser(userId, after);
    try {
      userService.softDeleteAndAnonymizeUser(userId, anonymizedEmail, anonymizedUsername);
    } catch (RuntimeException ex) {
      // best-effort rollback: Keycloak identifiers are hard to revert safely; do not attempt to
      // restore original email/username
      throw new UserProfileSyncException("Failed to soft-delete user in application database", ex);
    }
  }

  @Override
  public List<KeycloakUserSessionRepresentation> listUserSessions(UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }
    return keycloakAdminClient.listUserSessions(userId);
  }

  @Override
  public void logoutUser(UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }
    keycloakAdminClient.logoutUser(userId);
  }

  @Override
  public void sendPasswordResetEmail(UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }
    keycloakAdminClient.executeActionsEmail(userId, List.of("UPDATE_PASSWORD"));
  }

  @Override
  public void setUserPassword(UUID userId, AdminSetUserPasswordRequestDto request) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }
    if (request == null || request.getPassword() == null) {
      throw new IllegalArgumentException("password is required");
    }
    keycloakAdminClient.resetPassword(userId, request.getPassword(), request.isTemporary());
  }

  private Map<String, List<String>> buildAttributes(String title, String pictureUrl) {
    Map<String, List<String>> attributes = new HashMap<>();
    if (title != null) {
      attributes.put("title", List.of(title));
    }
    if (pictureUrl != null) {
      attributes.put("picture", List.of(pictureUrl));
    }
    return attributes;
  }

  private String getSingleAttribute(Map<String, List<String>> attributes, String key) {
    if (attributes == null || key == null) {
      return null;
    }
    List<String> values = attributes.get(key);
    if (values == null || values.isEmpty()) {
      return null;
    }
    String value = values.getFirst();
    return normalizeOptional(value);
  }

  private String buildDisplayName(
      String firstName, String lastName, String username, String email) {
    String full = normalizeOptional(firstName);
    String last = normalizeOptional(lastName);
    if (full != null && last != null) {
      return full + " " + last;
    }
    if (full != null) {
      return full;
    }
    if (last != null) {
      return last;
    }
    String normalizedUsername = normalizeOptional(username);
    if (normalizedUsername != null) {
      return normalizedUsername;
    }
    return email;
  }

  private String generateTemporaryCredential() {
    // Generates a strong password that satisfies common Keycloak password policies
    // (uppercase/lowercase/digit/special + length).
    int length = 18;
    char[] password = new char[length];

    password[0] = randomChar(TEMP_CREDENTIAL_LOWER);
    password[1] = randomChar(TEMP_CREDENTIAL_UPPER);
    password[2] = randomChar(TEMP_CREDENTIAL_DIGIT);
    password[3] = randomChar(TEMP_CREDENTIAL_SPECIAL);

    String all =
        TEMP_CREDENTIAL_LOWER
            + TEMP_CREDENTIAL_UPPER
            + TEMP_CREDENTIAL_DIGIT
            + TEMP_CREDENTIAL_SPECIAL;
    for (int i = 4; i < length; i++) {
      password[i] = randomChar(all);
    }

    // shuffle
    for (int i = password.length - 1; i > 0; i--) {
      int j = SECURE_RANDOM.nextInt(i + 1);
      char tmp = password[i];
      password[i] = password[j];
      password[j] = tmp;
    }

    return new String(password);
  }

  private char randomChar(String alphabet) {
    if (alphabet == null || alphabet.isEmpty()) {
      return 'x';
    }
    return alphabet.charAt(SECURE_RANDOM.nextInt(alphabet.length()));
  }

  private String normalizeOptional(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String normalizeRequired(String value, String field) {
    String normalized = normalizeOptional(value);
    if (normalized == null) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return normalized;
  }

  private KeycloakUserRepresentation deepCopy(KeycloakUserRepresentation source) {
    KeycloakUserRepresentation copy = new KeycloakUserRepresentation();
    copy.setId(source.getId());
    copy.setUsername(source.getUsername());
    copy.setEmail(source.getEmail());
    copy.setEnabled(source.getEnabled());
    copy.setFirstName(source.getFirstName());
    copy.setLastName(source.getLastName());
    copy.setAttributes(source.getAttributes());
    return copy;
  }

  private AdminUserListItemDto toListItem(User user) {
    if (user == null) {
      return null;
    }
    return new AdminUserListItemDto(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getUsername(),
        user.getFirstName(),
        user.getLastName(),
        user.getTitle(),
        user.getPicture(),
        toRoleStrings(user.getRoles()),
        user.getDeletedAt(),
        user.getAnonymizedAt());
  }

  private String toSoftDeletedEmail(String originalEmail, String timestamp, UUID userId) {
    String normalized = normalizeOptional(originalEmail);
    String token =
        normalized == null
            ? "unknown"
            : normalized.toLowerCase().replace("@", "_at_").replace("+", "_");

    String prefix = "deleted+" + timestamp + "+";
    String suffix = "@invalid.local";
    String candidate = prefix + token + suffix;

    if (candidate.length() <= 255) {
      return candidate;
    }

    int maxTokenLen = 255 - prefix.length() - suffix.length();
    String truncated =
        maxTokenLen > 0 ? token.substring(0, Math.min(token.length(), maxTokenLen)) : "unknown";
    if (truncated.isBlank()) {
      truncated = "unknown";
    }
    return prefix + truncated + suffix;
  }

  private String toSoftDeletedUsername(String originalUsername, String timestamp, UUID userId) {
    String normalized = normalizeOptional(originalUsername);
    String base = normalized == null ? "user" : normalized;
    String prefix = "deleted_" + timestamp + "_";
    String candidate = prefix + base;
    if (candidate.length() <= 255) {
      return candidate;
    }
    int maxBaseLen = 255 - prefix.length();
    String truncated =
        maxBaseLen > 0 ? base.substring(0, Math.min(base.length(), maxBaseLen)) : "user";
    if (truncated.isBlank()) {
      truncated = "user";
    }
    return prefix + truncated;
  }

  private Set<String> toRoleStrings(Set<UserRoleEnum> roles) {
    if (roles == null || roles.isEmpty()) {
      return Set.of();
    }
    return roles.stream().map(UserRoleEnum::name).collect(Collectors.toSet());
  }

  private AdminUserDirectoryItemDto toDirectoryItem(KeycloakUserRepresentation kcUser) {
    if (kcUser == null || kcUser.getId() == null) {
      return null;
    }

    UUID id;
    try {
      id = UUID.fromString(kcUser.getId());
    } catch (IllegalArgumentException ex) {
      // If Keycloak uses non-UUID IDs, we can't map to our DB schema.
      return null;
    }

    User dbUser = userRepository.findById(id).orElse(null);
    boolean provisioned = dbUser != null;

    return new AdminUserDirectoryItemDto(
        id,
        kcUser.getEmail(),
        kcUser.getUsername(),
        kcUser.getFirstName(),
        kcUser.getLastName(),
        Boolean.TRUE.equals(kcUser.getEnabled()),
        provisioned,
        provisioned ? dbUser.getDeletedAt() : null,
        provisioned ? dbUser.getAnonymizedAt() : null,
        provisioned ? toRoleStrings(dbUser.getRoles()) : Set.of());
  }
}
