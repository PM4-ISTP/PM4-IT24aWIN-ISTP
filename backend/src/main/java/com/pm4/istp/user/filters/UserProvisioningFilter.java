package com.pm4.istp.user.filters;

import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.db.entities.UserRoleEnum;
import com.pm4.istp.user.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
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
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
  private static final String USER_NOT_PROVISIONED_ERROR =
      "{\"error\":\"User not provisioned. Contact an administrator.\"}";
  private static final String USER_DISABLED_ERROR = "{\"error\":\"User account is disabled.\"}";
  private static final String USER_MISSING_ROLE_ERROR =
      "{\"error\":\"User is missing required application role.\"}";
  private static final String USER_IDENTIFIER_CONFLICT_ERROR =
      "{\"error\":\"Account conflict detected. Contact an administrator.\"}";

  private final UserRepository userRepository;

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
  private String issuerUri;

  @Value("${istp.userinfo.enabled:true}")
  private boolean userInfoEnabled = true;

  @Value("${istp.userinfo.timeout:2s}")
  private Duration userInfoTimeout = Duration.ofSeconds(2);

  private RestClient userInfoRestClient;

  @PostConstruct
  void init() {
    Duration timeout = userInfoTimeout == null ? Duration.ofSeconds(2) : userInfoTimeout;
    int timeoutMs = (int) Math.max(1, Math.min(Integer.MAX_VALUE, timeout.toMillis()));

    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(timeoutMs);
    requestFactory.setReadTimeout(timeoutMs);

    userInfoRestClient = RestClient.builder().requestFactory(requestFactory).build();
  }

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
      String existingPicture = existingUser.map(User::getPicture).map(this::normalize).orElse(null);
      String existingTitle = existingUser.map(User::getTitle).map(this::normalize).orElse(null);

      // Profile fields like picture/title are managed by the ISTP app (admin/user profile pages).
      // Once a user is provisioned, do not overwrite these fields from token claims/userinfo, so
      // admin changes (including clearing values) persist.
      String picture =
          existingUser.isPresent()
              ? existingPicture
              : firstNonBlank(
                  pictureClaim,
                  discardIfTooLong(
                      userInfoProfile.map(UserInfoProfile::picture).orElse(null),
                      MAX_PICTURE_LENGTH,
                      "picture",
                      keycloakId),
                  existingPicture);
      String title =
          existingUser.isPresent()
              ? existingTitle
              : firstNonBlank(
                  titleClaim,
                  discardIfTooLong(
                      userInfoProfile.map(UserInfoProfile::title).orElse(null),
                      MAX_COLUMN_LENGTH,
                      "title",
                      keycloakId),
                  existingTitle);

      Set<UserRoleEnum> rawRoles =
          authentication.getAuthorities().stream()
              .map(GrantedAuthority::getAuthority)
              .map(UserRoleEnum::fromString)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toSet());
      Set<UserRoleEnum> roles = reduceToSingleAppRole(rawRoles);

      // Defense-in-depth:
      // Never allow access to the application domain unless the user has a known app role.
      boolean hasAppRole =
          roles.contains(UserRoleEnum.ROLE_STUDENT)
              || roles.contains(UserRoleEnum.ROLE_INSTRUCTOR)
              || roles.contains(UserRoleEnum.ROLE_ADMINISTRATOR);
      if (!hasAppRole) {
        respondForbiddenJson(response, USER_MISSING_ROLE_ERROR);
        return;
      }

      existingUser.ifPresentOrElse(
          user -> {
            if (user.isDeleted()) {
              respondForbiddenJson(response, USER_DISABLED_ERROR);
              return;
            }
            if (hasIdentifierConflict(keycloakId, email, username)) {
              respondConflictJson(response, USER_IDENTIFIER_CONFLICT_ERROR);
              return;
            }
            boolean profileChanged =
                !Objects.equals(user.getName(), displayName)
                    || !Objects.equals(user.getEmail(), email)
                    || !Objects.equals(user.getUsername(), username)
                    || !Objects.equals(user.getFirstName(), givenName)
                    || !Objects.equals(user.getLastName(), familyName)
                    || !Objects.equals(user.getPicture(), picture)
                    || !Objects.equals(user.getTitle(), title)
                    || !Objects.equals(user.getRoles(), roles);
            if (profileChanged) {
              user.setName(displayName);
              user.setEmail(email);
              user.setUsername(username);
              user.setFirstName(givenName);
              user.setLastName(familyName);
              user.setPicture(picture);
              user.setTitle(title);
              user.setRoles(roles);
              userRepository.save(user);
            }
          },
          () -> {
            // Just-in-time provisioning:
            // If a self-registered user has the STUDENT role, create the shadow DB row on first
            // request.
            if (roles.contains(UserRoleEnum.ROLE_STUDENT)) {
              if (hasIdentifierConflict(keycloakId, email, username)) {
                respondConflictJson(response, USER_IDENTIFIER_CONFLICT_ERROR);
                return;
              }

              User newUser = new User();
              newUser.setId(keycloakId);
              newUser.setName(displayName);
              newUser.setEmail(email);
              newUser.setUsername(username);
              newUser.setFirstName(givenName);
              newUser.setLastName(familyName);
              newUser.setPicture(picture);
              newUser.setTitle(title);
              newUser.setRoles(roles);
              userRepository.save(newUser);
              return;
            }

            // Non-students (e.g. admins/instructors) must be provisioned explicitly.
            respondUserNotProvisioned(response);
          });
      if (response.isCommitted()) {
        return;
      }
    }

    filterChain.doFilter(request, response);
  }

  private void respondUserNotProvisioned(HttpServletResponse response) {
    respondForbiddenJson(response, USER_NOT_PROVISIONED_ERROR);
  }

  private void respondConflictJson(HttpServletResponse response, String json) {
    try {
      response.setStatus(HttpServletResponse.SC_CONFLICT);
      response.setCharacterEncoding("UTF-8");
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write(json);
      response.flushBuffer();
    } catch (IOException ex) {
      log.warn("Failed to write 409 JSON response body", ex);
      try {
        response.sendError(HttpServletResponse.SC_CONFLICT);
      } catch (IOException ignored) {
        // nothing else we can do
      }
    }
  }

  private void respondForbiddenJson(HttpServletResponse response, String json) {
    try {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setCharacterEncoding("UTF-8");
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write(json);
      response.flushBuffer();
    } catch (IOException ex) {
      log.warn("Failed to write 403 JSON response body", ex);
      try {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
      } catch (IOException ignored) {
        // nothing else we can do
      }
    }
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
    if (!userInfoEnabled) {
      return Optional.empty();
    }

    String tokenValue = normalize(jwt.getTokenValue());
    String normalizedIssuerUri = normalize(issuerUri);

    if (tokenValue == null || normalizedIssuerUri == null) {
      return Optional.empty();
    }

    try {
      UserInfoProfile profile =
          userInfoRestClient
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

  private String normalizeLowercase(String value) {
    String normalized = normalize(value);
    return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
  }

  private boolean hasIdentifierConflict(UUID currentUserId, String email, String username) {
    if (currentUserId == null) {
      return false;
    }
    if (email != null
        && userRepository.findByEmailIgnoreCaseAndIdNot(email, currentUserId).isPresent()) {
      log.warn("Login blocked due to conflicting email for user {}", currentUserId);
      return true;
    }
    if (username != null
        && userRepository.findByUsernameIgnoreCaseAndIdNot(username, currentUserId).isPresent()) {
      log.warn("Login blocked due to conflicting username for user {}", currentUserId);
      return true;
    }
    return false;
  }

  private Set<UserRoleEnum> reduceToSingleAppRole(Set<UserRoleEnum> roles) {
    if (roles == null || roles.isEmpty()) {
      return Set.of();
    }
    if (roles.contains(UserRoleEnum.ROLE_ADMINISTRATOR)) {
      return Set.of(UserRoleEnum.ROLE_ADMINISTRATOR);
    }
    if (roles.contains(UserRoleEnum.ROLE_INSTRUCTOR)) {
      return Set.of(UserRoleEnum.ROLE_INSTRUCTOR);
    }
    if (roles.contains(UserRoleEnum.ROLE_STUDENT)) {
      return Set.of(UserRoleEnum.ROLE_STUDENT);
    }
    return Set.of();
  }

  private record UserInfoProfile(String name, String email, String picture, String title) {}
}
