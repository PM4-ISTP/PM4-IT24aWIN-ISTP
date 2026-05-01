package com.pm4.istp.course.repositories;

import com.pm4.istp.course.db.entities.SubTaskCompletion;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubTaskCompletionRepository extends JpaRepository<SubTaskCompletion, UUID> {

  boolean existsByUserIdAndSubTaskId(UUID userId, UUID subTaskId);

  @Query(
      """
      select c.subTask.id
      from SubTaskCompletion c
      where c.user.id = :userId and c.subTask.id in :subTaskIds
      """)
  List<UUID> findSolvedSubTaskIds(
      @Param("userId") UUID userId, @Param("subTaskIds") Collection<UUID> subTaskIds);

  @Query(
      """
      select c.subTask.id
      from SubTaskCompletion c
      where c.user.id = :userId and c.subTask.challenge.id in :challengeIds
      """)
  List<UUID> findSolvedSubTaskIdsForChallenges(
      @Param("userId") UUID userId, @Param("challengeIds") Collection<UUID> challengeIds);

  @Query(
      """
      select c.user.id, c.subTask.challenge.id, count(c), max(c.solvedAt)
      from SubTaskCompletion c
      where c.user.id in :userIds and c.subTask.challenge.id in :challengeIds
      group by c.user.id, c.subTask.challenge.id
      """)
  List<Object[]> aggregateSolvedCountsForUsersAndChallenges(
      @Param("userIds") Collection<UUID> userIds,
      @Param("challengeIds") Collection<UUID> challengeIds);
}
