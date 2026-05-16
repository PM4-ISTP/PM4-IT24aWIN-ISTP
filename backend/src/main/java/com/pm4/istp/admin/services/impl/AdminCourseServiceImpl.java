package com.pm4.istp.admin.services.impl;

import com.pm4.istp.admin.dto.AdminCourseListItemDto;
import com.pm4.istp.admin.dto.AdminUpdateCourseRequestDto;
import com.pm4.istp.admin.dto.DeleteCheckResponseDto;
import com.pm4.istp.admin.exceptions.HardDeleteBlockedException;
import com.pm4.istp.admin.services.AdminDeleteCheckService;
import com.pm4.istp.admin.services.AdminCourseService;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.exceptions.CourseNotFoundException;
import com.pm4.istp.course.repositories.CourseRepository;
import com.pm4.istp.course.services.CourseInviteCodeHelper;
import com.pm4.istp.course.services.CourseTopicService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCourseServiceImpl implements AdminCourseService {
  private static final String COURSE_NOT_FOUND_MSG = "Course with ID '%s' not found";

  private final CourseRepository courseRepository;
  private final CourseTopicService courseTopicService;
  private final CourseInviteCodeHelper courseInviteCodeHelper;
  private final AdminDeleteCheckService adminDeleteCheckService;

  @Override
  @Transactional(readOnly = true)
  public Page<AdminCourseListItemDto> listCourses(String query, Pageable pageable) {
    String normalizedQuery = normalizeBlankToNull(query);

    if (normalizedQuery == null) {
      return courseRepository.findAllCoursesForAdmin(pageable);
    }

    return courseRepository.findAllCoursesForAdminByQuery(normalizedQuery, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AdminCourseListItemDto> listRemovedCourses(String query, Pageable pageable) {
    String normalizedQuery = normalizeBlankToNull(query);

    if (normalizedQuery == null) {
      return courseRepository.findRemovedCoursesForAdmin(pageable);
    }

    return courseRepository.findRemovedCoursesForAdminByQuery(normalizedQuery, pageable);
  }

  @Override
  public void updateCourse(UUID courseId, AdminUpdateCourseRequestDto request) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));

    validateVisibilityState(request.isPublished(), request.isPrivate());

    course.setTitle(request.getTitle());
    course.setDescription(request.getDescription());
    course.setShortDescription(normalizeBlankToNull(request.getShortDescription()));
    course.setPublished(request.isPublished());
    course.setPrivate(request.isPrivate());
    course.setTopic(courseTopicService.normalizeAndValidate(request.getTopic()));
    course.setImageUrl(normalizeBlankToNull(request.getImageUrl()));

    // Clear invite code when course is no longer private.
    if (!request.isPrivate()) {
      course.setInviteCode(null);
    }

    courseRepository.save(course);

    // Ensure private courses have an invite code. CourseInviteCodeHelper assigns the code in its
    // own REQUIRES_NEW transaction so that a unique-constraint collision on a concurrent insert
    // only rolls back that single attempt, leaving the caller's transaction intact for a retry.
    if (request.isPrivate()
        && (course.getInviteCode() == null || course.getInviteCode().isBlank())) {
      courseInviteCodeHelper.generateAndAssign(courseId);
    }
  }

  @Override
  public void deleteCourse(UUID courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));
    if (course.getDeletedAt() == null) {
      course.setDeletedAt(LocalDateTime.now());
      courseRepository.save(course);
      return;
    }

    DeleteCheckResponseDto check = adminDeleteCheckService.checkCourse(courseId);
    if (!check.hardDeleteAllowed()) {
      throw new HardDeleteBlockedException("Hard delete is blocked because related data still exists.");
    }
    courseRepository.delete(course);
  }

  private String normalizeBlankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private void validateVisibilityState(boolean published, boolean privateCourse) {
    if (published && privateCourse) {
      throw new IllegalArgumentException("Course cannot be published and private at the same time");
    }
  }
}
