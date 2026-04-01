package com.pm4.istp.filters;

import com.pm4.istp.domain.entites.User;
import com.pm4.istp.domain.entites.UserRoleEnum;
import com.pm4.istp.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class UserProvisioningFilterTest {

    @Mock private UserRepository userRepository;
    @Mock private FilterChain filterChain;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;
    @Mock private Jwt jwt;

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
        when(jwt.getClaimAsString("preferred_username")).thenReturn(USERNAME);
        when(jwt.getClaimAsString("name")).thenReturn(FULL_NAME);
        when(jwt.getClaimAsString("email")).thenReturn(EMAIL);
        when(jwt.getClaimAsString("picture")).thenReturn(PICTURE);
        when(authentication.getAuthorities()).thenReturn(Set.of());

        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getId()).isEqualTo(USER_ID);
        assertThat(savedUser.getName()).isEqualTo(FULL_NAME);
        assertThat(savedUser.getEmail()).isEqualTo(EMAIL);
        assertThat(savedUser.getPicture()).isEqualTo(PICTURE);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_newUser_withoutNameClaim_usesGivenAndFamilyName() throws Exception {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(USER_ID.toString());
        when(jwt.getClaimAsString("name")).thenReturn(null);
        when(jwt.getClaimAsString("given_name")).thenReturn("Test");
        when(jwt.getClaimAsString("family_name")).thenReturn("User");
        when(jwt.getClaimAsString("preferred_username")).thenReturn(USERNAME);
        when(jwt.getClaimAsString("email")).thenReturn(EMAIL);
        when(jwt.getClaimAsString("picture")).thenReturn(PICTURE);
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
        doReturn(null).when(jwt).getClaimAsString(anyString());
        when(jwt.getClaimAsString("preferred_username")).thenReturn(USERNAME);
        when(jwt.getClaimAsString("email")).thenReturn(EMAIL);
        when(jwt.getClaimAsString("picture")).thenReturn(PICTURE);
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
        when(jwt.getClaimAsString("name")).thenReturn(FULL_NAME);
        when(jwt.getClaimAsString("email")).thenReturn(EMAIL);
        when(jwt.getClaimAsString("picture")).thenReturn(PICTURE);
        when(authentication.getAuthorities()).thenReturn(
                Set.of(() -> UserRoleEnum.ROLE_INSTRUCTOR.name()));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        filter.doFilterInternal(request, response, filterChain);

        verify(userRepository, never()).save(any());
        verify(filterChain).doFilter(request, response);
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
        when(jwt.getClaimAsString("preferred_username")).thenReturn(USERNAME);
        when(jwt.getClaimAsString("name")).thenReturn(FULL_NAME);
        when(jwt.getClaimAsString("email")).thenReturn(EMAIL);
        when(jwt.getClaimAsString("picture")).thenReturn(PICTURE);
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
}
