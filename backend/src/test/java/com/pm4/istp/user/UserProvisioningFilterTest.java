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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

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
    lenient().when(securityContext.getAuthentication()).thenReturn(authentication);

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
  void doFilterInternal_withoutAuthenticatedJwt_continuesChainWithoutProvisioning()
      throws Exception {
    when(authentication.isAuthenticated()).thenReturn(false);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/courses/catalog");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(any(), any());
    verify(userRepository, never()).findById(any());
    verify(userRepository, never()).save(any(User.class));
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
  void doFilterInternal_studentWithoutDbRow_normalizesAndStoresProfileClaims() throws Exception {
    when(jwt.getClaimAsString("preferred_username")).thenReturn(" TestUser ");
    when(jwt.getClaimAsString("email")).thenReturn(" Test@Example.COM ");
    when(jwt.getClaimAsString("name")).thenReturn(" Test User ");
    when(jwt.getClaimAsString("given_name")).thenReturn(" Test ");
    when(jwt.getClaimAsString("family_name")).thenReturn(" User ");
    when(jwt.getClaimAsString("picture")).thenReturn(" https://example.com/p.png ");
    when(jwt.getClaimAsString("title")).thenReturn(" Student ");
    when(authentication.getAuthorities())
        .thenReturn(
            (java.util.Collection) List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/courses/catalog");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    verify(userRepository).save(savedUser.capture());
    assertThat(savedUser.getValue().getUsername()).isEqualTo("testuser");
    assertThat(savedUser.getValue().getEmail()).isEqualTo("test@example.com");
    assertThat(savedUser.getValue().getName()).isEqualTo("Test User");
    assertThat(savedUser.getValue().getFirstName()).isEqualTo("Test");
    assertThat(savedUser.getValue().getLastName()).isEqualTo("User");
    assertThat(savedUser.getValue().getPicture()).isEqualTo("https://example.com/p.png");
    assertThat(savedUser.getValue().getTitle()).isEqualTo("Student");
    verify(filterChain).doFilter(any(), any());
  }

  @Test
  void doFilterInternal_studentWithoutDbRow_whenEmailMissing_returns400() throws Exception {
    when(jwt.getClaimAsString("preferred_username")).thenReturn("testuser");
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/courses/catalog");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getErrorMessage()).contains("email is required");
    verify(userRepository, never()).save(any(User.class));
    verify(filterChain, never()).doFilter(any(), any());
  }

  @Test
  void doFilterInternal_instructorWithoutDbRow_returns403AndDoesNotCreateUser() throws Exception {
    when(jwt.getClaimAsString("preferred_username")).thenReturn("instructor");
    when(jwt.getClaimAsString("email")).thenReturn("instructor@example.com");
    when(authentication.getAuthorities())
        .thenReturn(
            (java.util.Collection) List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/courses/catalog");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString()).contains("User not provisioned");
    verify(userRepository, never()).save(any(User.class));
    verify(filterChain, never()).doFilter(any(), any());
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
    verify(userRepository, never()).findByEmailIgnoreCaseAndIdNot(anyString(), any(UUID.class));
    verify(userRepository, never()).findByUsernameIgnoreCaseAndIdNot(anyString(), any(UUID.class));
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void doFilterInternal_userProvisioned_deletedUser_returns403() throws Exception {
    User existing = new User();
    existing.setId(USER_ID);
    existing.setEmail("test@example.com");
    existing.setUsername("testuser");
    existing.setFirstName("Test");
    existing.setLastName("User");
    existing.setName("Test User");
    existing.setDeletedAt(java.time.LocalDateTime.now());
    existing.setRoles(Set.of(UserRoleEnum.ROLE_STUDENT));

    when(jwt.getClaimAsString("preferred_username")).thenReturn("testuser");
    when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
    when(authentication.getAuthorities())
        .thenReturn(
            (java.util.Collection) List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/courses/catalog");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString()).contains("disabled");
    verify(userRepository, never()).save(any(User.class));
    verify(filterChain, never()).doFilter(any(), any());
  }

  @Test
  void doFilterInternal_userProvisioned_usernameConflict_returns409() throws Exception {
    User existing = new User();
    existing.setId(USER_ID);
    existing.setEmail("test@example.com");
    existing.setUsername("testuser");
    existing.setFirstName("Test");
    existing.setLastName("User");
    existing.setName("Test User");
    existing.setRoles(Set.of(UserRoleEnum.ROLE_STUDENT));
    User conflicting = new User();
    conflicting.setId(UUID.randomUUID());

    when(jwt.getClaimAsString("preferred_username")).thenReturn("otheruser");
    when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
    when(authentication.getAuthorities())
        .thenReturn(
            (java.util.Collection) List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
    when(userRepository.findByUsernameIgnoreCaseAndIdNot("otheruser", USER_ID))
        .thenReturn(Optional.of(conflicting));

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/courses/catalog");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(409);
    assertThat(response.getContentAsString()).contains("Account conflict detected");
    verify(userRepository, never()).save(any(User.class));
    verify(filterChain, never()).doFilter(any(), any());
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

  @Test
  void doFilterInternal_userProvisioned_multipleAppRoles_reducesToSingleHighestRole()
      throws Exception {
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
                List.of(
                    new SimpleGrantedAuthority("ROLE_STUDENT"),
                    new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/courses/catalog");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(any(), any());
    verify(userRepository).save(any(User.class));
    assertThat(existing.getRoles()).containsExactly(UserRoleEnum.ROLE_ADMINISTRATOR);
  }

  @Test
  void privateRoleAndTextHelpers_coverFallbackBranches() {
    assertThat(
            (Set<UserRoleEnum>)
                ReflectionTestUtils.invokeMethod(filter, "reduceToSingleAppRole", new Object[] {null}))
        .isEmpty();
    assertThat(
            (Set<UserRoleEnum>)
                ReflectionTestUtils.invokeMethod(
                    filter, "reduceToSingleAppRole", Set.of(UserRoleEnum.ROLE_INSTRUCTOR)))
        .containsExactly(UserRoleEnum.ROLE_INSTRUCTOR);
    assertThat(
            (Set<UserRoleEnum>)
                ReflectionTestUtils.invokeMethod(
                    filter, "reduceToSingleAppRole", Set.of(UserRoleEnum.ROLE_STUDENT)))
        .containsExactly(UserRoleEnum.ROLE_STUDENT);

    assertThat((String) ReflectionTestUtils.invokeMethod(filter, "combineNameParts", null, "Family"))
        .isEqualTo("Family");
    assertThat((String) ReflectionTestUtils.invokeMethod(filter, "combineNameParts", null, null))
        .isNull();
    assertThat(
            (String)
                ReflectionTestUtils.invokeMethod(
                    filter, "firstNonBlank", new Object[] {new String[] {" ", "value"}}))
        .isEqualTo("value");
    assertThat(
            (String)
                ReflectionTestUtils.invokeMethod(
                    filter,
                    "discardIfTooLong",
                    "x".repeat(256),
                    255,
                    "field",
                    USER_ID))
        .isNull();
    assertThat((String) ReflectionTestUtils.invokeMethod(filter, "normalizeLowercase", " Test@EXAMPLE.COM "))
        .isEqualTo("test@example.com");
  }

  @Test
  void privateIdentifierConflictHelpers_coverEmailUsernameAndNullCurrentUserBranches() {
    User conflicting = new User();
    conflicting.setId(UUID.randomUUID());

    assertThat(
            (Boolean)
                ReflectionTestUtils.invokeMethod(
                    filter, "hasIdentifierConflict", null, "test@example.com", "testuser"))
        .isFalse();

    when(userRepository.findByEmailIgnoreCaseAndIdNot("test@example.com", USER_ID))
        .thenReturn(Optional.of(conflicting));
    assertThat(
            (Boolean)
                ReflectionTestUtils.invokeMethod(
                    filter, "hasIdentifierConflict", USER_ID, "test@example.com", null))
        .isTrue();

    when(userRepository.findByUsernameIgnoreCaseAndIdNot("other", USER_ID))
        .thenReturn(Optional.of(conflicting));
    assertThat(
            (Boolean)
                ReflectionTestUtils.invokeMethod(
                    filter, "hasIdentifierConflict", USER_ID, null, "other"))
        .isTrue();

    User existing = new User();
    existing.setId(USER_ID);
    existing.setEmail("same@example.com");
    existing.setUsername("same");
    assertThat(
            (Boolean)
                ReflectionTestUtils.invokeMethod(
                    filter, "hasChangedIdentifierConflict", existing, USER_ID, "same@example.com", "same"))
        .isFalse();
  }
}
