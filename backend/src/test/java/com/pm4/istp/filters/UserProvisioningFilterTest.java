package com.pm4.istp.filters;

import com.pm4.istp.domain.entites.User;
import com.pm4.istp.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    private static final String EMAIL = "test@example.com";

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
        when(jwt.getClaimAsString("email")).thenReturn(EMAIL);
        when(authentication.getAuthorities()).thenReturn(Set.of());

        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getId()).isEqualTo(USER_ID);
        assertThat(savedUser.getName()).isEqualTo(USERNAME);
        assertThat(savedUser.getEmail()).isEqualTo(EMAIL);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_existingUser_doesNotSaveAndContinuesChain() throws Exception {
        User existingUser = new User();
        existingUser.setId(USER_ID);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(USER_ID.toString());
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
        when(jwt.getClaimAsString("email")).thenReturn(EMAIL);
        when(authentication.getAuthorities()).thenReturn(Set.of());

        User existingUser = new User();
        existingUser.setId(USER_ID);
        existingUser.setName("oldName");
        existingUser.setEmail("old@email.com");
        existingUser.setRoles(Set.of());

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        filter.doFilterInternal(request, response, filterChain);

        verify(userRepository).save(existingUser);
    }
}