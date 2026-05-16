package com.pm4.istp.course.repositories;

import com.pm4.istp.course.db.entities.Challenge;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, UUID> {
  List<Challenge> findByLabIdOrderByOrderIndexAsc(UUID labId);

  long countByLabId(UUID labId);

  @Query(
      """
      select c
      from Challenge c
      where c.lab.id in :labIds
      order by c.lab.id, c.orderIndex asc
      """)
  List<Challenge> findByLabIdsOrderByLabIdAndOrderIndexAsc(
      @Param("labIds") Collection<UUID> labIds);

  @Query("select s.id, s.flag from Challenge s where s.id in :ids")
  List<Object[]> findFlagsByIds(@Param("ids") Collection<UUID> ids);

  @Query(
      """
      select s.lab.id, count(s)
      from Challenge s
      where s.lab.id in :challengeIds
      group by s.lab.id
      """)
  List<Object[]> countByLabIds(@Param("challengeIds") Collection<UUID> challengeIds);
}
