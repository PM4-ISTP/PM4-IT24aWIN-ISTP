package com.pm4.istp.repositories;

import com.pm4.istp.domain.entites.Course;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {
  Page<Course> findByCourseInstructorsInstructorId(UUID instructorId, Pageable pageable);
}
