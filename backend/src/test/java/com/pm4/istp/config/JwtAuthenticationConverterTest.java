package com.pm4.istp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class JwtAuthenticationConverterTest {

    private JwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new JwtAuthenticationConverter();
    }

    private Jwt buildJwt(Map<String, Object> realmAccess) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", realmAccess)
                .build();
    }

    @Test
    void convert_withValidRoles_returnsTokenWithAuthorities() {
        Jwt jwt = buildJwt(Map.of("roles", List.of("ROLE_STUDENT", "ROLE_ADMIN")));

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
        Jwt jwt = buildJwt(Map.of(
                "roles", List.of("ROLE_STUDENT", "admin", "ROLE_INSTRUCTOR", "student")
        ));

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_STUDENT", "ROLE_INSTRUCTOR");
    }

    @Test
    void convert_withNullRealmAccess_returnsEmptyAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", null)  // test with null value
                .build();

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token).isNotNull();
        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    void convert_withRealmAccessMissingRolesKey_returnsEmptyAuthorities() {
        Jwt jwt = buildJwt(Map.of("other_key", "some_value"));

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    void convert_withEmptyRolesList_returnsEmptyAuthorities() {
        Jwt jwt = buildJwt(Map.of("roles", Collections.emptyList()));

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    void convert_withOnlyNonRolePrefixedRoles_returnsEmptyAuthorities() {
        Jwt jwt = buildJwt(Map.of("roles", List.of("admin", "user", "instructor")));

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    void convert_alwaysSetsJwtAsPrincipal() {
        Jwt jwt = buildJwt(Map.of("roles", Collections.emptyList()));

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getPrincipal()).isSameAs(jwt);
    }
}