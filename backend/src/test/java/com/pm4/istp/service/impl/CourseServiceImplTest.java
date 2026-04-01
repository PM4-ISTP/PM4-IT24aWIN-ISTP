package com.pm4.istp.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.domain.CreateCourseInstructorRequest;
import com.pm4.istp.domain.CreateCourseRequest;
import com.pm4.istp.domain.UpdateCourseInstructorRequest;
import com.pm4.istp.domain.UpdateCourseRequest;
import com.pm4.istp.domain.entites.Course;
import com.pm4.istp.domain.entites.CourseInstructor;
import com.pm4.istp.domain.entites.InstructorRoleEnum;
import com.pm4.istp.domain.entites.User;
import com.pm4.istp.domain.entites.UserRoleEnum;
import com.pm4.istp.exception.CourseAccessDeniedException;
import com.pm4.istp.exception.InvalidCourseCollaboratorException;
import com.pm4.istp.repositories.CourseRepository;
import com.pm4.istp.repositories.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private CourseRepository courseRepository;

  @InjectMocks private CourseServiceImpl courseService;

  @Test
  void createCourse_withCollaborator_createsOwnerAndCollaboratorRelations() {
    UUID ownerId = UUID.randomUUID();
    UUID collaboratorId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);
    owner.setName("Owner");
    owner.setRoles(Set.of(UserRoleEnum.ROLE_INSTRUCTOR));

    User collaborator = new User();
    collaborator.setId(collaboratorId);
    collaborator.setName("Collaborator");
    collaborator.setRoles(Set.of(UserRoleEnum.ROLE_ADMINISTRATOR));

    when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
    when(userRepository.findById(collaboratorId)).thenReturn(Optional.of(collaborator));
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CreateCourseRequest request =
        new CreateCourseRequest(
            "Secure Coding",
            "Intro",
            false,
            List.of(new CreateCourseInstructorRequest(collaboratorId, InstructorRoleEnum.COLLABORATOR)));

    Course result = courseService.createCourse(ownerId, request);

    assertThat(result.getTitle()).isEqualTo("Secure Coding");
    assertThat(result.getCourseInstructors()).hasSize(2);

    CourseInstructor ownerRelation =
        result.getCourseInstructors().stream()
            .filter(ci -> ci.getInstructorRole() == InstructorRoleEnum.OWNER)
            .findFirst()
            .orElseThrow();
    assertThat(ownerRelation.isAccepted()).isTrue();
    assertThat(ownerRelation.getAcceptedAt()).isNotNull();
    assertThat(ownerRelation.getInstructor().getId()).isEqualTo(ownerId);
    assertThat(ownerRelation.getCourse()).isSameAs(result);

    CourseInstructor collaboratorRelation =
        result.getCourseInstructors().stream()
            .filter(ci -> ci.getInstructorRole() == InstructorRoleEnum.COLLABORATOR)
            .findFirst()
            .orElseThrow();
    assertThat(collaboratorRelation.isAccepted()).isFalse();
    assertThat(collaboratorRelation.getInstructor().getId()).isEqualTo(collaboratorId);
    assertThat(collaboratorRelation.getCourse()).isSameAs(result);

    verify(courseRepository).save(any(Course.class));
  }

  @Test
  void getCourse_whenUserIsNotInstructor_throwsCourseAccessDeniedException() {
    UUID instructorId = UUID.randomUUID();
    UUID requesterId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User instructor = new User();
    instructor.setId(instructorId);
    instructor.setRoles(Set.of(UserRoleEnum.ROLE_INSTRUCTOR));

    Course course = new Course();
    course.setId(courseId);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(instructor);
    course.addCourseInstructor(ownerRelation);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    assertThatThrownBy(() -> courseService.getCourse(requesterId, courseId))
        .isInstanceOf(CourseAccessDeniedException.class);
  }

  @Test
  void updateCourse_replacesCollaboratorSet_andKeepsOwner() {
    UUID ownerId = UUID.randomUUID();
    UUID oldCollaboratorId = UUID.randomUUID();
    UUID newCollaboratorId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);
    owner.setRoles(Set.of(UserRoleEnum.ROLE_ADMINISTRATOR));

    User oldCollaborator = new User();
    oldCollaborator.setId(oldCollaboratorId);
    oldCollaborator.setRoles(Set.of(UserRoleEnum.ROLE_INSTRUCTOR));

    User newCollaborator = new User();
    newCollaborator.setId(newCollaboratorId);
    newCollaborator.setRoles(Set.of(UserRoleEnum.ROLE_ADMINISTRATOR));

    Course course = new Course();
    course.setId(courseId);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    ownerRelation.setAccepted(true);
    course.addCourseInstructor(ownerRelation);

    CourseInstructor oldCollaboratorRelation = new CourseInstructor();
    oldCollaboratorRelation.setInstructorRole(InstructorRoleEnum.COLLABORATOR);
    oldCollaboratorRelation.setInstructor(oldCollaborator);
    course.addCourseInstructor(oldCollaboratorRelation);

    UpdateCourseRequest updateRequest =
        new UpdateCourseRequest(
            "Updated title",
            "Updated description",
            true,
            List.of(new UpdateCourseInstructorRequest(newCollaboratorId, InstructorRoleEnum.COLLABORATOR)));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(userRepository.findById(newCollaboratorId)).thenReturn(Optional.of(newCollaborator));
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Course updated = courseService.updateCourse(ownerId, courseId, updateRequest);

    assertThat(updated.getTitle()).isEqualTo("Updated title");
    assertThat(updated.getDescription()).isEqualTo("Updated description");
    assertThat(updated.isPublished()).isTrue();

    assertThat(updated.getCourseInstructors())
        .extracting(ci -> ci.getInstructor().getId())
        .contains(ownerId, newCollaboratorId)
        .doesNotContain(oldCollaboratorId);

    assertThat(updated.getCourseInstructors())
        .filteredOn(ci -> ci.getInstructorRole() == InstructorRoleEnum.OWNER)
        .hasSize(1);
  }

  @Test
  void createCourse_withStudentCollaborator_throwsInvalidCourseCollaboratorException() {
    UUID ownerId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);
    owner.setRoles(Set.of(UserRoleEnum.ROLE_INSTRUCTOR));

    User student = new User();
    student.setId(studentId);
    student.setRoles(Set.of(UserRoleEnum.ROLE_STUDENT));

    when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

    CreateCourseRequest request =
        new CreateCourseRequest(
            "Secure Coding",
            "Intro",
            false,
            List.of(new CreateCourseInstructorRequest(studentId, InstructorRoleEnum.COLLABORATOR)));

    assertThatThrownBy(() -> courseService.createCourse(ownerId, request))
        .isInstanceOf(InvalidCourseCollaboratorException.class)
        .hasMessage("Only admins or instructors can be added as collaborators");
  }
}
