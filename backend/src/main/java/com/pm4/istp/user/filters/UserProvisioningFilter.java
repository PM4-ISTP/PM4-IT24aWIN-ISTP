package com.pm4.istp.user.filters;

import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.db.entities.UserRoleEnum;
import com.pm4.istp.user.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserProvisioningFilter extends OncePerRequestFilter {

  private static final int MAX_COLUMN_LENGTH = 255;
  private static final int MAX_PICTURE_LENGTH = 2048;

  private final UserRepository userRepository;

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
  private String issuerUri;

  @Override
  public void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null
        && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof Jwt jwt) {

      UUID keycloakId = UUID.fromString(jwt.getSubject());
      Optional<User> existingUser = userRepository.findById(keycloakId);

      String fullName =
          discardIfTooLong(
              normalize(jwt.getClaimAsString("name")), MAX_COLUMN_LENGTH, "name", keycloakId);
      String givenName =
          discardIfTooLong(
              normalize(jwt.getClaimAsString("given_name")),
              MAX_COLUMN_LENGTH,
              "given_name",
              keycloakId);
      String familyName =
          discardIfTooLong(
              normalize(jwt.getClaimAsString("family_name")),
              MAX_COLUMN_LENGTH,
              "family_name",
              keycloakId);
      String username =
          discardIfTooLong(
              normalizeLowercase(jwt.getClaimAsString("preferred_username")),
              MAX_COLUMN_LENGTH,
              "preferred_username",
              keycloakId);
      String emailClaim =
          discardIfTooLong(
              normalizeLowercase(jwt.getClaimAsString("email")),
              MAX_COLUMN_LENGTH,
              "email",
              keycloakId);
      String pictureClaim =
          discardIfTooLong(
              normalize(jwt.getClaimAsString("picture")),
              MAX_PICTURE_LENGTH,
              "picture",
              keycloakId);
      String titleClaim =
          discardIfTooLong(
              normalize(jwt.getClaimAsString("title")), MAX_COLUMN_LENGTH, "title", keycloakId);
      String combinedName = combineNameParts(givenName, familyName);

      Optional<UserInfoProfile> userInfoProfile =
          shouldFetchUserInfo(
                  existingUser.orElse(null),
                  fullName,
                  combinedName,
                  username,
                  emailClaim,
                  pictureClaim,
                  titleClaim)
              ? fetchUserInfoProfile(jwt)
              : Optional.empty();

      String email =
          normalizeLowercase(
              firstNonBlank(
                  emailClaim,
                  discardIfTooLong(
                      userInfoProfile.map(UserInfoProfile::email).orElse(null),
                      MAX_COLUMN_LENGTH,
                      "email",
                      keycloakId),
                  existingUser.map(User::getEmail).map(this::normalize).orElse(null)));

      if (email == null) {
        log.error("Cannot provision user {}: email is missing", keycloakId);
        response.sendError(
            HttpServletResponse.SC_BAD_REQUEST, "Unable to provision user: email is required");
        return;
      }

      String displayName =
          discardIfTooLong(
              resolveDisplayName(
                  fullName,
                  combinedName,
                  userInfoProfile.map(UserInfoProfile::name).orElse(null),
                  existingUser.map(User::getName).map(this::normalize).orElse(null),
                  username,
                  email,
                  keycloakId),
              MAX_COLUMN_LENGTH,
              "displayName",
              keycloakId);
      String picture =
          firstNonBlank(
              pictureClaim,
              discardIfTooLong(
                  userInfoProfile.map(UserInfoProfile::picture).orElse(null),
                  MAX_PICTURE_LENGTH,
                  "picture",
                  keycloakId),
              existingUser.map(User::getPicture).map(this::normalize).orElse(null));
      String title =
          firstNonBlank(
              titleClaim,
              discardIfTooLong(
                  userInfoProfile.map(UserInfoProfile::title).orElse(null),
                  MAX_COLUMN_LENGTH,
                  "title",
                  keycloakId),
              existingUser.map(User::getTitle).map(this::normalize).orElse(null));

      Set<UserRoleEnum> roles =
          authentication.getAuthorities().stream()
              .map(GrantedAuthority::getAuthority)
              .map(UserRoleEnum::fromString)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toSet());

      existingUser.ifPresentOrElse(
          user -> {
            if (user.isDeleted()) {
              // Account was soft-deleted due to a conflict. Once deleted, always gone.
              return;
            }
            softDeleteConflicts(keycloakId, email, username);
            boolean profileChanged =
                !Objects.equals(user.getName(), displayName)
                    || !Objects.equals(user.getEmail(), email)
                    || !Objects.equals(user.getUsername(), username)
                    || !Objects.equals(user.getPicture(), picture)
                    || !Objects.equals(user.getTitle(), title)
                    || !Objects.equals(user.getRoles(), roles);
            if (profileChanged) {
              user.setName(displayName);
              user.setEmail(email);
              user.setUsername(username);
              user.setPicture(picture);
              user.setTitle(title);
              user.setRoles(roles);
              userRepository.save(user);
            }
          },
          () -> {
            // Soft-delete any active account that shares the same email or username but belongs
            // to a different Keycloak UUID (the old account was deleted in Keycloak and a new
            // one was created with the same credentials).
            softDeleteConflicts(keycloakId, email, username);

            User newUser = new User();
            newUser.setId(keycloakId);
            newUser.setName(displayName);
            newUser.setEmail(email);
            newUser.setUsername(username);
            newUser.setPicture(picture);
            newUser.setTitle(title);
            newUser.setRoles(roles);
            userRepository.save(newUser);
          });
    }

    filterChain.doFilter(request, response);
  }

  private boolean shouldFetchUserInfo(
      User existingUser,
      String fullName,
      String combinedName,
      String username,
      String email,
      String picture,
      String title) {
    String existingName = existingUser == null ? null : normalize(existingUser.getName());
    String existingEmail = existingUser == null ? null : normalize(existingUser.getEmail());
    String existingPicture = existingUser == null ? null : normalize(existingUser.getPicture());
    String existingTitle = existingUser == null ? null : normalize(existingUser.getTitle());

    boolean nameNeedsEnrichment =
        fullName == null
            && combinedName == null
            && (existingName == null || Objects.equals(existingName, username));
    boolean emailNeedsEnrichment = email == null && existingEmail == null;
    boolean pictureNeedsEnrichment = picture == null && existingPicture == null;
    boolean titleNeedsEnrichment = title == null && existingTitle == null;

    return nameNeedsEnrichment
        || emailNeedsEnrichment
        || pictureNeedsEnrichment
        || titleNeedsEnrichment;
  }

  private Optional<UserInfoProfile> fetchUserInfoProfile(Jwt jwt) {
    String tokenValue = normalize(jwt.getTokenValue());
    String normalizedIssuerUri = normalize(issuerUri);

    if (tokenValue == null || normalizedIssuerUri == null) {
      return Optional.empty();
    }

    try {
      UserInfoProfile profile =
          RestClient.create()
              .get()
              .uri(normalizedIssuerUri + "/protocol/openid-connect/userinfo")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValue)
              .retrieve()
              .body(UserInfoProfile.class);
      return Optional.ofNullable(profile);
    } catch (RestClientException exception) {
      log.debug("Failed to enrich user profile from Keycloak userinfo", exception);
      return Optional.empty();
    }
  }

  private String resolveDisplayName(
      String fullName,
      String combinedName,
      String userInfoName,
      String existingName,
      String username,
      String email,
      UUID keycloakId) {
    return firstNonBlank(
        fullName,
        combinedName,
        normalize(userInfoName),
        existingName,
        username,
        normalize(email),
        keycloakId.toString());
  }

  private String combineNameParts(String givenName, String familyName) {
    List<String> nameParts = new ArrayList<>();
    if (givenName != null) {
      nameParts.add(givenName);
    }
    if (familyName != null) {
      nameParts.add(familyName);
    }
    return nameParts.isEmpty() ? null : String.join(" ", nameParts);
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      String normalized = normalize(value);
      if (normalized != null) {
        return normalized;
      }
    }
    return null;
  }

  private String discardIfTooLong(String value, int maxLength, String fieldName, UUID keycloakId) {
    if (value != null && value.length() > maxLength) {
      log.warn(
          "Discarding {} value exceeding {} characters (length={}, userId={})",
          fieldName,
          maxLength,
          value.length(),
          keycloakId);
      return null;
    }
    return value;
  }

  private String normalize(String value) {
    if (value == null) {
      return null;
    }

    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private void softDeleteConflicts(UUID currentUserId, String email, String username) {
    if (email == null && username == null) {
      return;
    }

    LocalDateTime deletedAt = LocalDateTime.now();
    List<User> activeEmailConflicts =
        email == null
            ? List.of()
            : safeList(userRepository.findAllByEmailIgnoreCaseAndDeletedAtIsNull(email));
    List<User> activeUsernameConflicts =
        username == null
            ? List.of()
            : safeList(userRepository.findAllByUsernameIgnoreCaseAndDeletedAtIsNull(username));

    List<User> deletedEmailConflicts =
        email == null
            ? List.of()
            : safeList(userRepository.findAllByEmailIgnoreCaseAndDeletedAtIsNotNull(email));
    List<User> deletedUsernameConflicts =
        username == null
            ? List.of()
            : safeList(userRepository.findAllByUsernameIgnoreCaseAndDeletedAtIsNotNull(username));

    List<User> conflicts =
        new ArrayList<>(
            activeEmailConflicts.size()
                + activeUsernameConflicts.size()
                + deletedEmailConflicts.size()
                + deletedUsernameConflicts.size());
    conflicts.addAll(activeEmailConflicts);
    conflicts.addAll(activeUsernameConflicts);
    conflicts.addAll(deletedEmailConflicts);
    conflicts.addAll(deletedUsernameConflicts);

    Set<UUID> handledUserIds = new HashSet<>(conflicts.size());
    for (User conflictUser : conflicts) {
      if (conflictUser == null
          || conflictUser.getId() == null
          || conflictUser.getId().equals(currentUserId)
          || !handledUserIds.add(conflictUser.getId())) {
        continue;
      }

      boolean emailConflicted =
          email != null && Objects.equals(normalizeLowercase(conflictUser.getEmail()), email);
      boolean usernameConflicted =
          username != null
              && Objects.equals(normalizeLowercase(conflictUser.getUsername()), username);

      if (!emailConflicted && !usernameConflicted) {
        continue;
      }

      boolean changed = false;

      if (emailConflicted && !isInvalidEmail(conflictUser.getEmail())) {
        conflictUser.setEmail(toInvalidEmail(conflictUser.getEmail(), conflictUser.getId()));
        changed = true;
      }
      if (usernameConflicted && !isDeletedUsername(conflictUser.getUsername())) {
        conflictUser.setUsername(toInvalidUsername(conflictUser.getUsername(), conflictUser.getId()));
        changed = true;
      }

      if (!conflictUser.isDeleted()) {
        conflictUser.setDeletedAt(deletedAt);
        changed = true;
      }

      if (!changed) {
        continue;
      }
      userRepository.save(conflictUser);

      log.info(
          "Updated conflicting user {} due to conflict with user {} (email={}, username={}, deleted={})",
          conflictUser.getId(),
          currentUserId,
          emailConflicted,
          usernameConflicted,
          conflictUser.isDeleted());
    }
  }

  private List<User> safeList(List<User> users) {
    return users == null ? List.of() : users;
  }

  private boolean isInvalidEmail(String email) {
    String normalized = normalizeLowercase(email);
    return normalized != null && normalized.endsWith("@invalid.local");
  }

  private boolean isDeletedUsername(String username) {
    String normalized = normalizeLowercase(username);
    return normalized != null && normalized.contains("__deleted__");
  }

  private String normalizeLowercase(String value) {
    String normalized = normalize(value);
    if (normalized == null) {
      return null;
    }
    return normalized.toLowerCase(Locale.ROOT);
  }

  private String toInvalidEmail(String email, UUID userId) {
    if (userId == null) {
      return null;
    }
    String normalizedEmail = normalizeLowercase(email);
    String localPart = normalizedEmail == null ? "deleted" : normalizedEmail.split("@", 2)[0];

    String suffix = "+deleted-" + userId + "@invalid.local";
    int maxLocalPartLength = MAX_COLUMN_LENGTH - suffix.length();
    String safeLocalPart =
        maxLocalPartLength > 0
            ? localPart.substring(0, Math.min(localPart.length(), maxLocalPartLength))
            : "deleted";
    if (safeLocalPart.isEmpty()) {
      safeLocalPart = "deleted";
    }
    return safeLocalPart + suffix;
  }

  private String toInvalidUsername(String username, UUID userId) {
    if (userId == null) {
      return null;
    }
    String normalized = normalizeLowercase(username);
    String base = normalized == null ? "deleted" : normalized;
    String candidate = base + "__deleted__" + userId;
    if (candidate.length() <= MAX_COLUMN_LENGTH) {
      return candidate;
    }
    int maxBaseLen = MAX_COLUMN_LENGTH - ("__deleted__".length() + userId.toString().length());
    String truncated =
        maxBaseLen > 0 ? base.substring(0, Math.min(base.length(), maxBaseLen)) : "deleted";
    return truncated + "__deleted__" + userId;
  }

  private record UserInfoProfile(String name, String email, String picture, String title) {}
}
