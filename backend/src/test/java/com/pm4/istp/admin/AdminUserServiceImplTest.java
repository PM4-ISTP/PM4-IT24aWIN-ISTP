package com.pm4.istp.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pm4.istp.admin.dto.AdminCreateUserRequestDto;
import com.pm4.istp.admin.dto.AdminCreateUserResponseDto;
import com.pm4.istp.admin.dto.AdminProvisionUserResponseDto;
import com.pm4.istp.admin.services.impl.AdminUserServiceImpl;
import com.pm4.istp.shared.keycloak.KeycloakAdminApiException;
import com.pm4.istp.shared.keycloak.KeycloakAdminClient;
import com.pm4.istp.shared.keycloak.KeycloakRoleRepresentation;
import com.pm4.istp.shared.keycloak.KeycloakUserRepresentation;
import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.repositories.UserRepository;
import com.pm4.istp.user.services.UserService;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

  @Mock private KeycloakAdminClient keycloakAdminClient;
  @Mock private UserRepository userRepository;
  @Mock private UserService userService;

  @InjectMocks private AdminUserServiceImpl adminUserService;

  @Test
  void createUser_success_createsInKeycloakAndDb() {
    UUID createdId = UUID.randomUUID();

    AdminCreateUserRequestDto request = new AdminCreateUserRequestDto();
    request.setEmail("user@example.com");
    request.setUsername("user1");
    request.setFirstName("Alice");
    request.setLastName("Example");
    request.setTitle("Student");
    request.setPictureUrl("https://example.com/pic.png");

    when(userRepository.findAllByEmailIgnoreCaseAndDeletedAtIsNull("user@example.com"))
        .thenReturn(List.of());
    when(userRepository.findAllByUsernameIgnoreCaseAndDeletedAtIsNull("user1")).thenReturn(List.of());

    when(keycloakAdminClient.createUser(any(KeycloakUserRepresentation.class))).thenReturn(createdId);
    KeycloakRoleRepresentation role = new KeycloakRoleRepresentation();
    role.setId("role-id");
    role.setName("ROLE_STUDENT");
    when(keycloakAdminClient.getRealmRoleByName("ROLE_STUDENT")).thenReturn(role);
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    AdminCreateUserResponseDto response = adminUserService.createUser(request);

    assertThat(response.getUserId()).isEqualTo(createdId);
    assertThat(response.getTemporaryPassword()).isNotBlank();

    ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
    verify(keycloakAdminClient).resetPassword(eq(createdId), passwordCaptor.capture(), eq(true));
    assertThat(passwordCaptor.getValue()).isEqualTo(response.getTemporaryPassword());

    verify(keycloakAdminClient).addRealmRoles(eq(createdId), eq(List.of(role)));

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    assertThat(userCaptor.getValue().getId()).isEqualTo(createdId);
    assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@example.com");
    assertThat(userCaptor.getValue().getUsername()).isEqualTo("user1");
    assertThat(userCaptor.getValue().getFirstName()).isEqualTo("Alice");
    assertThat(userCaptor.getValue().getLastName()).isEqualTo("Example");
    assertThat(userCaptor.getValue().getTitle()).isEqualTo("Student");
    assertThat(userCaptor.getValue().getPicture()).isEqualTo("https://example.com/pic.png");
  }

  @Test
  void createUser_keycloakCreateFails_doesNotTouchDb() {
    AdminCreateUserRequestDto request = new AdminCreateUserRequestDto();
    request.setEmail("user@example.com");
    request.setUsername("user1");
    request.setFirstName("Alice");
    request.setLastName("Example");

    when(userRepository.findAllByEmailIgnoreCaseAndDeletedAtIsNull("user@example.com"))
        .thenReturn(List.of());
    when(userRepository.findAllByUsernameIgnoreCaseAndDeletedAtIsNull("user1")).thenReturn(List.of());

    doThrow(new KeycloakAdminApiException("boom"))
        .when(keycloakAdminClient)
        .createUser(any(KeycloakUserRepresentation.class));

    assertThatThrownBy(() -> adminUserService.createUser(request))
        .isInstanceOf(KeycloakAdminApiException.class);

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void createUser_dbSaveFails_deletesKeycloakUser() {
    UUID createdId = UUID.randomUUID();

    AdminCreateUserRequestDto request = new AdminCreateUserRequestDto();
    request.setEmail("user@example.com");
    request.setUsername("user1");
    request.setFirstName("Alice");
    request.setLastName("Example");

    when(userRepository.findAllByEmailIgnoreCaseAndDeletedAtIsNull("user@example.com"))
        .thenReturn(List.of());
    when(userRepository.findAllByUsernameIgnoreCaseAndDeletedAtIsNull("user1")).thenReturn(List.of());

    when(keycloakAdminClient.createUser(any(KeycloakUserRepresentation.class))).thenReturn(createdId);
    when(keycloakAdminClient.getRealmRoleByName("ROLE_STUDENT")).thenReturn(null);
    when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("db down"));

    assertThatThrownBy(() -> adminUserService.createUser(request)).isInstanceOf(RuntimeException.class);

    verify(keycloakAdminClient).deleteUser(createdId);
  }

  @Test
  void provisionUser_existingRow_returnsCreatedFalse() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));

    AdminProvisionUserResponseDto response = adminUserService.provisionUser(userId);

    assertThat(response.getUserId()).isEqualTo(userId);
    assertThat(response.isCreated()).isFalse();
    verify(keycloakAdminClient, never()).getUser(any());
  }

  @Test
  void provisionUser_newRow_assignsRoleInKeycloakAndCreatesDbRow() {
    UUID userId = UUID.randomUUID();

    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    KeycloakUserRepresentation kcUser = new KeycloakUserRepresentation();
    kcUser.setId(userId.toString());
    kcUser.setEmail("user@example.com");
    kcUser.setUsername("user1");
    kcUser.setFirstName("Alice");
    kcUser.setLastName("Example");
    when(keycloakAdminClient.getUser(userId)).thenReturn(kcUser);

    KeycloakRoleRepresentation role = new KeycloakRoleRepresentation();
    role.setId("role-id");
    role.setName("ROLE_STUDENT");
    when(keycloakAdminClient.getRealmRoleByName("ROLE_STUDENT")).thenReturn(role);

    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    AdminProvisionUserResponseDto response = adminUserService.provisionUser(userId);

    assertThat(response.getUserId()).isEqualTo(userId);
    assertThat(response.isCreated()).isTrue();

    verify(keycloakAdminClient).addRealmRoles(eq(userId), eq(List.of(role)));
    verify(userRepository).save(any(User.class));
  }

  @Test
  void provisionUser_softDeleted_throwsConflict() {
    UUID userId = UUID.randomUUID();

    User dbUser = new User();
    dbUser.setId(userId);
    dbUser.setAnonymizedAt(LocalDateTime.now());
    when(userRepository.findById(userId)).thenReturn(Optional.of(dbUser));

    assertThatThrownBy(() -> adminUserService.provisionUser(userId))
        .isInstanceOf(com.pm4.istp.user.exceptions.UserSoftDeletedException.class);

    verifyNoInteractions(keycloakAdminClient);
  }

  @Test
  void provisionUser_disabled_throwsConflict() {
    UUID userId = UUID.randomUUID();

    User dbUser = new User();
    dbUser.setId(userId);
    dbUser.setDeletedAt(LocalDateTime.now());
    when(userRepository.findById(userId)).thenReturn(Optional.of(dbUser));

    assertThatThrownBy(() -> adminUserService.provisionUser(userId))
        .isInstanceOf(com.pm4.istp.user.exceptions.UserSoftDeletedException.class);

    verifyNoInteractions(keycloakAdminClient);
  }

  @Test
  void updateUserRoles_updatesKeycloakAndDb() {
    UUID userId = UUID.randomUUID();

    var req = new com.pm4.istp.admin.dto.AdminUpdateUserRoleRequestDto();
    req.setRoles(java.util.Set.of("ROLE_INSTRUCTOR"));

    // current roles in KC: student
    KeycloakRoleRepresentation current = new KeycloakRoleRepresentation();
    current.setId("role-student");
    current.setName("ROLE_STUDENT");
    when(keycloakAdminClient.listUserRealmRoles(userId)).thenReturn(List.of(current));

    KeycloakRoleRepresentation instructor = new KeycloakRoleRepresentation();
    instructor.setId("role-instructor");
    instructor.setName("ROLE_INSTRUCTOR");
    when(keycloakAdminClient.getRealmRoleByName("ROLE_INSTRUCTOR")).thenReturn(instructor);

    KeycloakRoleRepresentation student = new KeycloakRoleRepresentation();
    student.setId("role-student");
    student.setName("ROLE_STUDENT");
    when(keycloakAdminClient.getRealmRoleByName("ROLE_STUDENT")).thenReturn(student);

    User dbUser = new User();
    dbUser.setId(userId);
    dbUser.setEmail("user@example.com");
    dbUser.setName("User");
    dbUser.setRoles(java.util.Set.of(com.pm4.istp.user.db.entities.UserRoleEnum.ROLE_STUDENT));
    when(userRepository.findById(userId)).thenReturn(Optional.of(dbUser));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // for getUser() at end
    KeycloakUserRepresentation kcUser = new KeycloakUserRepresentation();
    kcUser.setId(userId.toString());
    kcUser.setEmail("user@example.com");
    when(keycloakAdminClient.getUser(userId)).thenReturn(kcUser);

    var detail = adminUserService.updateUserRole(userId, req);

    assertThat(detail.getId()).isEqualTo(userId);
    assertThat(detail.getRoles()).contains("ROLE_INSTRUCTOR");

    verify(keycloakAdminClient).removeRealmRoles(eq(userId), eq(List.of(student)));
    verify(keycloakAdminClient).addRealmRoles(eq(userId), eq(List.of(instructor)));
    assertThat(dbUser.getRoles()).contains(com.pm4.istp.user.db.entities.UserRoleEnum.ROLE_INSTRUCTOR);
  }

  @Test
  void restoreUser_success_enablesKeycloakAndRestoresDb() {
    UUID userId = UUID.randomUUID();

    KeycloakUserRepresentation kcUser = new KeycloakUserRepresentation();
    kcUser.setId(userId.toString());
    kcUser.setEnabled(false);
    when(keycloakAdminClient.getUser(userId)).thenReturn(kcUser);

    adminUserService.restoreUser(userId);

    ArgumentCaptor<KeycloakUserRepresentation> captor = ArgumentCaptor.forClass(KeycloakUserRepresentation.class);
    verify(keycloakAdminClient).updateUser(eq(userId), captor.capture());
    assertThat(captor.getValue().getEnabled()).isTrue();

    verify(userService).restoreUser(userId);
  }

  @Test
  void restoreUser_dbFails_rollsBackKeycloak() {
    UUID userId = UUID.randomUUID();

    KeycloakUserRepresentation before = new KeycloakUserRepresentation();
    before.setId(userId.toString());
    before.setEnabled(false);
    when(keycloakAdminClient.getUser(userId)).thenReturn(before);

    doThrow(new RuntimeException("db down")).when(userService).restoreUser(userId);

    assertThatThrownBy(() -> adminUserService.restoreUser(userId))
        .isInstanceOf(com.pm4.istp.user.exceptions.UserProfileSyncException.class);

    verify(keycloakAdminClient).updateUser(eq(userId), eq(before));
  }

  @Test
  void softDeleteUser_success_anonymizesIdentifiersAndDisables() {
    UUID userId = UUID.randomUUID();

    KeycloakUserRepresentation kcUser = new KeycloakUserRepresentation();
    kcUser.setId(userId.toString());
    kcUser.setEnabled(true);
    kcUser.setEmail("user@example.com");
    kcUser.setUsername("user1");
    when(keycloakAdminClient.getUser(userId)).thenReturn(kcUser);

    when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));

    adminUserService.softDeleteUser(userId);

    ArgumentCaptor<KeycloakUserRepresentation> captor = ArgumentCaptor.forClass(KeycloakUserRepresentation.class);
    verify(keycloakAdminClient).updateUser(eq(userId), captor.capture());
    KeycloakUserRepresentation after = captor.getValue();
    assertThat(after.getEnabled()).isFalse();
    assertThat(after.getEmail()).startsWith("deleted+");
    assertThat(after.getEmail()).endsWith("@invalid.local");
    assertThat(after.getUsername()).startsWith("deleted_");

    verify(userService).softDeleteAndAnonymizeUser(eq(userId), eq(after.getEmail()), eq(after.getUsername()));
  }

  @Test
  void softDeleteUser_alreadySoftDeleted_isIdempotent() {
    UUID userId = UUID.randomUUID();

    User dbUser = new User();
    dbUser.setId(userId);
    dbUser.setAnonymizedAt(LocalDateTime.now());
    when(userRepository.findById(userId)).thenReturn(Optional.of(dbUser));

    // should early return without talking to Keycloak or UserService
    adminUserService.softDeleteUser(userId);

    verifyNoInteractions(keycloakAdminClient);
    verifyNoInteractions(userService);
  }
}
