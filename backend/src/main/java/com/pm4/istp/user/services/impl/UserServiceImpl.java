package com.pm4.istp.user.services.impl;

import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.db.entities.UserRoleEnum;
import com.pm4.istp.user.exceptions.UserNotFoundException;
import com.pm4.istp.user.repositories.UserRepository;
import com.pm4.istp.user.services.UserService;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private static final Set<UserRoleEnum> COURSE_COLLABORATOR_ROLES =
      Set.of(UserRoleEnum.ROLE_ADMINISTRATOR, UserRoleEnum.ROLE_INSTRUCTOR);
  private static final String USER_NOT_FOUND_MSG = "User with ID '%s' not found";

  private final UserRepository userRepository;

  @Override
  public Page<User> listCollaboratorUsers(UUID userId, Pageable pageable) {
    return userRepository.findDistinctByAnyRoleAndIdNot(
        COURSE_COLLABORATOR_ROLES, userId, pageable);
  }

  @Override
  public Page<User> searchCollaboratorUsersByName(UUID userId, String name, Pageable pageable) {
    return userRepository.findDistinctByAnyRoleAndNameContainingIgnoreCaseAndIdNot(
        COURSE_COLLABORATOR_ROLES, name, userId, pageable);
  }

  @Override
  public Page<User> searchCollaboratorUsersByQuery(UUID userId, String query, Pageable pageable) {
    return userRepository.findDistinctByAnyRoleAndNameOrUsernameOrEmailContainingIgnoreCaseAndIdNot(
        COURSE_COLLABORATOR_ROLES, query, userId, pageable);
  }

  @Override
  @Transactional
  public void softDeleteUser(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId)));

    if (user.getDeletedAt() == null) {
      user.setDeletedAt(LocalDateTime.now());
      userRepository.save(user);
    }
  }
}
