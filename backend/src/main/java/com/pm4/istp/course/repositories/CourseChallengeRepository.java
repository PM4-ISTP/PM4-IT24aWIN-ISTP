package com.pm4.istp.course.repositories;

import com.pm4.istp.course.db.entities.CourseChallenge;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

  @Modifying
  @Query("delete from CourseChallenge cc where cc.challenge.id = :challengeId")
  int deleteByChallengeId(@Param("challengeId") UUID challengeId);

  @Modifying
  @Query(
      """
      delete from CourseChallenge cc
      where cc.challenge.id = :challengeId
      and cc.course.id not in (
        select ci.course.id from CourseInstructor ci where ci.instructor.id = :creatorId
      )
      """)
  int deleteByChallengeIdWhereCreatorNotInstructor(
      @Param("challengeId") UUID challengeId, @Param("creatorId") UUID creatorId);

  @Query("select count(cc) from CourseChallenge cc where cc.challenge.id = :challengeId")
  long countByChallengeId(@Param("challengeId") UUID challengeId);

  @Query(
      """
      select count(cc) from CourseChallenge cc
      where cc.challenge.id = :challengeId
      and cc.course.id not in (
        select ci.course.id from CourseInstructor ci where ci.instructor.id = :creatorId
      )
      """)
  long countByChallengeIdWhereCreatorNotInstructor(
      @Param("challengeId") UUID challengeId, @Param("creatorId") UUID creatorId);
}
