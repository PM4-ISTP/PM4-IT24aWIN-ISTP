package com.pm4.istp.course.db.entities;

import static org.assertj.core.api.Assertions.assertThat;

import com.pm4.istp.user.db.entities.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CourseEntityTest {

  @Test
  void defaultsMatchStudentFacingCourseSettings() {
    Course course = new Course();

    assertThat(course.getMcAttemptsMode()).isEqualTo(McAttemptsMode.UNLIMITED);
    assertThat(course.isBadgeEnabled()).isTrue();
    assertThat(course.getCourseInstructors()).isEmpty();
    assertThat(course.getCourseEnrollments()).isEmpty();
    assertThat(course.getCourseLabs()).isEmpty();
  }

  @Test
  void addAndRemoveCourseInstructorMaintainsBothSides() {
    Course course = new Course();
    CourseInstructor instructor = new CourseInstructor();
    instructor.setInstructor(new User());

    course.addCourseInstructor(instructor);

    assertThat(course.getCourseInstructors()).containsExactly(instructor);
    assertThat(instructor.getCourse()).isSameAs(course);

    course.removeCourseInstructor(instructor);

    assertThat(course.getCourseInstructors()).isEmpty();
    assertThat(instructor.getCourse()).isNull();
  }

  @Test
  void addAndRemoveCourseEnrollmentMaintainsBothSides() {
    Course course = new Course();
    CourseEnrollment enrollment = new CourseEnrollment();
    enrollment.setParticipant(new User());

    course.addCourseEnrollment(enrollment);

    assertThat(course.getCourseEnrollments()).containsExactly(enrollment);
    assertThat(enrollment.getCourse()).isSameAs(course);

    course.removeCourseEnrollment(enrollment);

    assertThat(course.getCourseEnrollments()).isEmpty();
    assertThat(enrollment.getCourse()).isNull();
  }

  @Test
  void addAndRemoveCourseChallengeMaintainsBothSides() {
    Course course = new Course();
    CourseLab courseLab = new CourseLab();
    courseLab.setId(UUID.randomUUID());

    course.addCourseChallenge(courseLab);

    assertThat(course.getCourseLabs()).containsExactly(courseLab);
    assertThat(courseLab.getCourse()).isSameAs(course);

    course.removeCourseChallenge(courseLab);

    assertThat(course.getCourseLabs()).isEmpty();
    assertThat(courseLab.getCourse()).isNull();
  }
}
