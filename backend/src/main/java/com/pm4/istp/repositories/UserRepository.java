package com.pm4.istp.repositories;

import com.pm4.istp.domain.entites.User;
import com.pm4.istp.domain.entites.UserRoleEnum;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
  Page<User> findUserByRolesContainingAndIdNot(UserRoleEnum role, UUID userId, Pageable pageable);

  Page<User> findByRolesContainingAndNameContainingIgnoreCaseAndIdNot(
      UserRoleEnum role, String name, UUID userId, Pageable pageable);
}
