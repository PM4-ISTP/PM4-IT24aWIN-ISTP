package com.pm4.istp.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.pm4.istp.course.db.CreateCourseInstructorRequest;
import com.pm4.istp.course.db.CreateCourseRequest;
import com.pm4.istp.course.db.InstructorRoleEnum;
import com.pm4.istp.course.db.UpdateCourseInstructorRequest;
import com.pm4.istp.course.db.UpdateCourseRequest;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.db.entities.CourseEnrollment;
import com.pm4.istp.course.db.entities.CourseInstructor;
import com.pm4.istp.course.dto.CourseChallengeItemDto;
import com.pm4.istp.course.dto.ListCourseResponseDto;
import com.pm4.istp.course.exceptions.ChallengeNotFoundException;
import com.pm4.istp.course.exceptions.CourseAccessDeniedException;
import com.pm4.istp.course.exceptions.CourseNotFoundException;
import com.pm4.istp.course.exceptions.InvalidCourseChallengeException;
import com.pm4.istp.course.exceptions.InvalidCourseShortDescriptionException;
import com.pm4.istp.course.exceptions.InvalidInviteCodeException;
import com.pm4.istp.course.exceptions.InviteCodeGenerationException;
import com.pm4.istp.course.repositories.ChallengeRepository;
import com.pm4.istp.course.repositories.CourseEnrollmentRepository;
import com.pm4.istp.course.repositories.CourseRepository;
import com.pm4.istp.course.services.CourseInviteCodeHelper;
import com.pm4.istp.course.services.impl.CourseServiceImpl;
import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.db.entities.UserRoleEnum;
import com.pm4.istp.user.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

  @Mock
  private UserRepository userRepository;
  @Mock
  private CourseRepository courseRepository;
  @Mock
  private CourseEnrollmentRepository courseEnrollmentRepository;
  @Mock
  private ChallengeRepository challengeRepository;
  @Mock
  private CourseInviteCodeHelper courseInviteCodeHelper;

  @InjectMocks
  private CourseServiceImpl courseService;

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

    CreateCourseRequest request = new CreateCourseRequest(
        "Secure Coding",
        "Intro",
        "Learn the secure coding basics.",
        false,
        false,
        null,
        null,
        List.of(new CreateCourseInstructorRequest(collaboratorId, InstructorRoleEnum.COLLABORATOR)));

    Course result = courseService.createCourse(ownerId, request);

    assertThat(result.getTitle()).isEqualTo("Secure Coding");
    assertThat(result.getShortDescription()).isEqualTo("Learn the secure coding basics.");
    assertThat(result.getCourseInstructors()).hasSize(2);

    CourseInstructor ownerRelation = result.getCourseInstructors().stream()
        .filter(ci -> ci.getInstructorRole() == InstructorRoleEnum.OWNER)
        .findFirst()
        .orElseThrow();
    assertThat(ownerRelation.isAccepted()).isTrue();
    assertThat(ownerRelation.getAcceptedAt()).isNotNull();
    assertThat(ownerRelation.getInstructor().getId()).isEqualTo(ownerId);
    assertThat(ownerRelation.getCourse()).isSameAs(result);

    CourseInstructor collaboratorRelation = result.getCourseInstructors().stream()
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
  void getCourse_whenPublished_returnsCourseForAnyAuthenticatedUser() {
    UUID requesterId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    Course course = new Course();
    course.setId(courseId);
    course.setPublished(true);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    Course result = courseService.getCourse(requesterId, courseId);

    assertThat(result).isSameAs(course);
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

    UpdateCourseRequest updateRequest = new UpdateCourseRequest(
        "Updated title",
        "Updated description",
        "Updated short description for the header.",
        true,
        false,
        null,
        null,
        List.of(new UpdateCourseInstructorRequest(newCollaboratorId, InstructorRoleEnum.COLLABORATOR)));

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(userRepository.findById(newCollaboratorId)).thenReturn(Optional.of(newCollaborator));
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Course updated = courseService.updateCourse(ownerId, courseId, updateRequest);

    assertThat(updated.getTitle()).isEqualTo("Updated title");
    assertThat(updated.getDescription()).isEqualTo("Updated description");
    assertThat(updated.getShortDescription()).isEqualTo("Updated short description for the header.");
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
  void listPublishedCourses_delegatesToRepositoryWithNormalizedQuery() {
    Pageable pageable = PageRequest.of(0, 12);
    Page<ListCourseResponseDto> expected = new PageImpl<>(List.of());

    when(courseRepository.findPublishedCoursesByQuery("secure", pageable)).thenReturn(expected);

    Page<ListCourseResponseDto> result = courseService.listPublishedCourses("  secure  ", pageable);

    assertThat(result).isSameAs(expected);
    verify(courseRepository).findPublishedCoursesByQuery("secure", pageable);
  }

  @Test
  void listPublishedCourses_withBlankQuery_usesNullFilter() {
    Pageable pageable = PageRequest.of(0, 12);
    Page<ListCourseResponseDto> expected = new PageImpl<>(List.of());

    when(courseRepository.findPublishedCourses(pageable)).thenReturn(expected);

    Page<ListCourseResponseDto> result = courseService.listPublishedCourses("   ", pageable);

    assertThat(result).isSameAs(expected);
    verify(courseRepository).findPublishedCourses(pageable);
  }

  @Test
  void createCourse_withTooManyShortDescriptionChars_throwsValidationException() {
    UUID ownerId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);
    owner.setRoles(Set.of(UserRoleEnum.ROLE_INSTRUCTOR));

    when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

    String tooLong = "a".repeat(201);

    CreateCourseRequest request = new CreateCourseRequest(
        "Secure Coding",
        "Long description",
        tooLong,
        false,
        false,
        null,
        null,
        List.of());

    assertThatThrownBy(() -> courseService.createCourse(ownerId, request))
        .isInstanceOf(InvalidCourseShortDescriptionException.class)
        .hasMessageContaining("200")
        .hasMessageContaining("characters");

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void createCourse_withPublishedAndPrivate_throwsIllegalArgumentException() {
    UUID ownerId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);
    owner.setRoles(Set.of(UserRoleEnum.ROLE_INSTRUCTOR));

    when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

    CreateCourseRequest request = new CreateCourseRequest(
        "Secure Coding",
        "Long description",
        "Short summary.",
        true,
        true,
        null,
        null,
        List.of());

    assertThatThrownBy(() -> courseService.createCourse(ownerId, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("published and private");

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void enrollInCourse_whenPublished_createsEnrollment() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User participant = new User();
    participant.setId(userId);
    participant.setRoles(Set.of(UserRoleEnum.ROLE_STUDENT));

    Course course = new Course();
    course.setId(courseId);
    course.setPublished(true);

    when(userRepository.findById(userId)).thenReturn(Optional.of(participant));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
        .thenReturn(false);
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Course enrolledCourse = courseService.enrollInCourse(userId, courseId);

    assertThat(enrolledCourse.getCourseEnrollments()).hasSize(1);
    CourseEnrollment enrollment = enrolledCourse.getCourseEnrollments().getFirst();
    assertThat(enrollment.getParticipant().getId()).isEqualTo(userId);
    assertThat(enrollment.getCourse()).isSameAs(enrolledCourse);
    verify(courseRepository).save(course);
  }

  @Test
  void enrollInCourse_whenAlreadyEnrolled_returnsCourseWithoutSavingAgain() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User participant = new User();
    participant.setId(userId);

    Course course = new Course();
    course.setId(courseId);
    course.setPublished(true);

    when(userRepository.findById(userId)).thenReturn(Optional.of(participant));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
        .thenReturn(true);

    Course result = courseService.enrollInCourse(userId, courseId);

    assertThat(result).isSameAs(course);
    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void enrollInCourse_whenConcurrentDuplicateInsert_treatsAsAlreadyEnrolled() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User participant = new User();
    participant.setId(userId);

    Course course = new Course();
    course.setId(courseId);
    course.setPublished(true);

    when(userRepository.findById(userId)).thenReturn(Optional.of(participant));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
        .thenReturn(false);
    when(courseRepository.save(any(Course.class)))
        .thenThrow(new DataIntegrityViolationException("uk_course_enrollment_course_participant"));

    Course result = courseService.enrollInCourse(userId, courseId);

    assertThat(result).isSameAs(course);
  }

  @Test
  void deleteCourse_whenOwner_deletesCourse() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);
    owner.setRoles(Set.of(UserRoleEnum.ROLE_INSTRUCTOR));

    Course course = new Course();
    course.setId(courseId);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    courseService.deleteCourse(ownerId, courseId);

    verify(courseRepository).delete(course);
  }

  @Test
  void deleteCourse_whenNotOwner_throwsCourseAccessDeniedException() {
    UUID ownerId = UUID.randomUUID();
    UUID nonOwnerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);
    owner.setRoles(Set.of(UserRoleEnum.ROLE_INSTRUCTOR));

    Course course = new Course();
    course.setId(courseId);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    assertThatThrownBy(() -> courseService.deleteCourse(nonOwnerId, courseId))
        .isInstanceOf(CourseAccessDeniedException.class);

    verify(courseRepository, never()).delete(any(Course.class));
  }

  @Test
  void deleteCourse_whenCourseNotFound_throwsCourseNotFoundException() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> courseService.deleteCourse(userId, courseId))
        .isInstanceOf(CourseNotFoundException.class);

    verify(courseRepository, never()).delete(any(Course.class));
  }

  @Test
  void createCourse_withoutCollaborators_createsOnlyOwnerRelation() {
    UUID ownerId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);
    owner.setName("Owner");
    owner.setRoles(Set.of(UserRoleEnum.ROLE_INSTRUCTOR));

    when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CreateCourseRequest request = new CreateCourseRequest(
        "Solo Course", "Desc", "Short solo summary.", false, false, null, null, List.of());

    Course result = courseService.createCourse(ownerId, request);

    assertThat(result.getCourseInstructors()).hasSize(1);
    assertThat(result.getCourseInstructors().stream()
        .allMatch(ci -> ci.getInstructorRole() == InstructorRoleEnum.OWNER))
        .isTrue();
  }

  @Test
  void getCourse_whenUserIsInstructor_returnsCourse() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User user = new User();
    user.setId(userId);

    Course course = new Course();
    course.setId(courseId);

    CourseInstructor relation = new CourseInstructor();
    relation.setInstructorRole(InstructorRoleEnum.OWNER);
    relation.setInstructor(user);
    course.addCourseInstructor(relation);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    Course result = courseService.getCourse(userId, courseId);

    assertThat(result).isSameAs(course);
  }

  @Test
  void getCourse_whenCourseNotFound_throwsCourseNotFoundException() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> courseService.getCourse(userId, courseId))
        .isInstanceOf(CourseNotFoundException.class);
  }

  @Test
  void updateCourse_whenUserIsNotInstructor_throwsCourseAccessDeniedException() {
    UUID ownerId = UUID.randomUUID();
    UUID outsiderId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = new Course();
    course.setId(courseId);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    UpdateCourseRequest request = new UpdateCourseRequest(
        "Title", "Desc", "Short summary.", false, false, null, null, List.of());

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    assertThatThrownBy(() -> courseService.updateCourse(outsiderId, courseId, request))
        .isInstanceOf(CourseAccessDeniedException.class);

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void updateCourse_whenCourseNotFound_throwsCourseNotFoundException() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    UpdateCourseRequest request = new UpdateCourseRequest(
        "Title", "Desc", "Short summary.", false, false, null, null, List.of());

    assertThatThrownBy(() -> courseService.updateCourse(userId, courseId, request))
        .isInstanceOf(CourseNotFoundException.class);

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void listCoursesForInstructors_delegatesToRepository() {
    UUID instructorId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 10);
    Page<ListCourseResponseDto> expected = new PageImpl<>(List.of());

    when(courseRepository.findListCoursesForInstructor(instructorId, pageable))
        .thenReturn(expected);

    Page<ListCourseResponseDto> result = courseService.listCoursesForInstructors(instructorId, pageable);

    assertThat(result).isSameAs(expected);
    verify(courseRepository).findListCoursesForInstructor(instructorId, pageable);
  }

  private Course buildCourseWithOwner(UUID courseId, User owner) {
    Course course = new Course();
    course.setId(courseId);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    ownerRelation.setAccepted(true);
    course.addCourseInstructor(ownerRelation);

    return course;
  }

  private Challenge buildChallenge(UUID id, User creator, ChallengeStatusEnum status) {
    Challenge challenge = new Challenge();
    challenge.setId(id);
    challenge.setTitle("Challenge " + id);
    challenge.setStatus(status);
    challenge.setCreator(creator);
    return challenge;
  }

  @Test
  void updateCourseChallenges_replacesAssignmentsWithOwnPrivateChallenge() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = buildCourseWithOwner(courseId, owner);
    Challenge challenge = buildChallenge(challengeId, owner, ChallengeStatusEnum.PRIVATE);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Course updated = courseService.updateCourseChallenges(
        ownerId, courseId, List.of(new CourseChallengeItemDto(challengeId, 0)));

    assertThat(updated.getCourseChallenges()).hasSize(1);
    assertThat(updated.getCourseChallenges().getFirst().getChallenge()).isSameAs(challenge);
    assertThat(updated.getCourseChallenges().getFirst().getOrderIndex()).isZero();
    verify(courseRepository).save(course);
  }

  @Test
  void updateCourseChallenges_allowsPublicChallengeFromOtherCreator() {
    UUID ownerId = UUID.randomUUID();
    UUID otherCreatorId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);
    User otherCreator = new User();
    otherCreator.setId(otherCreatorId);

    Course course = buildCourseWithOwner(courseId, owner);
    Challenge challenge = buildChallenge(challengeId, otherCreator, ChallengeStatusEnum.PUBLIC);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Course updated = courseService.updateCourseChallenges(
        ownerId, courseId, List.of(new CourseChallengeItemDto(challengeId, 0)));

    assertThat(updated.getCourseChallenges()).hasSize(1);
  }

  @Test
  void updateCourseChallenges_rejectsDraftChallengeEvenFromOwnCreator() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = buildCourseWithOwner(courseId, owner);
    Challenge challenge = buildChallenge(challengeId, owner, ChallengeStatusEnum.DRAFT);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

    assertThatThrownBy(
        () -> courseService.updateCourseChallenges(
            ownerId, courseId, List.of(new CourseChallengeItemDto(challengeId, 0))))
        .isInstanceOf(InvalidCourseChallengeException.class)
        .hasMessageContaining("draft");

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void updateCourseChallenges_rejectsPrivateChallengeFromOtherCreator() {
    UUID ownerId = UUID.randomUUID();
    UUID otherCreatorId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);
    User otherCreator = new User();
    otherCreator.setId(otherCreatorId);

    Course course = buildCourseWithOwner(courseId, owner);
    Challenge challenge = buildChallenge(challengeId, otherCreator, ChallengeStatusEnum.PRIVATE);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

    assertThatThrownBy(
        () -> courseService.updateCourseChallenges(
            ownerId, courseId, List.of(new CourseChallengeItemDto(challengeId, 0))))
        .isInstanceOf(ChallengeNotFoundException.class);

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void updateCourseChallenges_whenChallengeDoesNotExist_throwsChallengeNotFound() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = buildCourseWithOwner(courseId, owner);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> courseService.updateCourseChallenges(
            ownerId, courseId, List.of(new CourseChallengeItemDto(challengeId, 0))))
        .isInstanceOf(ChallengeNotFoundException.class);

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void updateCourseChallenges_whenCallerIsNotInstructor_throwsAccessDenied() {
    UUID ownerId = UUID.randomUUID();
    UUID outsiderId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = buildCourseWithOwner(courseId, owner);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    assertThatThrownBy(
        () -> courseService.updateCourseChallenges(outsiderId, courseId, List.of()))
        .isInstanceOf(CourseAccessDeniedException.class);

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void updateCourseChallenges_whenCourseNotFound_throwsCourseNotFound() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> courseService.updateCourseChallenges(ownerId, courseId, List.of()))
        .isInstanceOf(CourseNotFoundException.class);
  }

  @Test
  void updateCourseChallenges_withEmptyList_clearsAllAssignments() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = buildCourseWithOwner(courseId, owner);
    // pre-seed with an existing assignment to verify it gets cleared
    Challenge existing = buildChallenge(UUID.randomUUID(), owner, ChallengeStatusEnum.PUBLIC);
    com.pm4.istp.course.db.entities.CourseChallenge existingAssignment = new com.pm4.istp.course.db.entities.CourseChallenge();
    existingAssignment.setChallenge(existing);
    existingAssignment.setOrderIndex(0);
    course.addCourseChallenge(existingAssignment);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Course updated = courseService.updateCourseChallenges(ownerId, courseId, List.of());

    assertThat(updated.getCourseChallenges()).isEmpty();
  }

  // ── joinByInviteCode ───────────────────────────────────────────────────────

  @Test
  void joinByInviteCode_withInvalidCode_throwsInvalidInviteCodeException() {
    UUID studentId = UUID.randomUUID();

    User student = new User();
    student.setId(studentId);

    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(courseRepository.findByInviteCode("BADCOD")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> courseService.joinByInviteCode("BADCOD", studentId))
        .isInstanceOf(InvalidInviteCodeException.class);

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void joinByInviteCode_whenCourseIsPublic_throwsInvalidInviteCodeException() {
    UUID studentId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User student = new User();
    student.setId(studentId);

    Course course = new Course();
    course.setId(courseId);
    course.setPublished(true);
    course.setPrivate(false);

    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(courseRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(course));

    assertThatThrownBy(() -> courseService.joinByInviteCode("ABC123", studentId))
        .isInstanceOf(InvalidInviteCodeException.class);

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void joinByInviteCode_withValidCodeAndPrivateCourse_enrollsParticipant() {
    UUID studentId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User student = new User();
    student.setId(studentId);

    Course course = new Course();
    course.setId(courseId);
    course.setPublished(false);
    course.setPrivate(true);

    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(courseRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(course));
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, studentId))
        .thenReturn(false);
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Course result = courseService.joinByInviteCode("ABC123", studentId);

    assertThat(result.getCourseEnrollments()).hasSize(1);
    assertThat(result.getCourseEnrollments().getFirst().getParticipant().getId())
        .isEqualTo(studentId);
    verify(courseRepository).save(course);
  }

  @Test
  void joinByInviteCode_whenAlreadyEnrolled_isIdempotent() {
    UUID studentId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User student = new User();
    student.setId(studentId);

    Course course = new Course();
    course.setId(courseId);
    course.setPublished(false);
    course.setPrivate(true);

    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(courseRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(course));
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, studentId))
        .thenReturn(true);

    Course result = courseService.joinByInviteCode("ABC123", studentId);

    assertThat(result).isSameAs(course);
    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void joinByInviteCode_whenCallerIsInstructor_isIdempotent() {
    UUID instructorId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User instructor = new User();
    instructor.setId(instructorId);

    Course course = new Course();
    course.setId(courseId);
    course.setPublished(false);
    course.setPrivate(true);

    CourseInstructor relation = new CourseInstructor();
    relation.setInstructorRole(InstructorRoleEnum.COLLABORATOR);
    relation.setInstructor(instructor);
    course.addCourseInstructor(relation);

    when(userRepository.findById(instructorId)).thenReturn(Optional.of(instructor));
    when(courseRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(course));

    Course result = courseService.joinByInviteCode("ABC123", instructorId);

    assertThat(result).isSameAs(course);
    verify(courseRepository, never()).save(any(Course.class));
  }

  // ── regenerateInviteCode ───────────────────────────────────────────────────

  @Test
  void regenerateInviteCode_whenNotOwner_throwsCourseAccessDeniedException() {
    UUID ownerId = UUID.randomUUID();
    UUID nonOwnerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = new Course();
    course.setId(courseId);
    course.setPublished(true);
    course.setPrivate(true);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    assertThatThrownBy(() -> courseService.regenerateInviteCode(courseId, nonOwnerId))
        .isInstanceOf(CourseAccessDeniedException.class);

    verify(courseInviteCodeHelper, never()).assignInviteCode(any(), any());
  }

  @Test
  void regenerateInviteCode_whenCourseIsNotPrivate_throwsCourseAccessDeniedException() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = new Course();
    course.setId(courseId);
    course.setPublished(true);
    course.setPrivate(false);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    assertThatThrownBy(() -> courseService.regenerateInviteCode(courseId, ownerId))
        .isInstanceOf(CourseAccessDeniedException.class)
        .hasMessageContaining("not private");

    verify(courseInviteCodeHelper, never()).assignInviteCode(any(), any());
  }

  @Test
  void regenerateInviteCode_whenOwnerAndPrivate_regeneratesCode() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = new Course();
    course.setId(courseId);
    course.setPublished(false);
    course.setPrivate(true);
    course.setInviteCode("OLDCOD");

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    doNothing().when(courseInviteCodeHelper).assignInviteCode(eq(courseId), any(String.class));

    Course result = courseService.regenerateInviteCode(courseId, ownerId);

    ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
    verify(courseInviteCodeHelper).assignInviteCode(eq(courseId), codeCaptor.capture());
    String generatedCode = codeCaptor.getValue();

    assertThat(result).isSameAs(course);
    assertThat(result.getInviteCode()).isEqualTo(generatedCode);
    assertThat(generatedCode).hasSize(6);
    assertThat(result.getCourseInstructors()).hasSize(1);
  }

  @Test
  void regenerateInviteCode_retriesOnInviteCodeConstraintViolation() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = new Course();
    course.setId(courseId);
    course.setPublished(false);
    course.setPrivate(true);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    doThrow(new DataIntegrityViolationException("uk_courses_invite_code"))
        .doNothing()
        .when(courseInviteCodeHelper)
        .assignInviteCode(eq(courseId), any(String.class));

    Course result = courseService.regenerateInviteCode(courseId, ownerId);

    assertThat(result).isSameAs(course);
    assertThat(result.getInviteCode()).hasSize(6);
    verify(courseInviteCodeHelper, times(2)).assignInviteCode(eq(courseId), any(String.class));
  }

  @Test
  void regenerateInviteCode_whenCourseNotFound_throwsCourseNotFoundException() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> courseService.regenerateInviteCode(courseId, ownerId))
        .isInstanceOf(CourseNotFoundException.class);

    verify(courseInviteCodeHelper, never()).assignInviteCode(any(), any());
  }

  @Test
  void updateCourse_withPublishedAndPrivate_throwsIllegalArgumentException() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = new Course();
    course.setId(courseId);
    course.setPublished(false);
    course.setPrivate(false);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    UpdateCourseRequest updateRequest = new UpdateCourseRequest(
        "Updated title",
        "Updated description",
        "Updated short description",
        true,
        true,
        null,
        null,
        List.of());

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    assertThatThrownBy(() -> courseService.updateCourse(ownerId, courseId, updateRequest))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("published and private");

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void regenerateInviteCode_after10ConstraintViolations_throwsInviteCodeGenerationException() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = new Course();
    course.setId(courseId);
    course.setPublished(false);
    course.setPrivate(true);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    doThrow(new DataIntegrityViolationException("uk_courses_invite_code"))
        .when(courseInviteCodeHelper)
        .assignInviteCode(eq(courseId), any(String.class));

    assertThatThrownBy(() -> courseService.regenerateInviteCode(courseId, ownerId))
        .isInstanceOf(InviteCodeGenerationException.class)
        .hasMessageContaining("10 attempts");

    verify(courseInviteCodeHelper, times(10)).assignInviteCode(eq(courseId), any(String.class));
  }

  @Test
  void regenerateInviteCode_onNonInviteCodeConstraintViolation_rethrowsImmediately() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = new Course();
    course.setId(courseId);
    course.setPublished(false);
    course.setPrivate(true);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    doThrow(new DataIntegrityViolationException("uk_course_enrollment_other_constraint"))
        .when(courseInviteCodeHelper)
        .assignInviteCode(eq(courseId), any(String.class));

    assertThatThrownBy(() -> courseService.regenerateInviteCode(courseId, ownerId))
        .isInstanceOf(DataIntegrityViolationException.class);

    // Should not retry — only one attempt made
    verify(courseInviteCodeHelper, times(1)).assignInviteCode(eq(courseId), any(String.class));
  }

  @Test
  void createCourse_whenInviteCodeExhausted_throwsInviteCodeGenerationException() {
    UUID ownerId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);
    owner.setRoles(Set.of(UserRoleEnum.ROLE_INSTRUCTOR));

    when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
    when(courseRepository.existsByInviteCode(any(String.class))).thenReturn(true);

    CreateCourseRequest request = new CreateCourseRequest(
        "Private Course",
        "Desc",
        null,
        false,
        true,
        null,
        null,
        List.of());

    assertThatThrownBy(() -> courseService.createCourse(ownerId, request))
        .isInstanceOf(InviteCodeGenerationException.class);
  }

  @Test
  void updateCourse_whenInviteCodeExhausted_throwsInviteCodeGenerationException() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = new Course();
    course.setId(courseId);
    course.setPublished(false);
    course.setPrivate(false);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(courseRepository.existsByInviteCode(any(String.class))).thenReturn(true);

    UpdateCourseRequest updateRequest = new UpdateCourseRequest(
        "Updated title",
        "Updated description",
        null,
        false,
        true,
        null,
        null,
        List.of());

    assertThatThrownBy(() -> courseService.updateCourse(ownerId, courseId, updateRequest))
        .isInstanceOf(InviteCodeGenerationException.class);
  }
}
