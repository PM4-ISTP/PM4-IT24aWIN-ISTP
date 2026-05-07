package com.pm4.istp.course.repositories;

import com.pm4.istp.course.db.entities.StudentOptionSubmission;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentOptionSubmissionRepository
    extends JpaRepository<StudentOptionSubmission, UUID> {

  boolean existsByUserIdAndChallengeId(UUID userId, UUID challengeId);

  Optional<StudentOptionSubmission> findByUserIdAndChallengeId(UUID userId, UUID challengeId);
}
