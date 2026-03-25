package com.pm4.istp.repositories;

import com.pm4.istp.domain.entites.CourseInstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CourseInstructorRepository extends JpaRepository<CourseInstructor, UUID> {
}
