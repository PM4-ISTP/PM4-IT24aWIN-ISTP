package com.pm4.istp.badge.repositories;

import com.pm4.istp.badge.db.entities.UserCourseBadge;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCourseBadgeRepository extends JpaRepository<UserCourseBadge, UUID> {

  boolean existsByUserIdAndCourseId(UUID userId, UUID courseId);

  Optional<UserCourseBadge> findByUserIdAndCourseId(UUID userId, UUID courseId);

  @Query(
      """
      select b from UserCourseBadge b
      join fetch b.course
      where b.user.id = :userId
      order by b.earnedAt desc
      """)
  List<UserCourseBadge> findByUserIdFetchCourse(@Param("userId") UUID userId);
}
