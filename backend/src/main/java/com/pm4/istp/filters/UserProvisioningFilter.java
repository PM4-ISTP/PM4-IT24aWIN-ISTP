package com.pm4.istp.filters;

import com.pm4.istp.domain.entites.User;
import com.pm4.istp.domain.entites.UserRoleEnum;
import com.pm4.istp.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

  private final UserRepository userRepository;

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
  private String issuerUri;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null
        && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof Jwt jwt) {

      UUID keycloakId = UUID.fromString(jwt.getSubject());
      Optional<User> existingUser = userRepository.findById(keycloakId);

      String fullName =
          truncate(normalize(jwt.getClaimAsString("name")), MAX_COLUMN_LENGTH, "name", keycloakId);
      String givenName =
          truncate(
              normalize(jwt.getClaimAsString("given_name")),
              MAX_COLUMN_LENGTH,
              "given_name",
              keycloakId);
      String familyName =
          truncate(
              normalize(jwt.getClaimAsString("family_name")),
              MAX_COLUMN_LENGTH,
              "family_name",
              keycloakId);
      String username =
          truncate(
              normalize(jwt.getClaimAsString("preferred_username")),
              MAX_COLUMN_LENGTH,
              "preferred_username",
              keycloakId);
      String emailClaim =
          discardIfTooLong(
              normalize(jwt.getClaimAsString("email")), MAX_COLUMN_LENGTH, "email", keycloakId);
      String pictureClaim =
          discardIfTooLong(
              normalize(jwt.getClaimAsString("picture")), MAX_COLUMN_LENGTH, "picture", keycloakId);
      String titleClaim =
          truncate(
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
          firstNonBlank(
              emailClaim,
              discardIfTooLong(
                  userInfoProfile.map(UserInfoProfile::email).orElse(null),
                  MAX_COLUMN_LENGTH,
                  "email",
                  keycloakId),
              existingUser.map(User::getEmail).map(this::normalize).orElse(null));

      if (email == null) {
        log.error("Cannot provision user {}: email is missing", keycloakId);
        response.sendError(
            HttpServletResponse.SC_BAD_REQUEST, "Unable to provision user: email is required");
        return;
      }

      String displayName =
          truncate(
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
                  MAX_COLUMN_LENGTH,
                  "picture",
                  keycloakId),
              existingUser.map(User::getPicture).map(this::normalize).orElse(null));
      String title =
          firstNonBlank(
              titleClaim,
              truncate(
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
            if (!Objects.equals(user.getName(), displayName)
                || !Objects.equals(user.getEmail(), email)
                || !Objects.equals(user.getUsername(), username)
                || !Objects.equals(user.getPicture(), picture)
                || !Objects.equals(user.getTitle(), title)
                || !Objects.equals(user.getRoles(), roles)) {
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

  private String truncate(String value, int maxLength, String fieldName, UUID keycloakId) {
    if (value != null && value.length() > maxLength) {
      log.warn(
          "Truncating {} value exceeding {} characters (length={}, userId={})",
          fieldName,
          maxLength,
          value.length(),
          keycloakId);
      return value.substring(0, maxLength);
    }
    return value;
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

  private record UserInfoProfile(String name, String email, String picture, String title) {}
}
