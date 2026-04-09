package com.pm4.istp.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.domain.entites.User;
import com.pm4.istp.domain.entites.UserRoleEnum;
import com.pm4.istp.repositories.UserRepository;
import java.util.List;
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

    when(userRepository.findDistinctByAnyRoleAndNameOrUsernameContainingIgnoreCaseAndIdNot(
            collaboratorRoles, query, currentUserId, pageable))
        .thenReturn(expected);

    Page<User> result = userService.searchCollaboratorUsersByQuery(currentUserId, query, pageable);

    assertThat(result).isSameAs(expected);
    verify(userRepository)
        .findDistinctByAnyRoleAndNameOrUsernameContainingIgnoreCaseAndIdNot(
            collaboratorRoles, query, currentUserId, pageable);
  }
}
