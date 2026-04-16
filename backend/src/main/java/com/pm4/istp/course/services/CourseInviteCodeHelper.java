package com.pm4.istp.course.services;

import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.exceptions.CourseNotFoundException;
import com.pm4.istp.course.repositories.CourseRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Helper component for invite-code assignment that runs each attempt in its own transaction.
 * Running in {@link Propagation#REQUIRES_NEW} ensures the transaction can be rolled back on a
 * unique-constraint violation without corrupting the caller's session, enabling the caller to retry
 * with a freshly generated code.
 */
@Component
@RequiredArgsConstructor
public class CourseInviteCodeHelper {

  private final CourseRepository courseRepository;

  /**
   * Assigns {@code code} to the course identified by {@code courseId} and persists the change
   * within a new, independent transaction.
   *
   * @throws CourseNotFoundException if the course no longer exists
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void assignInviteCode(UUID courseId, String code) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () ->
                    new CourseNotFoundException(
                        String.format("Course with ID '%s' not found", courseId)));
    course.setInviteCode(code);
    courseRepository.save(course);
  }
}
