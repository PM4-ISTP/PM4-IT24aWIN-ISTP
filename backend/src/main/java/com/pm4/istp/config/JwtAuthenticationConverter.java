package com.pm4.istp.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {

  @Override
  public JwtAuthenticationToken convert(Jwt jwt) {
    Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
    return new JwtAuthenticationToken(jwt, authorities);
  }

  private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaim("realm_access");

    if (null == realmAccess || !realmAccess.containsKey("roles")) {
      return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    List<String> roles = (List<String>) realmAccess.get("roles");

    return roles.stream()
        .filter(role -> role.startsWith("ROLE_"))
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
  }
}
