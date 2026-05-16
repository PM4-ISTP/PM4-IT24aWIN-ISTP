package com.pm4.istp.course.repositories;

import com.pm4.istp.course.db.entities.CourseChallengeScoreOverride;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseChallengeScoreOverrideRepository
    extends JpaRepository<CourseChallengeScoreOverride, UUID> {

  boolean existsByCourseId(UUID courseId);

  Optional<CourseChallengeScoreOverride> findByCourseIdAndParticipantIdAndChallengeId(
      UUID courseId, UUID participantId, UUID challengeId);

  @Query(
      """
      select o.participant.id, o.challenge.id, o.points
      from CourseChallengeScoreOverride o
      where o.course.id = :courseId
        and o.participant.id in :participantIds
        and o.challenge.id in :challengeIds
      """)
  List<Object[]> findPointsForCourseParticipantsAndChallenges(
      @Param("courseId") UUID courseId,
      @Param("participantIds") Collection<UUID> participantIds,
      @Param("challengeIds") Collection<UUID> challengeIds);

  @Query(
      """
      select count(o) > 0
      from CourseChallengeScoreOverride o
      where o.challenge.lab.id = :labId
      """)
  boolean existsByLabId(@Param("labId") UUID labId);
}
