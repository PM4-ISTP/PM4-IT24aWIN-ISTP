package com.pm4.istp.service.impl;

import com.pm4.istp.domain.entites.User;
import com.pm4.istp.domain.entites.UserRoleEnum;
import com.pm4.istp.exception.UserNotFoundException;
import com.pm4.istp.repositories.UserRepository;
import com.pm4.istp.service.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;

  @Override
  public User getUserById(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User with id " + userId + " not found"));
  }

  @Override
  public Page<User> listInstructorUsers(UUID userId, Pageable pageable) {
    return userRepository.findUserByRolesContainingAndIdNot(
        UserRoleEnum.ROLE_INSTRUCTOR, userId, pageable);
  }

  @Override
  public Page<User> searchInstructorUsersByName(UUID userId, String name, Pageable pageable) {
    return userRepository.findByRolesContainingAndNameContainingIgnoreCaseAndIdNot(
        UserRoleEnum.ROLE_INSTRUCTOR, name, userId, pageable);
  }
}
