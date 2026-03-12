package com.pm4.istp.config;

import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;


class JwtAuthenticationConverterTest {

    private JwtAuthenticationConverter converter;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        converter = new JwtAuthenticationConverter();
        jwt = mock(Jwt.class);
    }

    @Test
    void convert_withValidRoles_returnsTokenWithAuthorities() {
        Map<String, Object> realmAccess = Map.of(
                "roles", List.of("ROLE_STUDENT", "ROLE_ADMIN")
        );
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token).isNotNull();
        assertThat(token.getPrincipal()).isEqualTo(jwt);

        Collection<GrantedAuthority> authorities = token.getAuthorities();
        assertThat(authorities).hasSize(2);
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_STUDENT", "ROLE_ADMIN");
    }

    @Test
    void convert_withNoRolePrefix_filtersOutNonRoleEntries() {
        Map<String, Object> realmAccess = Map.of(
                "roles", List.of("ROLE_STUDENT", "admin", "ROLE_INSTRUCTOR", "student")
        );
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_STUDENT", "ROLE_INSTRUCTOR");
    }

    @Test
    void convert_withNullRealmAccess_returnsEmptyAuthorities() {
        when(jwt.getClaim("realm_access")).thenReturn(null);

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token).isNotNull();
        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    void convert_withRealmAccessMissingRolesKey_returnsEmptyAuthorities() {
        Map<String, Object> realmAccess = Map.of("other_key", "some_value");
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    void convert_withEmptyRolesList_returnsEmptyAuthorities() {
        Map<String, Object> realmAccess = Map.of("roles", Collections.emptyList());
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    void convert_withOnlyNonRolePrefixedRoles_returnsEmptyAuthorities() {
        Map<String, Object> realmAccess = Map.of(
                "roles", List.of("admin", "user", "instructor")
        );
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    void convert_alwaysSetsJwtAsPrincipal() {
        when(jwt.getClaim("realm_access")).thenReturn(null);

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getPrincipal()).isSameAs(jwt);
    }
}