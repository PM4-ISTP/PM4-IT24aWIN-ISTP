package com.pm4.istp.course.repositories;

import com.pm4.istp.course.db.entities.SubTask;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubTaskRepository extends JpaRepository<SubTask, UUID> {
  List<SubTask> findByChallengeIdOrderByOrderIndexAsc(UUID challengeId);
}
