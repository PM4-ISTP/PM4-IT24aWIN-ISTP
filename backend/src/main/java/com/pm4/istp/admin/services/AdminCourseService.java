package com.pm4.istp.admin.services;

import com.pm4.istp.admin.dto.AdminCourseListItemDto;
import com.pm4.istp.admin.dto.AdminUpdateCourseRequestDto;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.db.entities.CourseStatusEnum;
import com.pm4.istp.course.exceptions.CourseNotFoundException;
import com.pm4.istp.course.repositories.CourseRepository;
import com.pm4.istp.course.services.CourseInviteCodeHelper;
import com.pm4.istp.course.services.CourseTopicService;
import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.repositories.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCourseService {
  private static final String COURSE_NOT_FOUND_MSG = "Course with ID '%s' not found";

  private final CourseRepository courseRepository;
  private final CourseTopicService courseTopicService;
  private final CourseInviteCodeHelper courseInviteCodeHelper;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public Page<AdminCourseListItemDto> listCourses(String query, Pageable pageable) {
    String normalizedQuery = normalizeBlankToNull(query);

    if (normalizedQuery == null) {
      return courseRepository.findAllCoursesForAdmin(pageable);
    }

    return courseRepository.findAllCoursesForAdminByQuery(normalizedQuery, pageable);
  }

  public void updateCourse(UUID courseId, AdminUpdateCourseRequestDto request) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));

    course.setTitle(request.getTitle());
    course.setDescription(request.getDescription());
    course.setShortDescription(normalizeBlankToNull(request.getShortDescription()));
    course.setStatus(request.getStatus());
    course.setTopic(courseTopicService.normalizeAndValidate(request.getTopic()));
    course.setImageUrl(normalizeBlankToNull(request.getImageUrl()));

    // Clear invite code when course is no longer private.
    if (request.getStatus() != CourseStatusEnum.PRIVATE) {
      course.setInviteCode(null);
    }

    courseRepository.save(course);

    // Ensure private courses have an invite code. CourseInviteCodeHelper assigns the code in its
    // own REQUIRES_NEW transaction so that a unique-constraint collision on a concurrent insert
    // only rolls back that single attempt, leaving the caller's transaction intact for a retry.
    if (request.getStatus() == CourseStatusEnum.PRIVATE
        && (course.getInviteCode() == null || course.getInviteCode().isBlank())) {
      courseInviteCodeHelper.generateAndAssign(courseId);
    }
  }

  public void deleteCourse(UUID courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));
    if (course.getDeletedAt() == null) {
      UUID actorId = resolveActorIdFromSecurityContext();
      String deletedByUsername =
          actorId == null
              ? "unknown"
              : userRepository
                  .findByIdAndDeletedAtIsNull(actorId)
                  .map(User::getUsername)
                  .orElse("unknown");
      course.setStatus(CourseStatusEnum.SOFT_DELETED);
      course.setDeletedByUsername(deletedByUsername);
      course.setDeletedAt(LocalDateTime.now());
      courseRepository.save(course);
    }
  }

  private String normalizeBlankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private UUID resolveActorIdFromSecurityContext() {
    try {
      var auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth == null) {
        return null;
      }
      Object principal = auth.getPrincipal();
      if (principal instanceof Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
      }
      return null;
    } catch (Exception ignored) {
      return null;
    }
  }
}
