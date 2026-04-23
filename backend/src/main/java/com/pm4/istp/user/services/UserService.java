package com.pm4.istp.user.services;

import com.pm4.istp.user.db.entities.User;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
  Page<User> listCollaboratorUsers(UUID userId, Pageable pageable);

  Page<User> searchCollaboratorUsersByName(UUID userId, String name, Pageable pageable);

  Page<User> searchCollaboratorUsersByQuery(UUID userId, String query, Pageable pageable);

  void softDeleteUser(UUID userId);
}
