package com.pm4.istp.service.impl;

import com.pm4.istp.domain.entites.User;
import com.pm4.istp.domain.entites.UserRoleEnum;
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
