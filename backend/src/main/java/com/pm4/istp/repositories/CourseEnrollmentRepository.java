package com.pm4.istp.repositories;

import com.pm4.istp.domain.entites.CourseEnrollment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, UUID> {
  boolean existsByCourseIdAndParticipantId(UUID courseId, UUID participantId);

  long countByCourseId(UUID courseId);
}
