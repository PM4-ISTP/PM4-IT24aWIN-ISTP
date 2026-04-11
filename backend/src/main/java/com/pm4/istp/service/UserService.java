package com.pm4.istp.service;

import com.pm4.istp.domain.entites.User;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
  Page<User> listCollaboratorUsers(UUID userId, Pageable pageable);

  Page<User> searchCollaboratorUsersByName(UUID userId, String name, Pageable pageable);

  Page<User> searchCollaboratorUsersByQuery(UUID userId, String query, Pageable pageable);

  List<User> getTeamMembers(Collection<String> emails);
}
