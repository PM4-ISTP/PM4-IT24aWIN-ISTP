package com.pm4.istp.shared;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {

  @Override
  public JwtAuthenticationToken convert(Jwt jwt) {
    Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
    return new JwtAuthenticationToken(jwt, authorities);
  }

  private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
    Set<String> roleAuthorities = extractRealmRoleAuthorities(jwt);
    Set<String> groupAuthorities = extractGroupAuthorities(jwt);

    return Stream.concat(roleAuthorities.stream(), groupAuthorities.stream())
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toSet());
  }

  private Set<String> extractRealmRoleAuthorities(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaim("realm_access");
    if (realmAccess == null || !realmAccess.containsKey("roles")) {
      return Collections.emptySet();
    }

    @SuppressWarnings("unchecked")
    List<String> roles = (List<String>) realmAccess.get("roles");
    if (roles == null) {
      return Collections.emptySet();
    }

    return roles.stream()
        .filter(role -> role != null && role.startsWith("ROLE_"))
        .collect(Collectors.toSet());
  }

  /**
   * Optionally map Keycloak group membership claim to authorities.
   *
   * <p>Requires a "Group Membership" protocol mapper in Keycloak that adds the "groups" claim to
   * the access token.
   */
  private Set<String> extractGroupAuthorities(Jwt jwt) {
    Object groupsClaim = jwt.getClaim("groups");
    if (!(groupsClaim instanceof List<?> groups)) {
      return Collections.emptySet();
    }

    return groups.stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(groupPath -> "GROUP_" + groupPath)
        .collect(Collectors.toSet());
  }
}
