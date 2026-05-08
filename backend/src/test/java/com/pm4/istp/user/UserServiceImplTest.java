package com.pm4.istp.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.db.entities.UserRoleEnum;
import com.pm4.istp.user.exceptions.UserNotFoundException;
import com.pm4.istp.user.exceptions.UserSoftDeletedException;
import com.pm4.istp.user.repositories.UserRepository;
import com.pm4.istp.user.services.impl.UserServiceImpl;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private UserServiceImpl userService;

  @Test
  void listCollaboratorUsers_delegatesToRepository() {
    UUID currentUserId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 20);
    Page<User> expected = new PageImpl<>(List.of(new User()));
    Set<UserRoleEnum> collaboratorRoles =
        Set.of(UserRoleEnum.ROLE_ADMINISTRATOR, UserRoleEnum.ROLE_INSTRUCTOR);

    when(userRepository.findDistinctByAnyRoleAndIdNot(
            collaboratorRoles, currentUserId, pageable))
        .thenReturn(expected);

    Page<User> result = userService.listCollaboratorUsers(currentUserId, pageable);

    assertThat(result).isSameAs(expected);
    verify(userRepository)
        .findDistinctByAnyRoleAndIdNot(collaboratorRoles, currentUserId, pageable);
  }

  @Test
  void searchCollaboratorUsersByName_delegatesToRepository() {
    UUID currentUserId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(1, 10);
    String name = "ali";
    Page<User> expected = new PageImpl<>(List.of(new User(), new User()));
    Set<UserRoleEnum> collaboratorRoles =
        Set.of(UserRoleEnum.ROLE_ADMINISTRATOR, UserRoleEnum.ROLE_INSTRUCTOR);

    when(userRepository.findDistinctByAnyRoleAndNameContainingIgnoreCaseAndIdNot(
            collaboratorRoles, name, currentUserId, pageable))
        .thenReturn(expected);

    Page<User> result = userService.searchCollaboratorUsersByName(currentUserId, name, pageable);

    assertThat(result).isSameAs(expected);
    verify(userRepository)
        .findDistinctByAnyRoleAndNameContainingIgnoreCaseAndIdNot(
            collaboratorRoles, name, currentUserId, pageable);
  }

  @Test
  void searchCollaboratorUsersByQuery_delegatesToRepository() {
    UUID currentUserId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 20);
    String query = "biedeli1";
    Page<User> expected = new PageImpl<>(List.of(new User()));
    Set<UserRoleEnum> collaboratorRoles =
        Set.of(UserRoleEnum.ROLE_ADMINISTRATOR, UserRoleEnum.ROLE_INSTRUCTOR);

    when(userRepository.findDistinctByAnyRoleAndNameOrUsernameOrEmailContainingIgnoreCaseAndIdNot(
            collaboratorRoles, query, currentUserId, pageable))
        .thenReturn(expected);

    Page<User> result = userService.searchCollaboratorUsersByQuery(currentUserId, query, pageable);

    assertThat(result).isSameAs(expected);
    verify(userRepository)
        .findDistinctByAnyRoleAndNameOrUsernameOrEmailContainingIgnoreCaseAndIdNot(
            collaboratorRoles, query, currentUserId, pageable);
  }

  @Test
  void softDeleteUser_marksActiveUserDeletedAndIsIdempotent() {
    UUID userId = UUID.randomUUID();
    User active = new User();
    active.setId(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(active));

    userService.softDeleteUser(userId);

    assertThat(active.getDeletedAt()).isNotNull();
    verify(userRepository).save(active);

    User alreadyDeleted = new User();
    alreadyDeleted.setId(userId);
    alreadyDeleted.setDeletedAt(LocalDateTime.now());
    when(userRepository.findById(userId)).thenReturn(Optional.of(alreadyDeleted));

    userService.softDeleteUser(userId);
  }

  @Test
  void softDeleteUser_missingUser_throws() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.softDeleteUser(userId))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining(userId.toString());
  }

  @Test
  void restoreUser_restoresDeletedUserAndRejectsAnonymizedUser() {
    UUID userId = UUID.randomUUID();
    User deleted = new User();
    deleted.setId(userId);
    deleted.setDeletedAt(LocalDateTime.now());
    when(userRepository.findById(userId)).thenReturn(Optional.of(deleted));

    userService.restoreUser(userId);

    assertThat(deleted.getDeletedAt()).isNull();
    verify(userRepository).save(deleted);

    User anonymized = new User();
    anonymized.setId(userId);
    anonymized.setAnonymizedAt(LocalDateTime.now());
    when(userRepository.findById(userId)).thenReturn(Optional.of(anonymized));

    assertThatThrownBy(() -> userService.restoreUser(userId))
        .isInstanceOf(UserSoftDeletedException.class)
        .hasMessageContaining("cannot be restored");
  }

  @Test
  void restoreUser_missingOrAlreadyActiveUser_doesNotSave() {
    UUID missingId = UUID.randomUUID();
    when(userRepository.findById(missingId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.restoreUser(missingId))
        .isInstanceOf(UserNotFoundException.class);

    UUID activeId = UUID.randomUUID();
    User active = new User();
    active.setId(activeId);
    when(userRepository.findById(activeId)).thenReturn(Optional.of(active));

    userService.restoreUser(activeId);

    verify(userRepository, never()).save(active);
  }

  @Test
  void softDeleteAndAnonymizeUser_validatesInputAndCreatesMissingUser() {
    UUID userId = UUID.randomUUID();

    assertThatThrownBy(() -> userService.softDeleteAndAnonymizeUser(null, "a@x.test", "anon"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("userId");
    assertThatThrownBy(() -> userService.softDeleteAndAnonymizeUser(userId, " ", "anon"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("anonymizedEmail");
    assertThatThrownBy(() -> userService.softDeleteAndAnonymizeUser(userId, "a@x.test", " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("anonymizedUsername");

    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    userService.softDeleteAndAnonymizeUser(userId, "deleted@example.test", "deleted_user");

    org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();
    assertThat(saved.getId()).isEqualTo(userId);
    assertThat(saved.getName()).isEqualTo("Deleted user");
    assertThat(saved.getEmail()).isEqualTo("deleted@example.test");
    assertThat(saved.getUsername()).isEqualTo("deleted_user");
    assertThat(saved.getDeletedAt()).isNotNull();
    assertThat(saved.getAnonymizedAt()).isNotNull();
  }

  @Test
  void softDeleteAndAnonymizeUser_updatesExistingUserWithoutOverwritingExistingTimestamps() {
    UUID userId = UUID.randomUUID();
    LocalDateTime existingCreatedAt = LocalDateTime.now().minusDays(1);
    LocalDateTime existingDeletedAt = LocalDateTime.now().minusHours(1);
    LocalDateTime existingAnonymizedAt = LocalDateTime.now().minusMinutes(30);
    User user = new User();
    user.setId(userId);
    user.setCreatedAt(existingCreatedAt);
    user.setDeletedAt(existingDeletedAt);
    user.setAnonymizedAt(existingAnonymizedAt);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    userService.softDeleteAndAnonymizeUser(userId, "deleted@example.test", "deleted_user");

    assertThat(user.getCreatedAt()).isEqualTo(existingCreatedAt);
    assertThat(user.getDeletedAt()).isEqualTo(existingDeletedAt);
    assertThat(user.getAnonymizedAt()).isEqualTo(existingAnonymizedAt);
    assertThat(user.getUpdatedAt()).isNotNull();
    verify(userRepository).save(user);
  }
}
