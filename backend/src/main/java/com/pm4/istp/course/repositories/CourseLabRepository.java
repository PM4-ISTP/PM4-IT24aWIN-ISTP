package com.pm4.istp.course.repositories;

import com.pm4.istp.course.db.entities.CourseLab;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseLabRepository extends JpaRepository<CourseLab, UUID> {

  List<CourseLab> findByCourseIdOrderByOrderIndexAsc(UUID courseId);

  @Query(
      """
      select count(cc) > 0
      from CourseLab cc
      join cc.course.courseInstructors ci
      where cc.lab.id = :labId and ci.instructor.id = :userId
      """)
  boolean existsByChallengeIdAndCourseInstructorId(
      @Param("labId") UUID labId, @Param("userId") UUID userId);

  @Query(
      """
      select count(cc) > 0
      from CourseLab cc
      where cc.lab.id = :labId
      and exists (
        select 1 from CourseEnrollment e where e.course = cc.course and e.participant.id = :userId
      )
      """)
  boolean existsByChallengeIdAndEnrolledUserId(
      @Param("labId") UUID labId, @Param("userId") UUID userId);

  @Modifying
  @Query("delete from CourseLab cc where cc.lab.id = :labId")
  int deleteByChallengeId(@Param("labId") UUID labId);

  @Modifying
  @Query(
      """
      delete from CourseLab cc
      where cc.lab.id = :labId
      and cc.course.id not in (
        select ci.course.id from CourseInstructor ci where ci.instructor.id = :creatorId
      )
      """)
  int deleteByChallengeIdWhereCreatorNotInstructor(
      @Param("labId") UUID labId, @Param("creatorId") UUID creatorId);

  @Query("select count(cc) from CourseLab cc where cc.lab.id = :labId")
  long countByChallengeId(@Param("labId") UUID labId);

  @Query(
      """
      select count(cc) from CourseLab cc
      where cc.lab.id = :labId
      and cc.course.id not in (
        select ci.course.id from CourseInstructor ci where ci.instructor.id = :creatorId
      )
      """)
  long countByChallengeIdWhereCreatorNotInstructor(
      @Param("labId") UUID labId, @Param("creatorId") UUID creatorId);

  @Query(
      """
      select cc.course.id, cc.course.title, cc.lab.id, cc.lab.title, cc.dueAt
      from CourseLab cc
      where cc.dueAt is not null
      and exists (
        select 1 from CourseEnrollment e where e.course = cc.course and e.participant.id = :userId
      )
      order by cc.dueAt asc
      """)
  List<Object[]> findDeadlinesForUser(@Param("userId") UUID userId);
}
