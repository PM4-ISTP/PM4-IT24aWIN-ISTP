package com.pm4.istp.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm4.istp.admin.controllers.AdminUserController;
import com.pm4.istp.admin.dto.AdminCreateUserRequestDto;
import com.pm4.istp.admin.dto.AdminCreateUserResponseDto;
import com.pm4.istp.admin.dto.AdminProvisionUserResponseDto;
import com.pm4.istp.admin.dto.AdminSetUserPasswordRequestDto;
import com.pm4.istp.admin.dto.AdminUpdateUserRoleRequestDto;
import com.pm4.istp.admin.dto.AdminUserDetailDto;
import com.pm4.istp.admin.dto.AdminUserDirectoryItemDto;
import com.pm4.istp.admin.dto.AdminUserListItemDto;
import com.pm4.istp.admin.services.AdminUserService;
import com.pm4.istp.shared.keycloak.KeycloakUserRepresentation;
import com.pm4.istp.shared.keycloak.KeycloakUserSessionRepresentation;
import com.pm4.istp.shared.util.GlobalExceptionHandler;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock private AdminUserService adminUserService;

  @InjectMocks private AdminUserController controller;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setValidator(validator)
            .build();
  }

  @Test
  void listDirectoryAndUsersAndDetail_returnExpectedPayloads() throws Exception {
    UUID userId = UUID.randomUUID();
    when(adminUserService.listUserDirectory("ali", 0, 10))
        .thenReturn(
            List.of(
                new AdminUserDirectoryItemDto(
                    userId, "a@example.com", "alice", "Alice", "A", true, true, null, null, Set.of("ROLE_STUDENT"))));
    when(adminUserService.listUsers(eq(null), any()))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    new AdminUserListItemDto(
                        userId, "Alice A", "a@example.com", "alice", "Alice", "A", "Student", "pic", Set.of("ROLE_STUDENT"), null, null)),
                PageRequest.of(0, 10),
                1));
    when(adminUserService.getUser(userId))
        .thenReturn(
            new AdminUserDetailDto(
                userId,
                "Alice A",
                "a@example.com",
                "alice",
                "Alice",
                "A",
                "Student",
                "pic",
                Set.of("ROLE_STUDENT"),
                null,
                null,
                true,
                new KeycloakUserRepresentation()));

    mockMvc
        .perform(get("/api/admin/users/directory").param("q", "ali").param("first", "0").param("max", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].username").value("alice"));
    mockMvc
        .perform(get("/api/admin/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].email").value("a@example.com"));
    mockMvc
        .perform(get("/api/admin/users/" + userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()));
  }

  @Test
  void writeEndpoints_delegateAndReturnExpectedStatuses() throws Exception {
    UUID userId = UUID.randomUUID();
    AdminUpdateUserRoleRequestDto roles = new AdminUpdateUserRoleRequestDto();
    roles.setRoles(Set.of("ROLE_INSTRUCTOR"));
    AdminCreateUserRequestDto create = new AdminCreateUserRequestDto();
    create.setEmail("user@example.com");
    create.setUsername("user1");
    create.setFirstName("User");
    create.setLastName("One");
    AdminSetUserPasswordRequestDto password = new AdminSetUserPasswordRequestDto();
    password.setPassword("welcome123");
    password.setTemporary(false);

    when(adminUserService.updateUserRole(eq(userId), any()))
        .thenReturn(
            new AdminUserDetailDto(
                userId, "User One", "user@example.com", "user1", "User", "One", null, null, Set.of("ROLE_INSTRUCTOR"), null, null, true, null));
    when(adminUserService.createUser(any()))
        .thenReturn(new AdminCreateUserResponseDto(userId, "temporary-secret"));
    when(adminUserService.provisionUser(userId)).thenReturn(new AdminProvisionUserResponseDto(userId, true));
    doNothing().when(adminUserService).disableUser(userId);
    doNothing().when(adminUserService).softDeleteUser(userId);
    doNothing().when(adminUserService).restoreUser(userId);
    doNothing().when(adminUserService).logoutUser(userId);
    doNothing().when(adminUserService).sendPasswordResetEmail(userId);
    doNothing().when(adminUserService).setUserPassword(eq(userId), any());

    mockMvc
        .perform(
            put("/api/admin/users/" + userId + "/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roles)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles[0]").value("ROLE_INSTRUCTOR"));
    mockMvc
        .perform(
            post("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(create)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.temporaryPassword").value("temporary-secret"));
    mockMvc.perform(post("/api/admin/users/" + userId + "/provision")).andExpect(status().isOk());
    mockMvc.perform(post("/api/admin/users/" + userId + "/disable")).andExpect(status().isNoContent());
    mockMvc.perform(post("/api/admin/users/" + userId + "/soft-delete")).andExpect(status().isNoContent());
    mockMvc.perform(post("/api/admin/users/" + userId + "/restore")).andExpect(status().isNoContent());
    mockMvc.perform(post("/api/admin/users/" + userId + "/logout")).andExpect(status().isNoContent());
    mockMvc
        .perform(post("/api/admin/users/" + userId + "/password-reset-email"))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(
            put("/api/admin/users/" + userId + "/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(password)))
        .andExpect(status().isNoContent());

    verify(adminUserService).setUserPassword(eq(userId), any());
  }

  @Test
  void sessionsEndpoint_returnsSessions() throws Exception {
    UUID userId = UUID.randomUUID();
    KeycloakUserSessionRepresentation session = new KeycloakUserSessionRepresentation();
    session.setId("s1");
    session.setUsername("alice");
    when(adminUserService.listUserSessions(userId)).thenReturn(List.of(session));

    mockMvc
        .perform(get("/api/admin/users/" + userId + "/sessions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value("s1"));
  }

  @Test
  void invalidCreateUser_returnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-email\",\"username\":\"ab\"}"))
        .andExpect(status().isBadRequest());
  }
}
