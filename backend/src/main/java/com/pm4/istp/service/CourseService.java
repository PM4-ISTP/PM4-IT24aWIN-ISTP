package com.pm4.istp.service;

import com.pm4.istp.domain.CreateCourseRequest;
import com.pm4.istp.domain.entites.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CourseService {
    Course createCourse(CreateCourseRequest course);
    Page<Course> listCoursesForInstructors(UUID instructorId, Pageable pageable);
}
