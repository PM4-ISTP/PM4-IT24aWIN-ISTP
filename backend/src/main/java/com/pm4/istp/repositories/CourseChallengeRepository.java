package com.pm4.istp.repositories;

import com.pm4.istp.domain.entites.CourseChallenge;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseChallengeRepository extends JpaRepository<CourseChallenge, UUID> {

  List<CourseChallenge> findByCourseIdOrderByOrderIndexAsc(UUID courseId);

  @Query(
      """
      select count(cc) > 0
      from CourseChallenge cc
      join cc.course.courseInstructors ci
      where cc.challenge.id = :challengeId and ci.instructor.id = :userId
      """)
  boolean existsByChallengeIdAndCourseInstructorId(
      @Param("challengeId") UUID challengeId, @Param("userId") UUID userId);
}
