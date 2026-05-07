package com.pm4.istp.course.repositories;

import com.pm4.istp.course.db.entities.StudentFlagSubmission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentFlagSubmissionRepository extends JpaRepository<StudentFlagSubmission, UUID> {

  Optional<StudentFlagSubmission> findByUserIdAndSubTaskId(UUID userId, UUID subTaskId);

  @Query(
      """
      select s
      from StudentFlagSubmission s
      where s.user.id = :userId
        and s.subTask.challenge.id = :challengeId
      """)
  List<StudentFlagSubmission> findByUserIdAndChallengeId(
      @Param("userId") UUID userId, @Param("challengeId") UUID challengeId);
}

