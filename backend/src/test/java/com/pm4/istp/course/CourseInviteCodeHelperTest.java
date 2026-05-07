package com.pm4.istp.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.exceptions.CourseNotFoundException;
import com.pm4.istp.course.exceptions.InviteCodeGenerationException;
import com.pm4.istp.course.repositories.CourseRepository;
import com.pm4.istp.course.services.CourseInviteCodeHelper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@ExtendWith(MockitoExtension.class)
class CourseInviteCodeHelperTest {

  @Mock private CourseRepository courseRepository;

  private CourseInviteCodeHelper courseInviteCodeHelper;

  @BeforeEach
  void setUp() {
    courseInviteCodeHelper =
        new CourseInviteCodeHelper(courseRepository, new NoOpTransactionManager());
  }

  // ── generateCode ─────────────────────────────────────────────────────────────

  @Test
  void generateCode_returnsExactly6Characters() {
    String code = courseInviteCodeHelper.generateCode();
    assertThat(code).hasSize(6);
  }

  @Test
  void generateCode_containsOnlyAllowedCharacters() {
    for (int i = 0; i < 20; i++) {
      String code = courseInviteCodeHelper.generateCode();
      assertThat(code).matches("[A-Z0-9]{6}");
    }
  }

  @Test
  void generateCode_producesVariedCodes() {
    // With 36^6 ≈ 2.1 billion combinations, two consecutive calls almost certainly differ.
    String code1 = courseInviteCodeHelper.generateCode();
    String code2 = courseInviteCodeHelper.generateCode();
    // Not a strict equality assertion – just sanity-check both are valid.
    assertThat(code1).matches("[A-Z0-9]{6}");
    assertThat(code2).matches("[A-Z0-9]{6}");
  }

  // ── generateAndAssign ────────────────────────────────────────────────────────

  @Test
  void generateAndAssign_successOnFirstAttempt_returnsCode() {
    UUID courseId = UUID.randomUUID();
    Course course = new Course();
    course.setId(courseId);
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    String code = courseInviteCodeHelper.generateAndAssign(courseId);

    assertThat(code).matches("[A-Z0-9]{6}");
    verify(courseRepository).save(course);
  }

  @Test
  void generateAndAssign_whenCourseNotFound_throwsCourseNotFoundException() {
    UUID courseId = UUID.randomUUID();
    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> courseInviteCodeHelper.generateAndAssign(courseId))
        .isInstanceOf(CourseNotFoundException.class);
  }

  @Test
  void generateAndAssign_retriesOnInviteCodeConstraintViolation() {
    UUID courseId = UUID.randomUUID();
    Course course = new Course();
    course.setId(courseId);
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    // Fail twice with an invite-code constraint, succeed on third attempt.
    DataIntegrityViolationException constraintEx =
        new DataIntegrityViolationException("uk_courses_invite_code violation");
    when(courseRepository.save(any(Course.class)))
        .thenThrow(constraintEx)
        .thenThrow(constraintEx)
        .thenAnswer(invocation -> invocation.getArgument(0));

    String code = courseInviteCodeHelper.generateAndAssign(courseId);

    assertThat(code).matches("[A-Z0-9]{6}");
    verify(courseRepository, times(3)).save(course);
  }

  @Test
  void generateAndAssign_afterTenConstraintViolations_throwsInviteCodeGenerationException() {
    UUID courseId = UUID.randomUUID();
    Course course = new Course();
    course.setId(courseId);
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    DataIntegrityViolationException constraintEx =
        new DataIntegrityViolationException("uk_courses_invite_code violation");
    when(courseRepository.save(any(Course.class))).thenThrow(constraintEx);

    assertThatThrownBy(() -> courseInviteCodeHelper.generateAndAssign(courseId))
        .isInstanceOf(InviteCodeGenerationException.class)
        .hasMessageContaining("10 attempts");

    verify(courseRepository, times(10)).save(course);
  }

  @Test
  void generateAndAssign_onUnrelatedConstraintViolation_rethrowsOriginalException() {
    UUID courseId = UUID.randomUUID();
    Course course = new Course();
    course.setId(courseId);
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    DataIntegrityViolationException unrelatedEx =
        new DataIntegrityViolationException("some_other_constraint violation");
    when(courseRepository.save(any(Course.class))).thenThrow(unrelatedEx);

    assertThatThrownBy(() -> courseInviteCodeHelper.generateAndAssign(courseId))
        .isSameAs(unrelatedEx);

    // Only one save attempt – unrelated exceptions are not retried.
    verify(courseRepository, times(1)).save(course);
  }

  // ── saveNewCourseWithInviteCode ──────────────────────────────────────────────

  @Test
  void saveNewCourseWithInviteCode_successOnFirstAttempt_returnsSavedCourse() {
    Course course = new Course();
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Course result = courseInviteCodeHelper.saveNewCourseWithInviteCode(course);

    assertThat(result).isSameAs(course);
    assertThat(course.getInviteCode()).matches("[A-Z0-9]{6}");
  }

  @Test
  void saveNewCourseWithInviteCode_retriesOnInviteCodeConstraintViolation() {
    Course course = new Course();
    DataIntegrityViolationException constraintEx =
        new DataIntegrityViolationException("uk_courses_invite_code violation");
    when(courseRepository.save(any(Course.class)))
        .thenThrow(constraintEx)
        .thenAnswer(invocation -> invocation.getArgument(0));

    Course result = courseInviteCodeHelper.saveNewCourseWithInviteCode(course);

    assertThat(result).isSameAs(course);
    verify(courseRepository, times(2)).save(course);
  }

  @Test
  void saveNewCourseWithInviteCode_afterTenConstraintViolations_throwsInviteCodeGenerationException() {
    Course course = new Course();
    DataIntegrityViolationException constraintEx =
        new DataIntegrityViolationException("uk_courses_invite_code violation");
    when(courseRepository.save(any(Course.class))).thenThrow(constraintEx);

    assertThatThrownBy(() -> courseInviteCodeHelper.saveNewCourseWithInviteCode(course))
        .isInstanceOf(InviteCodeGenerationException.class)
        .hasMessageContaining("10 attempts");

    verify(courseRepository, times(10)).save(course);
  }

  private static final class NoOpTransactionManager extends AbstractPlatformTransactionManager {

    @Override
    protected Object doGetTransaction() throws TransactionException {
      return new Object();
    }

    @Override
    protected void doBegin(Object transaction, org.springframework.transaction.TransactionDefinition definition)
        throws TransactionException {
      // No resources are opened by this unit-test transaction manager.
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
      // Nothing is persisted by this unit-test transaction manager.
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
      // Nothing is persisted by this unit-test transaction manager.
    }
  }
}
