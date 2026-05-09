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
  private static final String EMAIL_CLAIM = "email";
  private static final String PICTURE_CLAIM = "picture";
  private static final String TITLE_CLAIM = "title";

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
    int timeoutMs = Math.clamp(timeout.toMillis(), 1, Integer.MAX_VALUE);

    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(timeoutMs);
    requestFactory.setReadTimeout(timeoutMs);

    userInfoRestClient = RestClient.builder().requestFactory(requestFactory).build();
  }

  @Override
  public void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    Optional<Jwt> jwt = authenticatedJwt();
    if (jwt.isPresent() && !provisionJwtUser(jwt.get(), response)) {
      return;
    }

    filterChain.doFilter(request, response);
  }

  private Optional<Jwt> authenticatedJwt() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof Jwt jwt) {
      return Optional.of(jwt);
    }
    return Optional.empty();
  }

  private boolean provisionJwtUser(Jwt jwt, HttpServletResponse response) {
    UUID keycloakId = UUID.fromString(jwt.getSubject());
    Optional<User> existingUser = userRepository.findById(keycloakId);
    UserClaims claims = extractClaims(jwt, keycloakId);
    Optional<UserInfoProfile> userInfoProfile = loadUserInfoProfile(jwt, existingUser, claims);
    ProvisioningProfile profile =
        resolveProvisioningProfile(keycloakId, existingUser, claims, userInfoProfile);

    if (profile.email() == null) {
      log.error("Cannot provision user {}: email is missing", keycloakId);
      try {
        response.sendError(
            HttpServletResponse.SC_BAD_REQUEST, "Unable to provision user: email is required");
      } catch (IOException ex) {
        log.warn("Failed to send missing-email response", ex);
      }
      return false;
    }

    Set<UserRoleEnum> roles = resolveApplicationRoles();
    if (!hasAppRole(roles)) {
      respondForbiddenJson(response, USER_MISSING_ROLE_ERROR);
      return false;
    }

    if (existingUser.isPresent()) {
      return provisionExistingUser(existingUser.get(), keycloakId, profile, roles, response);
    }
    return provisionNewUser(keycloakId, profile, roles, response);
  }

  private UserClaims extractClaims(Jwt jwt, UUID keycloakId) {
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
    String email =
        discardIfTooLong(
            normalizeLowercase(jwt.getClaimAsString(EMAIL_CLAIM)),
            MAX_COLUMN_LENGTH,
            EMAIL_CLAIM,
            keycloakId);
    String picture =
        discardIfTooLong(
            normalize(jwt.getClaimAsString(PICTURE_CLAIM)),
            MAX_PICTURE_LENGTH,
            PICTURE_CLAIM,
            keycloakId);
    String title =
        discardIfTooLong(
            normalize(jwt.getClaimAsString(TITLE_CLAIM)),
            MAX_COLUMN_LENGTH,
            TITLE_CLAIM,
            keycloakId);
    return new UserClaims(fullName, givenName, familyName, username, email, picture, title);
  }

  private Optional<UserInfoProfile> loadUserInfoProfile(
      Jwt jwt, Optional<User> existingUser, UserClaims claims) {
    String combinedName = combineNameParts(claims.givenName(), claims.familyName());
    return shouldFetchUserInfo(
            existingUser.orElse(null),
            claims.fullName(),
            combinedName,
            claims.username(),
            claims.email(),
            claims.picture(),
            claims.title())
        ? fetchUserInfoProfile(jwt)
        : Optional.empty();
  }

  private ProvisioningProfile resolveProvisioningProfile(
      UUID keycloakId,
      Optional<User> existingUser,
      UserClaims claims,
      Optional<UserInfoProfile> userInfoProfile) {
    String email = resolveEmail(existingUser, userInfoProfile, claims, keycloakId);
    String displayName =
        resolveStoredDisplayName(existingUser, userInfoProfile, claims, email, keycloakId);
    String picture =
        resolveManagedProfileValue(
            existingUser,
            claims.picture(),
            userInfoProfile.map(UserInfoProfile::picture).orElse(null),
            MAX_PICTURE_LENGTH,
            PICTURE_CLAIM,
            keycloakId,
            User::getPicture);
    String title =
        resolveManagedProfileValue(
            existingUser,
            claims.title(),
            userInfoProfile.map(UserInfoProfile::title).orElse(null),
            MAX_COLUMN_LENGTH,
            TITLE_CLAIM,
            keycloakId,
            User::getTitle);
    return new ProvisioningProfile(
        displayName,
        email,
        claims.username(),
        claims.givenName(),
        claims.familyName(),
        picture,
        title);
  }

  private String resolveEmail(
      Optional<User> existingUser,
      Optional<UserInfoProfile> userInfoProfile,
      UserClaims claims,
      UUID keycloakId) {
    return normalizeLowercase(
        firstNonBlank(
            claims.email(),
            discardIfTooLong(
                userInfoProfile.map(UserInfoProfile::email).orElse(null),
                MAX_COLUMN_LENGTH,
                EMAIL_CLAIM,
                keycloakId),
            existingUser.map(User::getEmail).map(this::normalize).orElse(null)));
  }

  private String resolveStoredDisplayName(
      Optional<User> existingUser,
      Optional<UserInfoProfile> userInfoProfile,
      UserClaims claims,
      String email,
      UUID keycloakId) {
    String combinedName = combineNameParts(claims.givenName(), claims.familyName());
    return discardIfTooLong(
        resolveDisplayName(
            claims.fullName(),
            combinedName,
            userInfoProfile.map(UserInfoProfile::name).orElse(null),
            existingUser.map(User::getName).map(this::normalize).orElse(null),
            claims.username(),
            email,
            keycloakId),
        MAX_COLUMN_LENGTH,
        "displayName",
        keycloakId);
  }

  private String resolveManagedProfileValue(
      Optional<User> existingUser,
      String claimValue,
      String userInfoValue,
      int maxLength,
      String fieldName,
      UUID keycloakId,
      java.util.function.Function<User, String> existingAccessor) {
    String existingValue = existingUser.map(existingAccessor).map(this::normalize).orElse(null);
    if (existingUser.isPresent()) {
      return existingValue;
    }
    return firstNonBlank(
        claimValue,
        discardIfTooLong(userInfoValue, maxLength, fieldName, keycloakId),
        existingValue);
  }

  private Set<UserRoleEnum> resolveApplicationRoles() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return Set.of();
    }
    Set<UserRoleEnum> rawRoles =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(UserRoleEnum::fromString)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
    return reduceToSingleAppRole(rawRoles);
  }

  private boolean hasAppRole(Set<UserRoleEnum> roles) {
    return roles.contains(UserRoleEnum.ROLE_STUDENT)
        || roles.contains(UserRoleEnum.ROLE_INSTRUCTOR)
        || roles.contains(UserRoleEnum.ROLE_ADMINISTRATOR);
  }

  private boolean provisionExistingUser(
      User user,
      UUID keycloakId,
      ProvisioningProfile profile,
      Set<UserRoleEnum> roles,
      HttpServletResponse response) {
    if (user.isDeleted()) {
      respondForbiddenJson(response, USER_DISABLED_ERROR);
      return false;
    }
    if (hasChangedIdentifierConflict(user, keycloakId, profile.email(), profile.username())) {
      respondConflictJson(response, USER_IDENTIFIER_CONFLICT_ERROR);
      return false;
    }
    updateProfileIfChanged(user, profile, roles);
    return !response.isCommitted();
  }

  private boolean provisionNewUser(
      UUID keycloakId,
      ProvisioningProfile profile,
      Set<UserRoleEnum> roles,
      HttpServletResponse response) {
    if (!roles.contains(UserRoleEnum.ROLE_STUDENT)) {
      respondUserNotProvisioned(response);
      return false;
    }
    if (hasIdentifierConflict(keycloakId, profile.email(), profile.username())) {
      respondConflictJson(response, USER_IDENTIFIER_CONFLICT_ERROR);
      return false;
    }
    User newUser = new User();
    newUser.setId(keycloakId);
    applyProfile(newUser, profile, roles);
    userRepository.save(newUser);
    return true;
  }

  private void updateProfileIfChanged(
      User user, ProvisioningProfile profile, Set<UserRoleEnum> roles) {
    if (profileChanged(user, profile, roles)) {
      applyProfile(user, profile, roles);
      userRepository.save(user);
    }
  }

  private boolean profileChanged(User user, ProvisioningProfile profile, Set<UserRoleEnum> roles) {
    return !Objects.equals(user.getName(), profile.displayName())
        || !Objects.equals(user.getEmail(), profile.email())
        || !Objects.equals(user.getUsername(), profile.username())
        || !Objects.equals(user.getFirstName(), profile.givenName())
        || !Objects.equals(user.getLastName(), profile.familyName())
        || !Objects.equals(user.getPicture(), profile.picture())
        || !Objects.equals(user.getTitle(), profile.title())
        || !Objects.equals(user.getRoles(), roles);
  }

  private void applyProfile(User user, ProvisioningProfile profile, Set<UserRoleEnum> roles) {
    user.setName(profile.displayName());
    user.setEmail(profile.email());
    user.setUsername(profile.username());
    user.setFirstName(profile.givenName());
    user.setLastName(profile.familyName());
    user.setPicture(profile.picture());
    user.setTitle(profile.title());
    user.setRoles(roles);
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

  private boolean hasChangedIdentifierConflict(
      User existingUser, UUID currentUserId, String email, String username) {
    if (existingUser == null) {
      return hasIdentifierConflict(currentUserId, email, username);
    }

    String existingEmail = normalizeLowercase(existingUser.getEmail());
    String normalizedEmail = normalizeLowercase(email);
    if (normalizedEmail != null
        && !Objects.equals(existingEmail, normalizedEmail)
        && userRepository.findByEmailIgnoreCaseAndIdNot(normalizedEmail, currentUserId).isPresent()) {
      log.warn("Login blocked due to conflicting email for user {}", currentUserId);
      return true;
    }

    String existingUsername = normalizeLowercase(existingUser.getUsername());
    String normalizedUsername = normalizeLowercase(username);
    if (normalizedUsername != null
        && !Objects.equals(existingUsername, normalizedUsername)
        && userRepository
            .findByUsernameIgnoreCaseAndIdNot(normalizedUsername, currentUserId)
            .isPresent()) {
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

  private record UserClaims(
      String fullName,
      String givenName,
      String familyName,
      String username,
      String email,
      String picture,
      String title) {}

  private record ProvisioningProfile(
      String displayName,
      String email,
      String username,
      String givenName,
      String familyName,
      String picture,
      String title) {}

  private record UserInfoProfile(String name, String email, String picture, String title) {}
}
