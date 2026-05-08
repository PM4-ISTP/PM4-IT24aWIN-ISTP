package com.pm4.istp.course.repositories;

import com.pm4.istp.course.db.entities.ChallengeCompletion;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChallengeCompletionRepository extends JpaRepository<ChallengeCompletion, UUID> {

  boolean existsByUserIdAndChallengeId(UUID userId, UUID challengeId);

  @Query(
      """
      select c.challenge.id
      from ChallengeCompletion c
      where c.user.id = :userId and c.challenge.id in :challengeIds
      """)
  List<UUID> findSolvedChallengeIds(
      @Param("userId") UUID userId, @Param("challengeIds") Collection<UUID> challengeIds);

  @Query(
      """
      select c.user.id, c.challenge.id
      from ChallengeCompletion c
      where c.user.id in :userIds and c.challenge.id in :challengeIds
      """)
  List<Object[]> findSolvedChallengePairs(
      @Param("userIds") Collection<UUID> userIds,
      @Param("challengeIds") Collection<UUID> challengeIds);

  @Query(
      """
      select c.challenge.id
      from ChallengeCompletion c
      where c.user.id = :userId and c.challenge.lab.id in :challengeIds
      """)
  List<UUID> findSolvedChallengeIdsForChallenges(
      @Param("userId") UUID userId, @Param("challengeIds") Collection<UUID> challengeIds);

  @Query(
      """
      select c.user.id, c.challenge.lab.id, count(c), max(c.solvedAt)
      from ChallengeCompletion c
      where c.user.id in :userIds and c.challenge.lab.id in :challengeIds
      group by c.user.id, c.challenge.lab.id
      """)
  List<Object[]> aggregateSolvedCountsForUsersAndLabs(
      @Param("userIds") Collection<UUID> userIds,
      @Param("challengeIds") Collection<UUID> challengeIds);

  @Query(
      """
      select count(distinct sc.challenge.lab.id)
      from ChallengeCompletion sc
      where sc.user.id = :userId
      and (
        select count(st.id) from Challenge st where st.lab.id = sc.challenge.lab.id
      ) = (
        select count(sc2.id) from ChallengeCompletion sc2
        where sc2.user.id = :userId and sc2.challenge.lab.id = sc.challenge.lab.id
      )
      """)
  long countCompletedChallenges(@Param("userId") UUID userId);
}
