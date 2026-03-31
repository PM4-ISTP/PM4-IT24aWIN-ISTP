package com.pm4.istp.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.domain.entites.User;
import com.pm4.istp.domain.entites.UserRoleEnum;
import com.pm4.istp.exception.UserNotFoundException;
import com.pm4.istp.repositories.UserRepository;
import java.util.List;
import java.util.Optional;
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
  void getUserById_returnsUserWhenPresent() {
    UUID userId = UUID.randomUUID();
    User expected = new User();
    expected.setId(userId);

    when(userRepository.findById(userId)).thenReturn(Optional.of(expected));

    User result = userService.getUserById(userId);

    assertThat(result).isSameAs(expected);
    verify(userRepository).findById(userId);
  }

  @Test
  void getUserById_throwsWhenMissing() {
    UUID userId = UUID.randomUUID();

    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUserById(userId))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessage("User with id " + userId + " not found");

    verify(userRepository).findById(userId);
  }

  @Test
  void listInstructorUsers_delegatesToRepository() {
    UUID currentUserId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 20);
    Page<User> expected = new PageImpl<>(List.of(new User()));

    when(userRepository.findUserByRolesContainingAndIdNot(
            UserRoleEnum.ROLE_INSTRUCTOR, currentUserId, pageable))
        .thenReturn(expected);

    Page<User> result = userService.listInstructorUsers(currentUserId, pageable);

    assertThat(result).isSameAs(expected);
    verify(userRepository)
        .findUserByRolesContainingAndIdNot(UserRoleEnum.ROLE_INSTRUCTOR, currentUserId, pageable);
  }

  @Test
  void searchInstructorUsersByName_delegatesToRepository() {
    UUID currentUserId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(1, 10);
    String name = "ali";
    Page<User> expected = new PageImpl<>(List.of(new User(), new User()));

    when(userRepository.findByRolesContainingAndNameContainingIgnoreCaseAndIdNot(
            UserRoleEnum.ROLE_INSTRUCTOR, name, currentUserId, pageable))
        .thenReturn(expected);

    Page<User> result = userService.searchInstructorUsersByName(currentUserId, name, pageable);

    assertThat(result).isSameAs(expected);
    verify(userRepository)
        .findByRolesContainingAndNameContainingIgnoreCaseAndIdNot(
            UserRoleEnum.ROLE_INSTRUCTOR, name, currentUserId, pageable);
  }
}
