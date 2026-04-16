package com.pm4.istp.course.services;

import com.pm4.istp.course.db.CreateCourseRequest;
import com.pm4.istp.course.db.UpdateCourseRequest;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.dto.CourseChallengeItemDto;
import com.pm4.istp.course.dto.ListCourseResponseDto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseService {
  Course createCourse(UUID userId, CreateCourseRequest course);

  Course getCourse(UUID userId, UUID courseId);

  Course enrollInCourse(UUID userId, UUID courseId);

  Course updateCourse(UUID userId, UUID courseId, UpdateCourseRequest request);

  void deleteCourse(UUID userId, UUID courseId);

  Course updateCourseChallenges(
      UUID userId, UUID courseId, List<CourseChallengeItemDto> challenges);

  Page<ListCourseResponseDto> listCoursesForInstructors(UUID instructorId, Pageable pageable);

  Page<ListCourseResponseDto> listUserEnrollments(UUID userId, Pageable pageable);

  Page<ListCourseResponseDto> listPublishedCourses(String query, Pageable pageable);

  Course joinByInviteCode(String code, UUID studentId);

  Course regenerateInviteCode(UUID courseId, UUID userId);
}
