package com.pm4.istp.user.repositories;

import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.db.entities.UserRoleEnum;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
  @Query(
      """
      select distinct u
      from User u
      join u.roles r
      where r in :roles
        and u.id <> :userId
      """)
  Page<User> findDistinctByAnyRoleAndIdNot(
      @Param("roles") Set<UserRoleEnum> roles, @Param("userId") UUID userId, Pageable pageable);

  @Query(
      """
      select distinct u
      from User u
      join u.roles r
      where r in :roles
        and lower(u.name) like lower(concat('%', :name, '%'))
        and u.id <> :userId
      """)
  Page<User> findDistinctByAnyRoleAndNameContainingIgnoreCaseAndIdNot(
      @Param("roles") Set<UserRoleEnum> roles,
      @Param("name") String name,
      @Param("userId") UUID userId,
      Pageable pageable);

  @Query(
      """
      select distinct u
      from User u
      join u.roles r
      where r in :roles
        and (lower(u.name) like lower(concat('%', :query, '%'))
          or lower(u.username) like lower(concat('%', :query, '%'))
          or lower(u.email) like lower(concat('%', :query, '%')))
        and u.id <> :userId
      """)
  Page<User> findDistinctByAnyRoleAndNameOrUsernameOrEmailContainingIgnoreCaseAndIdNot(
      @Param("roles") Set<UserRoleEnum> roles,
      @Param("query") String query,
      @Param("userId") UUID userId,
      Pageable pageable);
}
