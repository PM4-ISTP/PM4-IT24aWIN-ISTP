package com.pm4.istp.course.repositories;

import com.pm4.istp.course.db.entities.CourseTopic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseTopicRepository extends JpaRepository<CourseTopic, String> {
  List<CourseTopic> findAllByActiveTrueOrderByTopicAsc();

  long countByActiveTrue();

  boolean existsByTopicAndActiveTrue(String topic);
}
