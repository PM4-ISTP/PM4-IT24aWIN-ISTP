package com.pm4.istp.course.services;

import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.exceptions.CourseNotFoundException;
import com.pm4.istp.course.exceptions.InviteCodeGenerationException;
import com.pm4.istp.course.repositories.CourseRepository;
import java.security.SecureRandom;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Owns all invite-code generation and assignment. Each assignment attempt runs in a dedicated
 * transaction so a unique-constraint violation only rolls back that attempt, leaving the caller's
 * transaction intact for a retry.
 */
@Component
@RequiredArgsConstructor
public class CourseInviteCodeHelper {

  private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final int INVITE_CODE_LENGTH = 6;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final CourseRepository courseRepository;
  private final PlatformTransactionManager transactionManager;

  public String generateAndAssign(UUID courseId) {
    for (int attempt = 0; attempt < 10; attempt++) {
      String code = generateCode();
      try {
        runInNewTransaction(() -> assignInviteCode(courseId, code));
        return code;
      } catch (DataIntegrityViolationException ex) {
        if (!isInviteCodeConstraintViolation(ex)) {
          throw ex;
        }
        if (attempt == 9) {
          throw new InviteCodeGenerationException(
              "Could not generate a unique invite code after 10 attempts", ex);
        }
      }
    }
    throw new InviteCodeGenerationException(
        "Could not generate a unique invite code after 10 attempts");
  }

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

  public Course saveNewCourseWithInviteCode(Course course) {
    for (int attempt = 0; attempt < 10; attempt++) {
      String code = generateCode();
      try {
        return runInNewTransaction(() -> saveNewCourse(course, code));
      } catch (DataIntegrityViolationException ex) {
        if (!isInviteCodeConstraintViolation(ex)) {
          throw ex;
        }
        if (attempt == 9) {
          throw new InviteCodeGenerationException(
              "Could not generate a unique invite code after 10 attempts", ex);
        }
      }
    }
    throw new InviteCodeGenerationException(
        "Could not generate a unique invite code after 10 attempts");
  }

  private Course saveNewCourse(Course course, String code) {
    course.setInviteCode(code);
    return courseRepository.save(course);
  }

  public String generateCode() {
    StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
    for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
      sb.append(INVITE_CODE_CHARS.charAt(SECURE_RANDOM.nextInt(INVITE_CODE_CHARS.length())));
    }
    return sb.toString();
  }

  private static boolean isInviteCodeConstraintViolation(DataIntegrityViolationException ex) {
    Throwable current = ex;
    while (current != null) {
      String msg = current.getMessage();
      if (msg != null && msg.contains("uk_courses_invite_code")) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private void runInNewTransaction(Runnable action) {
    TransactionTemplate template = newTransactionTemplate();
    template.executeWithoutResult(status -> action.run());
  }

  private <T> T runInNewTransaction(java.util.function.Supplier<T> action) {
    TransactionTemplate template = newTransactionTemplate();
    return template.execute(status -> action.get());
  }

  private TransactionTemplate newTransactionTemplate() {
    TransactionTemplate template = new TransactionTemplate(transactionManager);
    template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return template;
  }
}
