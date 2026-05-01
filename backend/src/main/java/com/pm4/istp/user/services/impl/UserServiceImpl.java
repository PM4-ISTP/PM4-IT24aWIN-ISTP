package com.pm4.istp.user.services.impl;

import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.db.entities.UserRoleEnum;
import com.pm4.istp.user.exceptions.UserNotFoundException;
import com.pm4.istp.user.exceptions.UserSoftDeletedException;
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

  @Override
  @Transactional
  public void restoreUser(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId)));

    if (user.getAnonymizedAt() != null) {
      throw new UserSoftDeletedException("User is soft-deleted and cannot be restored");
    }

    if (user.getDeletedAt() != null) {
      user.setDeletedAt(null);
      userRepository.save(user);
    }
  }

  @Override
  @Transactional
  public void softDeleteAndAnonymizeUser(
      UUID userId, String anonymizedEmail, String anonymizedUsername) {
    if (userId == null) {
      throw new IllegalArgumentException("userId is required");
    }
    if (anonymizedEmail == null || anonymizedEmail.isBlank()) {
      throw new IllegalArgumentException("anonymizedEmail is required");
    }
    if (anonymizedUsername == null || anonymizedUsername.isBlank()) {
      throw new IllegalArgumentException("anonymizedUsername is required");
    }

    LocalDateTime now = LocalDateTime.now();

    User user = userRepository.findById(userId).orElse(null);
    if (user == null) {
      user = new User();
      user.setId(userId);
      user.setName("Deleted user");
      user.setFirstName(null);
      user.setLastName(null);
      user.setTitle(null);
      user.setPicture(null);
      user.setRoles(new java.util.HashSet<>());
      user.setCreatedAt(now);
      user.setUpdatedAt(now);
    }

    user.setEmail(anonymizedEmail);
    user.setUsername(anonymizedUsername);
    if (user.getCreatedAt() == null) {
      user.setCreatedAt(now);
    }
    user.setUpdatedAt(now);
    if (user.getDeletedAt() == null) {
      user.setDeletedAt(now);
    }
    if (user.getAnonymizedAt() == null) {
      user.setAnonymizedAt(now);
    }
    userRepository.save(user);
  }
}
