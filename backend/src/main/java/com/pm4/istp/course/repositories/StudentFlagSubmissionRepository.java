package com.pm4.istp.course.repositories;

import com.pm4.istp.course.db.entities.StudentFlagSubmission;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentFlagSubmissionRepository extends JpaRepository<StudentFlagSubmission, UUID> {
  Optional<StudentFlagSubmission> findByUserIdAndChallengeId(UUID userId, UUID challengeId);

  @Query(
      """
      select s.user.id, s.challenge.id, s.submittedFlag, s.correct, s.submittedAt
      from StudentFlagSubmission s
      where s.user.id = :userId and s.challenge.id in :challengeIds
      """)
  List<Object[]> findForUserAndChallenges(
      @Param("userId") UUID userId, @Param("challengeIds") Collection<UUID> challengeIds);
}

