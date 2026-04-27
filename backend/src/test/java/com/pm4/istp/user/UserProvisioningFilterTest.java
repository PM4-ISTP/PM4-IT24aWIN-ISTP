package com.pm4.istp.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.db.entities.UserRoleEnum;
import com.pm4.istp.user.filters.UserProvisioningFilter;
import com.pm4.istp.user.repositories.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class UserProvisioningFilterTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private FilterChain filterChain;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Jwt jwt;

    @InjectMocks
    private UserProvisioningFilter filter;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String USERNAME = "testuser";
    private static final String FULL_NAME = "Test User";
    private static final String EMAIL = "test@example.com";
    private static final String PICTURE = "https://example.com/avatar.png";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().doReturn(null).when(jwt).getClaimAsString(anyString());
        lenient().doReturn(null).when(jwt).getTokenValue();
        lenient().when(userRepository.findAllByEmailIgnoreCaseAndDeletedAtIsNull(anyString())).thenReturn(java.util.List.of());
        lenient().when(userRepository.findAllByUsernameIgnoreCaseAndDeletedAtIsNull(anyString())).thenReturn(java.util.List.of());
        lenient().when(userRepository.findAllByEmailIgnoreCaseAndDeletedAtIsNotNull(anyString())).thenReturn(java.util.List.of());
        lenient().when(userRepository.findAllByUsernameIgnoreCaseAndDeletedAtIsNotNull(anyString())).thenReturn(java.util.List.of());
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_newUser_savesUserAndContinuesChain() throws Exception {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(USER_ID.toString());
        doReturn(USERNAME).when(jwt).getClaimAsString("preferred_username");
        doReturn(FULL_NAME).when(jwt).getClaimAsString("name");
        doReturn(EMAIL).when(jwt).getClaimAsString("email");
        doReturn(PICTURE).when(jwt).getClaimAsString("picture");
        when(authentication.getAuthorities()).thenReturn(Set.of());

        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getId()).isEqualTo(USER_ID);
        assertThat(savedUser.getName()).isEqualTo(FULL_NAME);
        assertThat(savedUser.getEmail()).isEqualTo(EMAIL);
        assertThat(savedUser.getUsername()).isEqualTo(USERNAME);
        assertThat(savedUser.getPicture()).isEqualTo(PICTURE);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_newKeycloakId_sameEmail_deactivatesConflictingUserAndSavesNewUser() throws Exception {
        UUID conflictingUserId = UUID.randomUUID();
        User conflictingUser = new User();
        conflictingUser.setId(conflictingUserId);
        conflictingUser.setName("Old Name");
        conflictingUser.setEmail(EMAIL);
        conflictingUser.setUsername("olduser");
        conflictingUser.setDeletedAt(null);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(USER_ID.toString());
        doReturn(USERNAME).when(jwt).getClaimAsString("preferred_username");
        doReturn(FULL_NAME).when(jwt).getClaimAsString("name");
        doReturn(EMAIL).when(jwt).getClaimAsString("email");
        doReturn(PICTURE).when(jwt).getClaimAsString("picture");
        when(authentication.getAuthorities()).thenReturn(Set.of());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(userRepository.findAllByEmailIgnoreCaseAndDeletedAtIsNull(EMAIL)).thenReturn(java.util.List.of(conflictingUser));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(conflictingUser.getDeletedAt()).isNotNull();
        assertThat(conflictingUser.getEmail()).contains("@invalid.local");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.atLeast(2)).save(userCaptor.capture());
        assertThat(userCaptor.getAllValues()).anyMatch(saved -> USER_ID.equals(saved.getId()) && EMAIL.equals(saved.getEmail()));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_newKeycloakId_sameUsername_deactivatesConflictingUserAndSavesNewUser() throws Exception {
        UUID conflictingUserId = UUID.randomUUID();
        User conflictingUser = new User();
        conflictingUser.setId(conflictingUserId);
        conflictingUser.setName("Old Name");
        conflictingUser.setEmail("old@example.com");
        conflictingUser.setUsername(USERNAME);
        conflictingUser.setDeletedAt(null);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(USER_ID.toString());
        doReturn(USERNAME).when(jwt).getClaimAsString("preferred_username");
        doReturn(FULL_NAME).when(jwt).getClaimAsString("name");
        doReturn(EMAIL).when(jwt).getClaimAsString("email");
        doReturn(PICTURE).when(jwt).getClaimAsString("picture");
        when(authentication.getAuthorities()).thenReturn(Set.of());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(userRepository.findAllByUsernameIgnoreCaseAndDeletedAtIsNull(USERNAME)).thenReturn(java.util.List.of(conflictingUser));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(conflictingUser.getDeletedAt()).isNotNull();
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.atLeast(2)).save(userCaptor.capture());
        assertThat(userCaptor.getAllValues()).anyMatch(saved -> USER_ID.equals(saved.getId()) && USERNAME.equals(saved.getUsername()));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_existingUser_sameEmailAndUsername_doesNotDeleteAnything() throws Exception {
        User existingUser = new User();
        existingUser.setId(USER_ID);
        existingUser.setName(FULL_NAME);
        existingUser.setEmail(EMAIL);
        existingUser.setUsername(USERNAME);
        existingUser.setPicture(PICTURE);
        existingUser.setRoles(Set.of());

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(USER_ID.toString());
        doReturn(USERNAME).when(jwt).getClaimAsString("preferred_username");
        doReturn(FULL_NAME).when(jwt).getClaimAsString("name");
        doReturn(EMAIL).when(jwt).getClaimAsString("email");
        doReturn(PICTURE).when(jwt).getClaimAsString("picture");
        when(authentication.getAuthorities()).thenReturn(Set.of());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        filter.doFilterInternal(request, response, filterChain);

        verify(userRepository, never()).delete(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_newUser_mixedCaseEmailAndUsername_normalizesAndFindsIgnoreCaseConflicts() throws Exception {
        UUID conflictingUserId = UUID.randomUUID();
        User conflictingUser = new User();
        conflictingUser.setId(conflictingUserId);
        conflictingUser.setName("Old Name");
        conflictingUser.setEmail("test@example.com");
        conflictingUser.setUsername("testuser");
        conflictingUser.setDeletedAt(null);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(USER_ID.toString());
        doReturn("TeStUsEr").when(jwt).getClaimAsString("preferred_username");
        doReturn(FULL_NAME).when(jwt).getClaimAsString("name");
        doReturn("TeSt@Example.com").when(jwt).getClaimAsString("email");
        doReturn(PICTURE).when(jwt).getClaimAsString("picture");
        when(authentication.getAuthorities()).thenReturn(Set.of());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(userRepository.findAllByEmailIgnoreCaseAndDeletedAtIsNull("test@example.com")).thenReturn(java.util.List.of(conflictingUser));
        when(userRepository.findAllByUsernameIgnoreCaseAndDeletedAtIsNull("testuser")).thenReturn(java.util.List.of());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(conflictingUser.getDeletedAt()).isNotNull();
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.atLeast(2)).save(userCaptor.capture());
        assertThat(userCaptor.getAllValues()).anyMatch(saved -> "test@example.com".equals(saved.getEmail()) && "testuser".equals(saved.getUsername()));
    }

    @Test
    void doFilterInternal_newUser_withoutNameClaim_usesGivenAndFamilyName() throws Exception {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(USER_ID.toString());
        doReturn("Test").when(jwt).getClaimAsString("given_name");
        doReturn("User").when(jwt).getClaimAsString("family_name");
        doReturn(USERNAME).when(jwt).getClaimAsString("preferred_username");
        doReturn(EMAIL).when(jwt).getClaimAsString("email");
        doReturn(PICTURE).when(jwt).getClaimAsString("picture");
        when(authentication.getAuthorities()).thenReturn(Set.of());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getName()).isEqualTo(FULL_NAME);
    }

    @Test
    void doFilterInternal_newUser_withoutFullName_usesUsernameFallback() throws Exception {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(USER_ID.toString());
        doReturn(USERNAME).when(jwt).getClaimAsString("preferred_username");
        doReturn(EMAIL).when(jwt).getClaimAsString("email");
        doReturn(PICTURE).when(jwt).getClaimAsString("picture");
        when(authentication.getAuthorities()).thenReturn(Set.of());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getName()).isEqualTo(USERNAME);
    }

    @Test
    void doFilterInternal_existingUser_doesNotSaveAndContinuesChain() throws Exception {
        User existingUser = new User();
        existingUser.setId(USER_ID);
        existingUser.setName(FULL_NAME);
        existingUser.setEmail(EMAIL);
        existingUser.setPicture(PICTURE);
        existingUser.setRoles(Set.of(UserRoleEnum.ROLE_INSTRUCTOR));
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(USER_ID.toString());
        doReturn(FULL_NAME).when(jwt).getClaimAsString("name");
        doReturn(EMAIL).when(jwt).getClaimAsString("email");
        doReturn(PICTURE).when(jwt).getClaimAsString("picture");
        doReturn(Set.of((GrantedAuthority) UserRoleEnum.ROLE_INSTRUCTOR::name))
                .when(authentication)
                .getAuthorities();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        filter.doFilterInternal(request, response, filterChain);

        verify(userRepository, never()).save(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_oversizedPicture_discardsPictureAndSavesUser() throws Exception {
        String oversizedPicture = "https://example.com/" + "a".repeat(2040);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(USER_ID.toString());
        doReturn(USERNAME).when(jwt).getClaimAsString("preferred_username");
        doReturn(FULL_NAME).when(jwt).getClaimAsString("name");
        doReturn(EMAIL).when(jwt).getClaimAsString("email");
        doReturn(oversizedPicture).when(jwt).getClaimAsString("picture");
        when(authentication.getAuthorities()).thenReturn(Set.of());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPicture()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_longButValidPicture_savesPictureUrl() throws Exception {
        String longValidPicture = "https://example.com/" + "a".repeat(2028);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(USER_ID.toString());
        doReturn(USERNAME).when(jwt).getClaimAsString("preferred_username");
        doReturn(FULL_NAME).when(jwt).getClaimAsString("name");
        doReturn(EMAIL).when(jwt).getClaimAsString("email");
        doReturn(longValidPicture).when(jwt).getClaimAsString("picture");
        when(authentication.getAuthorities()).thenReturn(Set.of());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPicture()).isEqualTo(longValidPicture);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_oversizedUsername_discardsUsernameAndSavesUser() throws Exception {
        String oversizedUsername = "u".repeat(300);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(USER_ID.toString());
        doReturn(oversizedUsername).when(jwt).getClaimAsString("preferred_username");
        doReturn(FULL_NAME).when(jwt).getClaimAsString("name");
        doReturn(EMAIL).when(jwt).getClaimAsString("email");
        when(authentication.getAuthorities()).thenReturn(Set.of());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_oversizedName_discardsNameAndUsesFallback() throws Exception {
        String oversizedName = "n".repeat(300);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(USER_ID.toString());
        doReturn(USERNAME).when(jwt).getClaimAsString("preferred_username");
        doReturn(oversizedName).when(jwt).getClaimAsString("name");
        doReturn(EMAIL).when(jwt).getClaimAsString("email");
        when(authentication.getAuthorities()).thenReturn(Set.of());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getName()).isEqualTo(USERNAME);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_oversizedTitle_discardsTitleAndSavesUser() throws Exception {
        String oversizedTitle = "t".repeat(300);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(USER_ID.toString());
        doReturn(USERNAME).when(jwt).getClaimAsString("preferred_username");
        doReturn(FULL_NAME).when(jwt).getClaimAsString("name");
        doReturn(EMAIL).when(jwt).getClaimAsString("email");
        doReturn(oversizedTitle).when(jwt).getClaimAsString("title");
        when(authentication.getAuthorities()).thenReturn(Set.of());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getTitle()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_oversizedEmail_withExistingUser_usesExistingEmail() throws Exception {
        String oversizedEmail = "a".repeat(250) + "@example.com";
        User existingUser = new User();
        existingUser.setId(USER_ID);
        existingUser.setName(FULL_NAME);
        existingUser.setEmail(EMAIL);
        existingUser.setUsername(USERNAME);
        existingUser.setPicture(PICTURE);
        existingUser.setRoles(Set.of());

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(USER_ID.toString());
        doReturn(USERNAME).when(jwt).getClaimAsString("preferred_username");
        doReturn(FULL_NAME).when(jwt).getClaimAsString("name");
        doReturn(oversizedEmail).when(jwt).getClaimAsString("email");
        doReturn(PICTURE).when(jwt).getClaimAsString("picture");
        when(authentication.getAuthorities()).thenReturn(Set.of());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        filter.doFilterInternal(request, response, filterChain);

        verify(userRepository, never()).save(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_oversizedEmail_withNoExistingUser_returns400() throws Exception {
        String oversizedEmail = "a".repeat(250) + "@example.com";
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(USER_ID.toString());
        doReturn(USERNAME).when(jwt).getClaimAsString("preferred_username");
        doReturn(FULL_NAME).when(jwt).getClaimAsString("name");
        doReturn(oversizedEmail).when(jwt).getClaimAsString("email");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        verify(userRepository, never()).save(any());
        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to provision user: email is required");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_nullAuthentication_skipsProvisioningAndContinuesChain() throws Exception {
        when(securityContext.getAuthentication()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(userRepository, never()).existsById(any());
        verify(userRepository, never()).save(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_notAuthenticated_skipsProvisioningAndContinuesChain() throws Exception {
        when(authentication.isAuthenticated()).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(userRepository, never()).existsById(any());
        verify(userRepository, never()).save(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_principalNotJwt_skipsProvisioningAndContinuesChain() throws Exception {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("not-a-jwt");

        filter.doFilterInternal(request, response, filterChain);

        verify(userRepository, never()).existsById(any());
        verify(userRepository, never()).save(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_existingUser_updatedData_savesUser() throws Exception {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(USER_ID.toString());
        doReturn(USERNAME).when(jwt).getClaimAsString("preferred_username");
        doReturn(FULL_NAME).when(jwt).getClaimAsString("name");
        doReturn(EMAIL).when(jwt).getClaimAsString("email");
        doReturn(PICTURE).when(jwt).getClaimAsString("picture");
        when(authentication.getAuthorities()).thenReturn(Set.of());

        User existingUser = new User();
        existingUser.setId(USER_ID);
        existingUser.setName("oldName");
        existingUser.setEmail("old@email.com");
        existingUser.setPicture("https://example.com/old-avatar.png");
        existingUser.setRoles(Set.of());

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        filter.doFilterInternal(request, response, filterChain);

        verify(userRepository).save(existingUser);
    }

    @Test
    void doFilterInternal_existingUser_emailNowConflictsWithAlreadyDeletedUser_scrubsDeletedEmail() throws Exception {
        User existingUser = new User();
        existingUser.setId(USER_ID);
        existingUser.setName(FULL_NAME);
        existingUser.setEmail("old@email.com");
        existingUser.setUsername(USERNAME);
        existingUser.setPicture(PICTURE);
        existingUser.setRoles(Set.of());

        UUID deletedUserId = UUID.randomUUID();
        User deletedUser = new User();
        deletedUser.setId(deletedUserId);
        deletedUser.setName("Old Name");
        deletedUser.setEmail(EMAIL);
        deletedUser.setUsername("someone");
        deletedUser.setDeletedAt(java.time.LocalDateTime.now().minusDays(1));

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(USER_ID.toString());
        doReturn(USERNAME).when(jwt).getClaimAsString("preferred_username");
        doReturn(FULL_NAME).when(jwt).getClaimAsString("name");
        doReturn(EMAIL).when(jwt).getClaimAsString("email");
        when(authentication.getAuthorities()).thenReturn(Set.of());

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.findAllByEmailIgnoreCaseAndDeletedAtIsNotNull(EMAIL)).thenReturn(java.util.List.of(deletedUser));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(deletedUser.getEmail()).contains("@invalid.local");
        verify(userRepository).save(existingUser);
        verify(userRepository).save(deletedUser);
        verify(filterChain).doFilter(request, response);
    }
}
