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

  @Query(
      """
      select c.user.id, c.subTask.challenge.id, coalesce(sum(c.subTask.points), 0), max(c.solvedAt)
      from SubTaskCompletion c
      where c.user.id in :userIds and c.subTask.challenge.id in :challengeIds
      group by c.user.id, c.subTask.challenge.id
      """)
  List<Object[]> aggregateSolvedPointsForUsersAndChallenges(
      @Param("userIds") Collection<UUID> userIds,
      @Param("challengeIds") Collection<UUID> challengeIds);

  @Query(
      """
      select c.user.id, c.subTask.challenge.id, coalesce(sum(c.subTask.points), 0)
      from SubTaskCompletion c
      where c.user.id in :userIds and c.subTask.challenge.id in :challengeIds
      group by c.user.id, c.subTask.challenge.id
      """)
  List<Object[]> aggregatePointsForUsersAndChallenges(
      @Param("userIds") Collection<UUID> userIds,
      @Param("challengeIds") Collection<UUID> challengeIds);

  @Query(
      """
      select stc.user.id, stc.subTask.challenge.id, count(stc)
      from SubTaskCompletion stc
      join CourseChallenge cc on cc.challenge.id = stc.subTask.challenge.id and cc.course.id = :courseId
      where stc.user.id in :userIds
        and stc.subTask.challenge.id in :challengeIds
        and cc.dueAt is not null
        and stc.solvedAt <= cc.dueAt
      group by stc.user.id, stc.subTask.challenge.id
      """)
  List<Object[]> aggregateSolvedCountsBeforeDeadline(
      @Param("courseId") UUID courseId,
      @Param("userIds") Collection<UUID> userIds,
      @Param("challengeIds") Collection<UUID> challengeIds);

  @Query(
      """
      select count(distinct sc.subTask.challenge.id)
      from SubTaskCompletion sc
      where sc.user.id = :userId
      and (
        select count(st.id) from SubTask st where st.challenge.id = sc.subTask.challenge.id
      ) = (
        select count(sc2.id) from SubTaskCompletion sc2
        where sc2.user.id = :userId and sc2.subTask.challenge.id = sc.subTask.challenge.id
      )
      """)
  long countCompletedChallenges(@Param("userId") UUID userId);
}
