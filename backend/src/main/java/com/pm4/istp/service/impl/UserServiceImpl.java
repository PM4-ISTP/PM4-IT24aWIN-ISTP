package com.pm4.istp.service.impl;

import com.pm4.istp.domain.entites.User;
import com.pm4.istp.domain.entites.UserRoleEnum;
import com.pm4.istp.repositories.UserRepository;
import com.pm4.istp.service.UserService;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private static final Set<UserRoleEnum> COURSE_COLLABORATOR_ROLES =
      Set.of(UserRoleEnum.ROLE_ADMINISTRATOR, UserRoleEnum.ROLE_INSTRUCTOR);

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
}
