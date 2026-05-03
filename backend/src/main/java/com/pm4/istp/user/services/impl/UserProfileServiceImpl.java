package com.pm4.istp.user.services.impl;

import com.pm4.istp.shared.keycloak.KeycloakAdminClient;
import com.pm4.istp.shared.keycloak.KeycloakUserRepresentation;
import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.dto.UpdateUserProfileRequestDto;
import com.pm4.istp.user.exceptions.UserNotFoundException;
import com.pm4.istp.user.exceptions.UserProfileSyncException;
import com.pm4.istp.user.repositories.UserRepository;
import com.pm4.istp.user.services.UserProfileService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {
  private static final String USER_NOT_FOUND_MSG = "User with ID '%s' not found";

  private static final String ATTR_PICTURE = "picture";
  private static final String ATTR_TITLE = "title";
  private static final String ROLE_ADMINISTRATOR = "ROLE_ADMINISTRATOR";

  private final UserRepository userRepository;
  private final KeycloakAdminClient keycloakAdminClient;

  @Override
  public User getProfile(UUID userId) {
    return userRepository
        .findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId)));
  }

  @Override
  public User updateProfile(
      UUID actorUserId,
      Collection<? extends GrantedAuthority> actorAuthorities,
      UUID targetUserId,
      UpdateUserProfileRequestDto request) {
    if (!targetUserId.equals(actorUserId) && !isAdmin(actorAuthorities)) {
      throw new AccessDeniedException("Users may only edit their own profile");
    }

    User user =
        userRepository
            .findByIdAndDeletedAtIsNull(targetUserId)
            .orElseThrow(
                () -> new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, targetUserId)));

    KeycloakUserRepresentation before = keycloakAdminClient.getUser(targetUserId);
    if (before == null) {
      throw new UserProfileSyncException("Keycloak user could not be loaded for update");
    }

    KeycloakUserRepresentation after = deepCopy(before);
    after.setFirstName(normalizeRequired(request.getFirstName(), "firstName"));
    after.setLastName(normalizeRequired(request.getLastName(), "lastName"));

    String title = normalizeOptional(request.getTitle());
    String pictureUrl = normalizeOptional(request.getPictureUrl());

    Map<String, List<String>> mergedAttributes = deepCopyAttributes(before.getAttributes());
    setSingleAttribute(mergedAttributes, ATTR_TITLE, title);
    setSingleAttribute(mergedAttributes, ATTR_PICTURE, pictureUrl);
    after.setAttributes(mergedAttributes);

    keycloakAdminClient.updateUser(targetUserId, after);

    try {
      user.setFirstName(after.getFirstName());
      user.setLastName(after.getLastName());
      user.setName(combineNameParts(after.getFirstName(), after.getLastName()));
      user.setTitle(title);
      user.setPicture(pictureUrl);
      return userRepository.save(user);
    } catch (RuntimeException ex) {
      try {
        keycloakAdminClient.updateUser(targetUserId, before);
      } catch (RuntimeException rollbackEx) {
        log.error(
            "Failed to rollback Keycloak user profile update for user {}",
            targetUserId,
            rollbackEx);
      }
      throw new UserProfileSyncException(
          "Failed to update user profile in application database", ex);
    }
  }

  private boolean isAdmin(Collection<? extends GrantedAuthority> authorities) {
    if (authorities == null) {
      return false;
    }
    return authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(a -> ROLE_ADMINISTRATOR.equalsIgnoreCase(a));
  }

  private KeycloakUserRepresentation deepCopy(KeycloakUserRepresentation source) {
    KeycloakUserRepresentation copy = new KeycloakUserRepresentation();
    copy.setId(source.getId());
    copy.setUsername(source.getUsername());
    copy.setEmail(source.getEmail());
    copy.setEnabled(source.getEnabled());
    copy.setFirstName(source.getFirstName());
    copy.setLastName(source.getLastName());
    copy.setAttributes(deepCopyAttributes(source.getAttributes()));
    return copy;
  }

  private Map<String, List<String>> deepCopyAttributes(Map<String, List<String>> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return new HashMap<>();
    }
    Map<String, List<String>> copy = new HashMap<>(attributes.size());
    for (Map.Entry<String, List<String>> entry : attributes.entrySet()) {
      if (entry.getKey() == null) {
        continue;
      }
      List<String> values = entry.getValue();
      copy.put(entry.getKey(), values == null ? null : new ArrayList<>(values));
    }
    return copy;
  }

  private void setSingleAttribute(Map<String, List<String>> attributes, String key, String value) {
    if (attributes == null || key == null) {
      return;
    }
    if (value == null) {
      attributes.remove(key);
      return;
    }
    attributes.put(key, List.of(value));
  }

  private String normalizeOptional(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String normalizeRequired(String value, String field) {
    String trimmed = normalizeOptional(value);
    if (trimmed == null) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return trimmed;
  }

  private String combineNameParts(String firstName, String lastName) {
    String normalizedFirst = normalizeOptional(firstName);
    String normalizedLast = normalizeOptional(lastName);
    if (normalizedFirst == null && normalizedLast == null) {
      return null;
    }
    if (normalizedFirst == null) {
      return normalizedLast;
    }
    if (normalizedLast == null) {
      return normalizedFirst;
    }
    return normalizedFirst + " " + normalizedLast;
  }

  @Override
  public long addOnlineTime(UUID userId, long seconds) {
    if (seconds <= 0) {
      User user =
          userRepository
              .findByIdAndDeletedAtIsNull(userId)
              .orElseThrow(
                  () -> new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId)));
      return user.getTotalSecondsOnline();
    }
    User user =
        userRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(
                () -> new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId)));
    user.setTotalSecondsOnline(user.getTotalSecondsOnline() + seconds);
    return userRepository.save(user).getTotalSecondsOnline();
  }
}
