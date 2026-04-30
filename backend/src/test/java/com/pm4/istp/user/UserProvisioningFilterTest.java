package com.pm4.istp.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.db.entities.UserRoleEnum;
import com.pm4.istp.user.filters.UserProvisioningFilter;
import com.pm4.istp.user.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class UserProvisioningFilterTest {

  @Mock private UserRepository userRepository;
  @Mock private FilterChain filterChain;
  @Mock private Authentication authentication;
  @Mock private SecurityContext securityContext;
  @Mock private Jwt jwt;

  @InjectMocks private UserProvisioningFilter filter;

  private static final UUID USER_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    SecurityContextHolder.setContext(securityContext);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    lenient().when(authentication.isAuthenticated()).thenReturn(true);
    lenient().when(authentication.getPrincipal()).thenReturn(jwt);
    lenient().when(jwt.getSubject()).thenReturn(USER_ID.toString());
    lenient().when(jwt.getClaimAsString(anyString())).thenReturn(null);
    lenient().when(jwt.getTokenValue()).thenReturn(null);

    // Avoid NPEs in conflict detection.
    lenient().when(userRepository.findByEmailIgnoreCaseAndIdNot(anyString(), any(UUID.class)))
        .thenReturn(Optional.empty());
    lenient().when(userRepository.findByUsernameIgnoreCaseAndIdNot(anyString(), any(UUID.class)))
        .thenReturn(Optional.empty());
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void doFilterInternal_userMissingRole_returns403AndDoesNotInsert() throws Exception {
    when(jwt.getClaimAsString("preferred_username")).thenReturn("testuser");
    when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
    when(authentication.getAuthorities()).thenReturn(Set.of());
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/courses/catalog");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentType()).startsWith("application/json");
    assertThat(response.getContentAsString()).contains("missing required application role");

    verify(userRepository, never()).save(any(User.class));
    verify(filterChain, never()).doFilter(any(), any());
  }

  @Test
  void doFilterInternal_studentWithoutDbRow_createsShadowRowAndContinuesChain() throws Exception {
    when(jwt.getClaimAsString("preferred_username")).thenReturn("testuser");
    when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
    when(jwt.getClaimAsString("given_name")).thenReturn("Test");
    when(jwt.getClaimAsString("family_name")).thenReturn("User");
    when(authentication.getAuthorities())
        .thenReturn(
            (java.util.Collection)
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/courses/catalog");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    verify(userRepository).save(any(User.class));
    verify(filterChain).doFilter(any(), any());
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void doFilterInternal_studentWithoutDbRow_withIdentifierConflict_returns409AndDoesNotInsert()
      throws Exception {
    when(jwt.getClaimAsString("preferred_username")).thenReturn("testuser");
    when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
    when(authentication.getAuthorities())
        .thenReturn(
            (java.util.Collection)
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    User conflicting = new User();
    conflicting.setId(UUID.randomUUID());
    when(userRepository.findByEmailIgnoreCaseAndIdNot("test@example.com", USER_ID))
        .thenReturn(Optional.of(conflicting));

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/courses/catalog");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(409);
    assertThat(response.getContentType()).startsWith("application/json");
    assertThat(response.getContentAsString()).contains("Account conflict detected");

    verify(userRepository, never()).save(any(User.class));
    verify(filterChain, never()).doFilter(any(), any());
  }

  @Test
  void doFilterInternal_userProvisioned_continuesChainWithoutWrite() throws Exception {
    User existing = new User();
    existing.setId(USER_ID);
    existing.setEmail("test@example.com");
    existing.setUsername("testuser");
    existing.setFirstName("Test");
    existing.setLastName("User");
    existing.setName("Test User");
    existing.setRoles(Set.of(UserRoleEnum.ROLE_STUDENT));

    when(jwt.getClaimAsString("preferred_username")).thenReturn("testuser");
    when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
    when(jwt.getClaimAsString("given_name")).thenReturn("Test");
    when(jwt.getClaimAsString("family_name")).thenReturn("User");
    when(authentication.getAuthorities())
        .thenReturn(
            (java.util.Collection)
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/courses/catalog");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(any(), any());
    verify(userRepository, never()).save(any(User.class));
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void doFilterInternal_userProvisioned_profileChanged_updatesShadowUser() throws Exception {
    User existing = new User();
    existing.setId(USER_ID);
    existing.setEmail("test@example.com");
    existing.setUsername("testuser");
    existing.setFirstName("Old");
    existing.setLastName("Name");
    existing.setName("Old Name");
    existing.setRoles(Set.of(UserRoleEnum.ROLE_STUDENT));

    when(jwt.getClaimAsString("preferred_username")).thenReturn("testuser");
    when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
    when(jwt.getClaimAsString("given_name")).thenReturn("New");
    when(jwt.getClaimAsString("family_name")).thenReturn("Name");
    when(authentication.getAuthorities())
        .thenReturn(
            (java.util.Collection)
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/courses/catalog");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    verify(userRepository).save(any(User.class));
    assertThat(existing.getFirstName()).isEqualTo("New");
    assertThat(existing.getName()).isEqualTo("New Name");
    verify(filterChain).doFilter(any(), any());
  }

  @Test
  void doFilterInternal_userProvisioned_pictureFromToken_doesNotOverwriteDbValue() throws Exception {
    User existing = new User();
    existing.setId(USER_ID);
    existing.setEmail("test@example.com");
    existing.setUsername("testuser");
    existing.setFirstName("Test");
    existing.setLastName("User");
    existing.setName("Test User");
    existing.setPicture("https://db.example.com/p.png");
    existing.setRoles(Set.of(UserRoleEnum.ROLE_STUDENT));

    when(jwt.getClaimAsString("preferred_username")).thenReturn("testuser");
    when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
    when(jwt.getClaimAsString("given_name")).thenReturn("Test");
    when(jwt.getClaimAsString("family_name")).thenReturn("User");
    when(jwt.getClaimAsString("picture")).thenReturn("https://token.example.com/p.png");
    when(authentication.getAuthorities())
        .thenReturn(
            (java.util.Collection) List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/courses/catalog");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(any(), any());
    verify(userRepository, never()).save(any(User.class));
    assertThat(existing.getPicture()).isEqualTo("https://db.example.com/p.png");
  }

  @Test
  void doFilterInternal_userProvisioned_pictureClearedInDb_isNotReEnrichedFromToken()
      throws Exception {
    User existing = new User();
    existing.setId(USER_ID);
    existing.setEmail("test@example.com");
    existing.setUsername("testuser");
    existing.setFirstName("Test");
    existing.setLastName("User");
    existing.setName("Test User");
    existing.setPicture(null);
    existing.setRoles(Set.of(UserRoleEnum.ROLE_STUDENT));

    when(jwt.getClaimAsString("preferred_username")).thenReturn("testuser");
    when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
    when(jwt.getClaimAsString("given_name")).thenReturn("Test");
    when(jwt.getClaimAsString("family_name")).thenReturn("User");
    when(jwt.getClaimAsString("picture")).thenReturn("https://token.example.com/p.png");
    when(authentication.getAuthorities())
        .thenReturn(
            (java.util.Collection) List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/courses/catalog");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(any(), any());
    verify(userRepository, never()).save(any(User.class));
    assertThat(existing.getPicture()).isNull();
  }
}
