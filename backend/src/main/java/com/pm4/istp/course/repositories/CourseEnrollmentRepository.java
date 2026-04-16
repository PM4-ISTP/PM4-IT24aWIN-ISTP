package com.pm4.istp.course.repositories;

import com.pm4.istp.course.db.entities.CourseEnrollment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, UUID> {
  boolean existsByCourseIdAndParticipantId(UUID courseId, UUID participantId);

  long countByCourseId(UUID courseId);

  @Query("SELECT e FROM CourseEnrollment e JOIN FETCH e.participant WHERE e.course.id = :courseId")
  List<CourseEnrollment> findByCourseIdFetchParticipant(@Param("courseId") UUID courseId);
}
