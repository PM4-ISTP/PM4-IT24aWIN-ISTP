package com.pm4.istp.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.pm4.istp.course.dto.CourseLabDeadlineDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.pm4.istp.course.db.CreateCourseInstructorRequest;
import com.pm4.istp.course.db.CreateCourseRequest;
import com.pm4.istp.course.db.entities.CourseStatusEnum;
import com.pm4.istp.course.db.entities.McAttemptsMode;
import com.pm4.istp.course.db.InstructorRoleEnum;
import com.pm4.istp.course.db.UpdateCourseInstructorRequest;
import com.pm4.istp.course.db.UpdateCourseRequest;
import com.pm4.istp.course.db.entities.Lab;
import com.pm4.istp.course.db.entities.LabStatusEnum;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.ChallengeOption;
import com.pm4.istp.course.db.entities.ChallengeType;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.db.entities.CourseChallengeScoreOverride;
import com.pm4.istp.course.db.entities.CourseEnrollment;
import com.pm4.istp.course.db.entities.CourseInstructor;
import com.pm4.istp.course.db.entities.CourseLab;
import com.pm4.istp.course.db.entities.StudentFlagSubmission;
import com.pm4.istp.course.db.entities.StudentOptionSubmission;
import com.pm4.istp.course.dto.CourseLabItemDto;
import com.pm4.istp.course.dto.CourseLabSubmissionStatusEnum;
import com.pm4.istp.course.dto.ListCourseResponseDto;
import com.pm4.istp.course.dto.UpdateCourseChallengeScoreRequestDto;
import com.pm4.istp.course.exceptions.ChallengeNotFoundException;
import com.pm4.istp.course.exceptions.LabNotFoundException;
import com.pm4.istp.course.exceptions.CourseAccessDeniedException;
import com.pm4.istp.course.exceptions.CourseNotFoundException;
import com.pm4.istp.course.exceptions.CourseParticipantNotFoundException;
import com.pm4.istp.course.exceptions.InvalidCourseLabException;
import com.pm4.istp.course.exceptions.InvalidInviteCodeException;
import com.pm4.istp.course.exceptions.InviteCodeGenerationException;
import com.pm4.istp.course.repositories.LabRepository;
import com.pm4.istp.course.repositories.CourseEnrollmentRepository;
import com.pm4.istp.course.repositories.CourseRepository;
import com.pm4.istp.course.repositories.ChallengeCompletionRepository;
import com.pm4.istp.course.repositories.ChallengeRepository;
import com.pm4.istp.course.repositories.CourseChallengeScoreOverrideRepository;
import com.pm4.istp.course.repositories.CourseLabRepository;
import com.pm4.istp.course.repositories.StudentFlagSubmissionRepository;
import com.pm4.istp.course.repositories.StudentOptionSubmissionRepository;
import com.pm4.istp.course.services.CourseInviteCodeHelper;
import com.pm4.istp.course.services.CourseTopicService;
import com.pm4.istp.course.services.CourseService;
import com.pm4.istp.badge.services.BadgeService;
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
  private CourseLabRepository courseLabRepository;
  @Mock
  private LabRepository labRepository;
  @Mock
  private ChallengeRepository challengeRepository;
  @Mock
  private ChallengeCompletionRepository challengeCompletionRepository;
  @Mock
  private StudentOptionSubmissionRepository studentOptionSubmissionRepository;
  @Mock
  private StudentFlagSubmissionRepository studentFlagSubmissionRepository;
  @Mock
  private CourseChallengeScoreOverrideRepository courseChallengeScoreOverrideRepository;
  @Mock
  private CourseInviteCodeHelper courseInviteCodeHelper;
  @Mock
  private CourseTopicService courseTopicService;
  @Mock
  private BadgeService badgeService;

  @Test
  void privateCourseHelpers_coverNormalizationVisibilityEnrollmentAndStatusBranches() {
    assertThat((String) ReflectionTestUtils.invokeMethod(courseService, "normalizeShortDescription", "  A   short   text  "))
        .isEqualTo("A short text");
    assertThat((String) ReflectionTestUtils.invokeMethod(courseService, "normalizeShortDescription", "   "))
        .isNull();

    User participant = new User();
    participant.setId(UUID.randomUUID());
    Course course = new Course();
    ReflectionTestUtils.invokeMethod(courseService, "addEnrollmentIfMissing", course, participant);
    ReflectionTestUtils.invokeMethod(courseService, "addEnrollmentIfMissing", course, participant);

    assertThat(course.getCourseEnrollments()).hasSize(1);
    assertThat(
            (CourseLabSubmissionStatusEnum)
                ReflectionTestUtils.invokeMethod(courseService, "resolveSubmissionStatus", 0, 0))
        .isEqualTo(CourseLabSubmissionStatusEnum.NOT_STARTED);
    assertThat(
            (CourseLabSubmissionStatusEnum)
                ReflectionTestUtils.invokeMethod(courseService, "resolveSubmissionStatus", 1, 2))
        .isEqualTo(CourseLabSubmissionStatusEnum.IN_PROGRESS);
    assertThat(
            (CourseLabSubmissionStatusEnum)
                ReflectionTestUtils.invokeMethod(courseService, "resolveSubmissionStatus", 2, 2))
        .isEqualTo(CourseLabSubmissionStatusEnum.SUBMITTED);
  }

  @InjectMocks
  private CourseService courseService;

  @BeforeEach
  void setUp() {
    // Only some tests call create/update (which validate topics). Keep other tests strict.
    lenient()
        .when(courseTopicService.normalizeAndValidate(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

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

    when(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(Optional.of(owner));
    when(userRepository.findByIdAndDeletedAtIsNull(collaboratorId)).thenReturn(Optional.of(collaborator));
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CreateCourseRequest request = new CreateCourseRequest(
        "Secure Coding",
        "Intro",
        "Learn the secure coding basics.",
        CourseStatusEnum.DRAFT,
        null,
        null,
        List.of(new CreateCourseInstructorRequest(collaboratorId, InstructorRoleEnum.COLLABORATOR)),
        McAttemptsMode.UNLIMITED);

    Course result = courseService.createCourse(ownerId, request);

    assertThat(result.getTitle()).isEqualTo("Secure Coding");
    assertThat(result.getShortDescription()).isEqualTo("Learn the secure coding basics.");
    assertThat(result.getCourseInstructors()).hasSize(2);
    assertThat(result.getCourseEnrollments()).hasSize(1);

    CourseEnrollment ownerEnrollment = result.getCourseEnrollments().getFirst();
    assertThat(ownerEnrollment.getParticipant().getId()).isEqualTo(ownerId);
    assertThat(ownerEnrollment.getCourse()).isSameAs(result);

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
  void createCourse_whenPrivate_autoEnrollsOwnerAndUsesInviteCodeHelper() {
    UUID ownerId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);
    owner.setName("Owner");
    owner.setRoles(Set.of(UserRoleEnum.ROLE_INSTRUCTOR));

    when(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(Optional.of(owner));
    when(courseInviteCodeHelper.saveNewCourseWithInviteCode(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CreateCourseRequest request = new CreateCourseRequest(
        "Private Secure Coding",
        "Intro",
        "Private practice course.",
        CourseStatusEnum.PRIVATE,
        null,
        null,
        List.of(),
        McAttemptsMode.UNLIMITED);

    Course result = courseService.createCourse(ownerId, request);

    assertThat(result.getStatus()).isEqualTo(CourseStatusEnum.PRIVATE);
    assertThat(result.getCourseInstructors()).hasSize(1);
    assertThat(result.getCourseEnrollments()).hasSize(1);

    CourseEnrollment ownerEnrollment = result.getCourseEnrollments().getFirst();
    assertThat(ownerEnrollment.getParticipant().getId()).isEqualTo(ownerId);
    assertThat(ownerEnrollment.getCourse()).isSameAs(result);

    verify(courseInviteCodeHelper).saveNewCourseWithInviteCode(any(Course.class));
    verify(courseRepository, never()).save(any(Course.class));
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

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));

    assertThatThrownBy(() -> courseService.getCourse(requesterId, courseId))
        .isInstanceOf(CourseAccessDeniedException.class);
  }

  @Test
  void getCourse_whenPublished_returnsCourseForAnyAuthenticatedUser() {
    UUID requesterId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    Course course = new Course();
    course.setId(courseId);
    course.setStatus(CourseStatusEnum.PUBLIC);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));

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
        CourseStatusEnum.PUBLIC,
        null,
        null,
        List.of(new UpdateCourseInstructorRequest(newCollaboratorId, InstructorRoleEnum.COLLABORATOR)),
        McAttemptsMode.UNLIMITED);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(userRepository.findByIdAndDeletedAtIsNull(newCollaboratorId)).thenReturn(Optional.of(newCollaborator));
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Course updated = courseService.updateCourse(ownerId, courseId, updateRequest);

    assertThat(updated.getTitle()).isEqualTo("Updated title");
    assertThat(updated.getDescription()).isEqualTo("Updated description");
    assertThat(updated.getShortDescription()).isEqualTo("Updated short description for the header.");
    assertThat(updated.getStatus()).isEqualTo(CourseStatusEnum.PUBLIC);

    assertThat(updated.getCourseInstructors())
        .extracting(ci -> ci.getInstructor().getId())
        .contains(ownerId, newCollaboratorId)
        .doesNotContain(oldCollaboratorId);

    assertThat(updated.getCourseInstructors())
        .filteredOn(ci -> ci.getInstructorRole() == InstructorRoleEnum.OWNER)
        .hasSize(1);
  }

  @Test
  void updateCourse_whenCollaboratorUpdatesContentOnly_savesChanges() {
    UUID ownerId = UUID.randomUUID();
    UUID collaboratorId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    User collaborator = new User();
    collaborator.setId(collaboratorId);

    Course course = new Course();
    course.setId(courseId);
    course.setStatus(CourseStatusEnum.DRAFT);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    CourseInstructor collaboratorRelation = new CourseInstructor();
    collaboratorRelation.setInstructorRole(InstructorRoleEnum.COLLABORATOR);
    collaboratorRelation.setInstructor(collaborator);
    course.addCourseInstructor(collaboratorRelation);

    UpdateCourseRequest updateRequest =
        new UpdateCourseRequest(
            "Collaborator update",
            "Updated by collaborator",
            "Updated short description",
            CourseStatusEnum.DRAFT,
            "https://example.com/course.png",
            "Web-Security",
            List.of(
                new UpdateCourseInstructorRequest(
                    collaboratorId, InstructorRoleEnum.COLLABORATOR)),
            McAttemptsMode.ONCE);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Course updated = courseService.updateCourse(collaboratorId, courseId, updateRequest);

    assertThat(updated.getTitle()).isEqualTo("Collaborator update");
    assertThat(updated.getDescription()).isEqualTo("Updated by collaborator");
    assertThat(updated.getImageUrl()).isEqualTo("https://example.com/course.png");
    assertThat(updated.getTopic()).isEqualTo("Web-Security");
    assertThat(updated.getMcAttemptsMode()).isEqualTo(McAttemptsMode.ONCE);
    assertThat(updated.getStatus()).isEqualTo(CourseStatusEnum.DRAFT);

    verify(courseRepository).save(course);
  }

  @Test
  void updateCourse_whenCollaboratorChangesVisibility_throwsCourseAccessDeniedException() {
    UUID ownerId = UUID.randomUUID();
    UUID collaboratorId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    User collaborator = new User();
    collaborator.setId(collaboratorId);

    Course course = new Course();
    course.setId(courseId);
    course.setStatus(CourseStatusEnum.DRAFT);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    CourseInstructor collaboratorRelation = new CourseInstructor();
    collaboratorRelation.setInstructorRole(InstructorRoleEnum.COLLABORATOR);
    collaboratorRelation.setInstructor(collaborator);
    course.addCourseInstructor(collaboratorRelation);

    UpdateCourseRequest updateRequest =
        new UpdateCourseRequest(
            "Collaborator publish",
            "Attempted publish",
            "Updated short description",
            CourseStatusEnum.PUBLIC,
            null,
            null,
            List.of(
                new UpdateCourseInstructorRequest(
                    collaboratorId, InstructorRoleEnum.COLLABORATOR)),
            McAttemptsMode.UNLIMITED);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));

    assertThatThrownBy(() -> courseService.updateCourse(collaboratorId, courseId, updateRequest))
        .isInstanceOf(CourseAccessDeniedException.class)
        .hasMessageContaining("not the owner");

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void updateCourse_whenCollaboratorChangesCollaborators_throwsCourseAccessDeniedException() {
    UUID ownerId = UUID.randomUUID();
    UUID collaboratorId = UUID.randomUUID();
    UUID newCollaboratorId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    User collaborator = new User();
    collaborator.setId(collaboratorId);

    Course course = new Course();
    course.setId(courseId);
    course.setStatus(CourseStatusEnum.DRAFT);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    CourseInstructor collaboratorRelation = new CourseInstructor();
    collaboratorRelation.setInstructorRole(InstructorRoleEnum.COLLABORATOR);
    collaboratorRelation.setInstructor(collaborator);
    course.addCourseInstructor(collaboratorRelation);

    UpdateCourseRequest updateRequest =
        new UpdateCourseRequest(
            "Collaborator update",
            "Attempted collaborator change",
            "Updated short description",
            CourseStatusEnum.DRAFT,
            null,
            null,
            List.of(
                new UpdateCourseInstructorRequest(
                    newCollaboratorId, InstructorRoleEnum.COLLABORATOR)),
            McAttemptsMode.UNLIMITED);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));

    assertThatThrownBy(() -> courseService.updateCourse(collaboratorId, courseId, updateRequest))
        .isInstanceOf(CourseAccessDeniedException.class)
        .hasMessageContaining("not the owner");

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void listPublishedCourses_delegatesToRepositoryWithNormalizedQuery() {
    Pageable pageable = PageRequest.of(0, 12);
    Page<ListCourseResponseDto> expected = new PageImpl<>(List.of());

    when(courseRepository.findPublishedCoursesByQuery("secure", pageable)).thenReturn(expected);

    Page<ListCourseResponseDto> result = courseService.listPublishedCourses("  secure  ", null, pageable);

    assertThat(result).isSameAs(expected);
    verify(courseRepository).findPublishedCoursesByQuery("secure", pageable);
  }

  @Test
  void listPublishedCourses_withBlankQuery_usesNullFilter() {
    Pageable pageable = PageRequest.of(0, 12);
    Page<ListCourseResponseDto> expected = new PageImpl<>(List.of());

    when(courseRepository.findPublishedCourses(pageable)).thenReturn(expected);

    Page<ListCourseResponseDto> result = courseService.listPublishedCourses("   ", null, pageable);

    assertThat(result).isSameAs(expected);
    verify(courseRepository).findPublishedCourses(pageable);
  }

  @Test
  void listPublishedCourses_withTopicOnly_delegatesToTopicRepository() {
    Pageable pageable = PageRequest.of(0, 12);
    Page<ListCourseResponseDto> expected = new PageImpl<>(List.of());

    when(courseRepository.findPublishedCoursesByTopic("Security", pageable)).thenReturn(expected);

    Page<ListCourseResponseDto> result = courseService.listPublishedCourses(null, "Security", pageable);

    assertThat(result).isSameAs(expected);
    verify(courseRepository).findPublishedCoursesByTopic("Security", pageable);
  }

  @Test
  void listPublishedCourses_withQueryAndTopic_delegatesToQueryAndTopicRepository() {
    Pageable pageable = PageRequest.of(0, 12);
    Page<ListCourseResponseDto> expected = new PageImpl<>(List.of());

    when(courseRepository.findPublishedCoursesByQueryAndTopic("sql", "Security", pageable))
        .thenReturn(expected);

    Page<ListCourseResponseDto> result =
        courseService.listPublishedCourses("  sql  ", "  Security  ", pageable);

    assertThat(result).isSameAs(expected);
    verify(courseRepository).findPublishedCoursesByQueryAndTopic("sql", "Security", pageable);
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
    course.setStatus(CourseStatusEnum.PUBLIC);

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(participant));
    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
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
    verify(badgeService).tryAwardBadgeForCourse(userId, courseId);
  }

  @Test
  void enrollInCourse_whenAlreadyEnrolled_returnsCourseWithoutSavingAgain() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User participant = new User();
    participant.setId(userId);

    Course course = new Course();
    course.setId(courseId);
    course.setStatus(CourseStatusEnum.PUBLIC);

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(participant));
    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
        .thenReturn(true);

    Course result = courseService.enrollInCourse(userId, courseId);

    assertThat(result).isSameAs(course);
    verify(courseRepository, never()).save(any(Course.class));
    verifyNoInteractions(badgeService);
  }

  @Test
  void enrollInCourse_whenConcurrentDuplicateInsert_treatsAsAlreadyEnrolled() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User participant = new User();
    participant.setId(userId);

    Course course = new Course();
    course.setId(courseId);
    course.setStatus(CourseStatusEnum.PUBLIC);

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(participant));
    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
        .thenReturn(false);
    when(courseRepository.save(any(Course.class)))
        .thenThrow(new DataIntegrityViolationException("uk_course_enrollment_course_participant"));

    Course result = courseService.enrollInCourse(userId, courseId);

    assertThat(result).isSameAs(course);
    verify(badgeService).tryAwardBadgeForCourse(userId, courseId);
  }

  @Test
  void deleteCourse_whenOwner_softDeletesCourse() {
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

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));

    courseService.deleteCourse(ownerId, courseId);

    verify(courseRepository).save(course);
    assertThat(course.getStatus()).isEqualTo(CourseStatusEnum.SOFT_DELETED);
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

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));

    assertThatThrownBy(() -> courseService.deleteCourse(nonOwnerId, courseId))
        .isInstanceOf(CourseAccessDeniedException.class);

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void deleteCourse_whenCourseNotFound_throwsCourseNotFoundException() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> courseService.deleteCourse(userId, courseId))
        .isInstanceOf(CourseNotFoundException.class);

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void removeParticipant_whenNotOwner_throwsCourseAccessDeniedException() {
    UUID ownerId = UUID.randomUUID();
    UUID nonOwnerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = new Course();
    course.setId(courseId);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));

    assertThatThrownBy(() -> courseService.removeParticipant(nonOwnerId, courseId, participantId))
        .isInstanceOf(CourseAccessDeniedException.class);

    verify(courseEnrollmentRepository, never()).findByCourseIdAndParticipantId(any(), any());
    verify(courseEnrollmentRepository, never()).delete(any(CourseEnrollment.class));
  }

  @Test
  void removeParticipant_whenEnrollmentMissing_throwsCourseParticipantNotFoundException() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = new Course();
    course.setId(courseId);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(courseEnrollmentRepository.findByCourseIdAndParticipantId(courseId, participantId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> courseService.removeParticipant(ownerId, courseId, participantId))
        .isInstanceOf(CourseParticipantNotFoundException.class);

    verify(courseEnrollmentRepository, never()).delete(any(CourseEnrollment.class));
  }

  @Test
  void removeParticipant_whenOwnerAndEnrolled_deletesEnrollment() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    UUID enrollmentId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = new Course();
    course.setId(courseId);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    CourseEnrollment enrollment = new CourseEnrollment();
    enrollment.setId(enrollmentId);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(courseEnrollmentRepository.findByCourseIdAndParticipantId(courseId, participantId))
        .thenReturn(Optional.of(enrollment));

    courseService.removeParticipant(ownerId, courseId, participantId);

    verify(courseRepository).save(course);
    verify(courseEnrollmentRepository, never()).delete(any(CourseEnrollment.class));
  }

  @Test
  void createCourse_withoutCollaborators_createsOnlyOwnerRelation() {
    UUID ownerId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);
    owner.setName("Owner");
    owner.setRoles(Set.of(UserRoleEnum.ROLE_INSTRUCTOR));

    when(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(Optional.of(owner));
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CreateCourseRequest request = new CreateCourseRequest(
        "Solo Course",
        "Desc",
        "Short solo summary.",
        CourseStatusEnum.DRAFT,
        null,
        null,
        List.of(),
        McAttemptsMode.UNLIMITED);

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

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));

    Course result = courseService.getCourse(userId, courseId);

    assertThat(result).isSameAs(course);
  }

  @Test
  void getCourse_whenCourseNotFound_throwsCourseNotFoundException() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.empty());

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
        "Title",
        "Desc",
        "Short summary.",
        CourseStatusEnum.DRAFT,
        null,
        null,
        List.of(),
        McAttemptsMode.UNLIMITED);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));

    assertThatThrownBy(() -> courseService.updateCourse(outsiderId, courseId, request))
        .isInstanceOf(CourseAccessDeniedException.class);

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void updateCourse_whenCourseNotFound_throwsCourseNotFoundException() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.empty());

    UpdateCourseRequest request = new UpdateCourseRequest(
        "Title",
        "Desc",
        "Short summary.",
        CourseStatusEnum.DRAFT,
        null,
        null,
        List.of(),
        McAttemptsMode.UNLIMITED);

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

  private Lab buildLab(UUID id, User creator, LabStatusEnum status) {
    Lab lab = new Lab();
    lab.setId(id);
    lab.setTitle("Lab " + id);
    lab.setStatus(status);
    lab.setCreator(creator);
    return lab;
  }

  private Challenge buildCourseChallenge(
      UUID id, Lab lab, String title, ChallengeType type, int points) {
    Challenge challenge = new Challenge();
    challenge.setId(id);
    challenge.setLab(lab);
    challenge.setTitle(title);
    challenge.setType(type);
    challenge.setPoints(points);
    return challenge;
  }

  @Test
  void updateCourseLabs_replacesAssignmentsWithOwnPrivateLab() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = buildCourseWithOwner(courseId, owner);
    Lab lab = buildLab(labId, owner, LabStatusEnum.PRIVATE);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Course updated = courseService.updateCourseLabs(
        ownerId, courseId, List.of(new CourseLabItemDto(labId, 0, null)));

    assertThat(updated.getCourseLabs()).hasSize(1);
    assertThat(updated.getCourseLabs().getFirst().getLab()).isSameAs(lab);
    assertThat(updated.getCourseLabs().getFirst().getOrderIndex()).isZero();
    verify(courseRepository).save(course);
  }

  @Test
  void updateCourseLabs_setsDueAtWhenProvided() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    LocalDateTime dueAt = LocalDateTime.of(2026, 5, 1, 12, 0);

    User owner = new User();
    owner.setId(ownerId);

    Course course = buildCourseWithOwner(courseId, owner);
    Lab lab = buildLab(labId, owner, LabStatusEnum.PUBLIC);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));
    when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Course updated =
        courseService.updateCourseLabs(
            ownerId, courseId, List.of(new CourseLabItemDto(labId, 0, dueAt)));

    assertThat(updated.getCourseLabs()).hasSize(1);
    assertThat(updated.getCourseLabs().getFirst().getDueAt()).isEqualTo(dueAt);
  }

  @Test
  void updateCourseLabs_diffsExistingAssignments() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID keptLabId = UUID.randomUUID();
    UUID removedLabId = UUID.randomUUID();
    UUID addedLabId = UUID.randomUUID();
    LocalDateTime dueAt = LocalDateTime.of(2026, 5, 2, 12, 0);

    User owner = new User();
    owner.setId(ownerId);

    Course course = buildCourseWithOwner(courseId, owner);
    Lab keptLab = buildLab(keptLabId, owner, LabStatusEnum.PUBLIC);
    Lab removedLab = buildLab(removedLabId, owner, LabStatusEnum.PUBLIC);
    Lab addedLab = buildLab(addedLabId, owner, LabStatusEnum.PUBLIC);

    CourseLab keptAssignment = new CourseLab();
    keptAssignment.setLab(keptLab);
    keptAssignment.setOrderIndex(0);
    course.addCourseLab(keptAssignment);

    CourseLab removedAssignment = new CourseLab();
    removedAssignment.setLab(removedLab);
    removedAssignment.setOrderIndex(1);
    course.addCourseLab(removedAssignment);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(labRepository.findById(keptLabId)).thenReturn(Optional.of(keptLab));
    when(labRepository.findById(addedLabId)).thenReturn(Optional.of(addedLab));
    when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Course updated =
        courseService.updateCourseLabs(
            ownerId,
            courseId,
            List.of(new CourseLabItemDto(addedLabId, 0, null), new CourseLabItemDto(keptLabId, 1, dueAt)));

    assertThat(updated.getCourseLabs()).hasSize(2);
    assertThat(updated.getCourseLabs()).extracting(courseLab -> courseLab.getLab().getId())
        .containsExactly(addedLabId, keptLabId);
    assertThat(updated.getCourseLabs().get(1)).isSameAs(keptAssignment);
    assertThat(keptAssignment.getOrderIndex()).isEqualTo(1);
    assertThat(keptAssignment.getDueAt()).isEqualTo(dueAt);
    assertThat(removedAssignment.getCourse()).isNull();
    verify(labRepository, never()).findById(removedLabId);
    verify(courseRepository).save(course);
  }

  @Test
  void updateCourseLabs_allowsPublicLabFromOtherCreator() {
    UUID ownerId = UUID.randomUUID();
    UUID otherCreatorId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);
    User otherCreator = new User();
    otherCreator.setId(otherCreatorId);

    Course course = buildCourseWithOwner(courseId, owner);
    Lab lab = buildLab(labId, otherCreator, LabStatusEnum.PUBLIC);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Course updated = courseService.updateCourseLabs(
        ownerId, courseId, List.of(new CourseLabItemDto(labId, 0, null)));

    assertThat(updated.getCourseLabs()).hasSize(1);
  }

  @Test
  void updateCourseLabs_rejectsDraftLabEvenFromOwnCreator() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = buildCourseWithOwner(courseId, owner);
    Lab lab = buildLab(labId, owner, LabStatusEnum.DRAFT);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));

    List<CourseLabItemDto> items = List.of(new CourseLabItemDto(labId, 0, null));

    assertThatThrownBy(
        () -> courseService.updateCourseLabs(ownerId, courseId, items))
        .isInstanceOf(InvalidCourseLabException.class)
        .hasMessageContaining("draft");

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void updateCourseLabs_rejectsRemovedLabEvenFromOwnCreator() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = buildCourseWithOwner(courseId, owner);
    Lab lab = buildLab(labId, owner, LabStatusEnum.PUBLIC);
    lab.setDeletedAt(LocalDateTime.now());

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));

    List<CourseLabItemDto> items = List.of(new CourseLabItemDto(labId, 0, null));

    assertThatThrownBy(() -> courseService.updateCourseLabs(ownerId, courseId, items))
        .isInstanceOf(InvalidCourseLabException.class)
        .hasMessageContaining("removed");

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void updateCourseLabs_rejectsPrivateLabFromOtherCreator() {
    UUID ownerId = UUID.randomUUID();
    UUID otherCreatorId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);
    User otherCreator = new User();
    otherCreator.setId(otherCreatorId);

    Course course = buildCourseWithOwner(courseId, owner);
    Lab lab = buildLab(labId, otherCreator, LabStatusEnum.PRIVATE);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));

    List<CourseLabItemDto> items = List.of(new CourseLabItemDto(labId, 0, null));

    assertThatThrownBy(
        () -> courseService.updateCourseLabs(ownerId, courseId, items))
        .isInstanceOf(LabNotFoundException.class);

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void updateCourseLabs_whenLabDoesNotExist_throwsLabNotFound() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = buildCourseWithOwner(courseId, owner);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(labRepository.findById(labId)).thenReturn(Optional.empty());

    List<CourseLabItemDto> items = List.of(new CourseLabItemDto(labId, 0, null));

    assertThatThrownBy(
        () -> courseService.updateCourseLabs(ownerId, courseId, items))
        .isInstanceOf(LabNotFoundException.class);

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void updateCourseLabs_whenCallerIsNotInstructor_throwsAccessDenied() {
    UUID ownerId = UUID.randomUUID();
    UUID outsiderId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = buildCourseWithOwner(courseId, owner);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));

    List<CourseLabItemDto> items = List.of();

    assertThatThrownBy(() -> courseService.updateCourseLabs(outsiderId, courseId, items))
        .isInstanceOf(CourseAccessDeniedException.class);

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void updateCourseLabs_whenCourseNotFound_throwsCourseNotFound() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.empty());

    List<CourseLabItemDto> items = List.of();

    assertThatThrownBy(() -> courseService.updateCourseLabs(ownerId, courseId, items))
        .isInstanceOf(CourseNotFoundException.class);
  }

  @Test
  void updateCourseLabs_withEmptyList_clearsAllAssignments() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = buildCourseWithOwner(courseId, owner);
    // pre-seed with an existing assignment to verify it gets cleared
    Lab existing = buildLab(UUID.randomUUID(), owner, LabStatusEnum.PUBLIC);
    com.pm4.istp.course.db.entities.CourseLab existingAssignment = new com.pm4.istp.course.db.entities.CourseLab();
    existingAssignment.setLab(existing);
    existingAssignment.setOrderIndex(0);
    course.addCourseLab(existingAssignment);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Course updated = courseService.updateCourseLabs(ownerId, courseId, List.of());

    assertThat(updated.getCourseLabs()).isEmpty();
  }

  @Test
  void getCourseLabSubmissions_returnsOnTimeInProgressAndNotSubmitted() {
    UUID instructorId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();

    User instructor = new User();
    instructor.setId(instructorId);

    Course course = buildCourseWithOwner(courseId, instructor);
    Lab lab = buildLab(labId, instructor, LabStatusEnum.PUBLIC);

    com.pm4.istp.course.db.entities.CourseLab assignment =
        new com.pm4.istp.course.db.entities.CourseLab();
    assignment.setLab(lab);
    assignment.setOrderIndex(0);
    LocalDateTime dueAt = LocalDateTime.of(2026, 5, 1, 12, 0);
    assignment.setDueAt(dueAt);
    course.addCourseLab(assignment);

    User studentOnTime = new User();
    studentOnTime.setId(UUID.randomUUID());
    studentOnTime.setName("On Time");
    User studentLate = new User();
    studentLate.setId(UUID.randomUUID());
    studentLate.setName("Late");
    User studentInProgress = new User();
    studentInProgress.setId(UUID.randomUUID());
    studentInProgress.setName("In Progress");
    User studentNotSubmitted = new User();
    studentNotSubmitted.setId(UUID.randomUUID());
    studentNotSubmitted.setName("Not Submitted");

    CourseEnrollment e1 = new CourseEnrollment();
    e1.setCourse(course);
    e1.setParticipant(studentOnTime);
    CourseEnrollment e2 = new CourseEnrollment();
    e2.setCourse(course);
    e2.setParticipant(studentLate);
    CourseEnrollment e3 = new CourseEnrollment();
    e3.setCourse(course);
    e3.setParticipant(studentInProgress);
    CourseEnrollment e4 = new CourseEnrollment();
    e4.setCourse(course);
    e4.setParticipant(studentNotSubmitted);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(courseEnrollmentRepository.findByCourseIdFetchParticipant(courseId))
        .thenReturn(List.of(e1, e2, e3, e4));
    when(challengeRepository.countByLabIds(List.of(labId)))
        .thenReturn(List.<Object[]>of(new Object[] {labId, 3L}));

    when(
            challengeCompletionRepository.aggregateSolvedCountsForUsersAndLabs(
                any(), any()))
        .thenReturn(
            List.of(
                new Object[] {studentOnTime.getId(), labId, 3L, dueAt.minusMinutes(5)},
                new Object[] {studentLate.getId(), labId, 3L, dueAt.plusMinutes(1)},
                new Object[] {studentInProgress.getId(), labId, 2L, dueAt.minusMinutes(2)}));

    var result = courseService.getCourseLabSubmissions(instructorId, courseId);

    assertThat(result.getLabs()).hasSize(1);
    assertThat(result.getLabs().getFirst().getDueAt()).isEqualTo(dueAt);
    assertThat(result.getParticipants()).hasSize(4);

    var byStudent =
        result.getSubmissions().stream()
            .collect(java.util.stream.Collectors.toMap(s -> s.getParticipantId(), s -> s));

    assertThat(byStudent.get(studentOnTime.getId()).getStatus().name()).isEqualTo("SUBMITTED");
    // Late submissions are no longer a concept. If someone completed after the due date, it's still SUBMITTED.
    assertThat(byStudent.get(studentLate.getId()).getStatus().name()).isEqualTo("SUBMITTED");
    assertThat(byStudent.get(studentInProgress.getId()).getStatus().name()).isEqualTo("IN_PROGRESS");
    assertThat(byStudent.get(studentNotSubmitted.getId()).getStatus().name())
        .isEqualTo("NOT_STARTED");
  }

  // ── joinByInviteCode ───────────────────────────────────────────────────────

  @Test
  void getCourseLabSubmissionDetails_combinesSolvedEvidenceAndOverrides() {
    UUID instructorId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID optionChallengeId = UUID.randomUUID();
    UUID flagChallengeId = UUID.randomUUID();
    LocalDateTime dueAt = LocalDateTime.of(2026, 5, 12, 16, 30);

    User instructor = new User();
    instructor.setId(instructorId);
    Course course = buildCourseWithOwner(courseId, instructor);
    Lab lab = buildLab(labId, instructor, LabStatusEnum.PUBLIC);
    lab.setTitle("Evidence lab");
    lab.setMaxScore(8);
    CourseLab assignment = new CourseLab();
    assignment.setLab(lab);
    assignment.setDueAt(dueAt);
    course.addCourseLab(assignment);

    Challenge optionChallenge =
        buildCourseChallenge(optionChallengeId, lab, "Choose wisely", ChallengeType.MULTIPLE_CHOICE, 5);
    Challenge flagChallenge =
        buildCourseChallenge(flagChallengeId, lab, "Capture flag", ChallengeType.FLAG, 3);

    ChallengeOption selectedOption = new ChallengeOption();
    selectedOption.setText("Sanitize input");
    StudentOptionSubmission optionSubmission = new StudentOptionSubmission();
    optionSubmission.setSelectedOption(selectedOption);
    optionSubmission.setCorrect(true);
    StudentFlagSubmission flagSubmission = new StudentFlagSubmission();
    flagSubmission.setSubmittedFlag("pm4{almost}");
    flagSubmission.setCorrect(false);

    List<UUID> challengeIds = List.of(optionChallengeId, flagChallengeId);
    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, participantId))
        .thenReturn(true);
    when(challengeRepository.findByLabIdOrderByOrderIndexAsc(labId))
        .thenReturn(List.of(optionChallenge, flagChallenge));
    when(challengeCompletionRepository.findSolvedChallengeIds(participantId, challengeIds))
        .thenReturn(List.of(optionChallengeId));
    when(studentOptionSubmissionRepository.findByUserIdAndChallengeId(participantId, optionChallengeId))
        .thenReturn(Optional.of(optionSubmission));
    when(studentOptionSubmissionRepository.findByUserIdAndChallengeId(participantId, flagChallengeId))
        .thenReturn(Optional.empty());
    when(studentFlagSubmissionRepository.findByUserIdAndChallengeId(participantId, optionChallengeId))
        .thenReturn(Optional.empty());
    when(studentFlagSubmissionRepository.findByUserIdAndChallengeId(participantId, flagChallengeId))
        .thenReturn(Optional.of(flagSubmission));
    when(
            courseChallengeScoreOverrideRepository.findPointsForCourseParticipantsAndChallenges(
                courseId, List.of(participantId), challengeIds))
        .thenReturn(
            List.of(
                new Object[] {participantId, flagChallengeId, 2},
                new Object[] {UUID.randomUUID(), optionChallengeId, 99},
                new Object[] {participantId, optionChallengeId, null}));

    var detail =
        courseService.getCourseLabSubmissionDetails(
            instructorId, courseId, participantId, labId);

    assertThat(detail.getStatus()).isEqualTo(CourseLabSubmissionStatusEnum.IN_PROGRESS);
    assertThat(detail.getCompletedAt()).isNull();
    assertThat(detail.getAwardedPoints()).isEqualTo(7);
    assertThat(detail.getMaxPoints()).isEqualTo(8);
    assertThat(detail.getDueAt()).isEqualTo(dueAt);
    assertThat(detail.getChallenges())
        .extracting(challenge -> challenge.getChallengeId())
        .containsExactly(optionChallengeId, flagChallengeId);
    assertThat(detail.getChallenges().get(0).getSelectedOptionText()).isEqualTo("Sanitize input");
    assertThat(detail.getChallenges().get(0).getCorrect()).isTrue();
    assertThat(detail.getChallenges().get(0).getAwardedPoints()).isEqualTo(5);
    assertThat(detail.getChallenges().get(1).getSubmittedFlag()).isEqualTo("pm4{almost}");
    assertThat(detail.getChallenges().get(1).getCorrect()).isFalse();
    assertThat(detail.getChallenges().get(1).getOverridePoints()).isEqualTo(2);
  }

  @Test
  void getCourseLabSubmissionDetails_whenAllChallengesSolved_setsCompletedAt() {
    UUID instructorId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    LocalDateTime completedAt = LocalDateTime.of(2026, 5, 12, 17, 45);

    User instructor = new User();
    instructor.setId(instructorId);
    Course course = buildCourseWithOwner(courseId, instructor);
    Lab lab = buildLab(labId, instructor, LabStatusEnum.PUBLIC);
    CourseLab assignment = new CourseLab();
    assignment.setLab(lab);
    course.addCourseLab(assignment);
    Challenge challenge = buildCourseChallenge(challengeId, lab, "Solved", ChallengeType.FLAG, 1);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, participantId))
        .thenReturn(true);
    when(challengeRepository.findByLabIdOrderByOrderIndexAsc(labId)).thenReturn(List.of(challenge));
    when(challengeCompletionRepository.findSolvedChallengeIds(participantId, List.of(challengeId)))
        .thenReturn(List.of(challengeId));
    when(studentOptionSubmissionRepository.findByUserIdAndChallengeId(participantId, challengeId))
        .thenReturn(Optional.empty());
    when(studentFlagSubmissionRepository.findByUserIdAndChallengeId(participantId, challengeId))
        .thenReturn(Optional.empty());
    when(
            courseChallengeScoreOverrideRepository.findPointsForCourseParticipantsAndChallenges(
                courseId, List.of(participantId), List.of(challengeId)))
        .thenReturn(List.of());
    when(
            challengeCompletionRepository.aggregateSolvedCountsForUsersAndLabs(
                List.of(participantId), List.of(labId)))
        .thenReturn(List.<Object[]>of(new Object[] {participantId, labId, 1L, completedAt}));

    var detail =
        courseService.getCourseLabSubmissionDetails(
            instructorId, courseId, participantId, labId);

    assertThat(detail.getStatus()).isEqualTo(CourseLabSubmissionStatusEnum.SUBMITTED);
    assertThat(detail.getCompletedAt()).isEqualTo(completedAt);
    assertThat(detail.getChallenges().getFirst().getCorrect()).isTrue();
  }

  @Test
  void getCourseLabSubmissionDetails_whenMultipleChoiceCompletedButWrong_awardsZeroPoints() {
    UUID instructorId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID optionChallengeId = UUID.randomUUID();
    LocalDateTime completedAt = LocalDateTime.of(2026, 5, 12, 19, 15);

    User instructor = new User();
    instructor.setId(instructorId);
    Course course = buildCourseWithOwner(courseId, instructor);

    Lab lab = buildLab(labId, instructor, LabStatusEnum.PUBLIC);
    lab.setMaxScore(5);
    CourseLab assignment = new CourseLab();
    assignment.setLab(lab);
    course.addCourseLab(assignment);

    Challenge optionChallenge =
        buildCourseChallenge(
            optionChallengeId, lab, "Pick one", ChallengeType.MULTIPLE_CHOICE, 5);

    StudentOptionSubmission optionSubmission = new StudentOptionSubmission();
    optionSubmission.setCorrect(false);
    ChallengeOption selected = new ChallengeOption();
    selected.setText("Wrong answer");
    optionSubmission.setSelectedOption(selected);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, participantId))
        .thenReturn(true);
    when(challengeRepository.findByLabIdOrderByOrderIndexAsc(labId)).thenReturn(List.of(optionChallenge));
    when(challengeCompletionRepository.findSolvedChallengeIds(participantId, List.of(optionChallengeId)))
        .thenReturn(List.of(optionChallengeId));
    when(studentOptionSubmissionRepository.findByUserIdAndChallengeId(participantId, optionChallengeId))
        .thenReturn(Optional.of(optionSubmission));
    when(studentFlagSubmissionRepository.findByUserIdAndChallengeId(participantId, optionChallengeId))
        .thenReturn(Optional.empty());
    when(
            courseChallengeScoreOverrideRepository.findPointsForCourseParticipantsAndChallenges(
                courseId, List.of(participantId), List.of(optionChallengeId)))
        .thenReturn(List.of());
    when(
            challengeCompletionRepository.aggregateSolvedCountsForUsersAndLabs(
                List.of(participantId), List.of(labId)))
        .thenReturn(List.<Object[]>of(new Object[] {participantId, labId, 1L, completedAt}));

    var detail =
        courseService.getCourseLabSubmissionDetails(instructorId, courseId, participantId, labId);

    assertThat(detail.getStatus()).isEqualTo(CourseLabSubmissionStatusEnum.SUBMITTED);
    assertThat(detail.getAwardedPoints()).isZero();
    assertThat(detail.getChallenges()).singleElement().satisfies(challenge -> {
      assertThat(challenge.getCorrect()).isFalse();
      assertThat(challenge.getAwardedPoints()).isZero();
      assertThat(challenge.getSelectedOptionText()).isEqualTo("Wrong answer");
    });
  }

  @Test
  void getCourseLabSubmissionDetails_whenMultipleChoiceCompletedButNoSubmission_inUnlimited_awardsFullPoints() {
    UUID instructorId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID optionChallengeId = UUID.randomUUID();
    LocalDateTime completedAt = LocalDateTime.of(2026, 5, 12, 19, 15);

    User instructor = new User();
    instructor.setId(instructorId);
    Course course = buildCourseWithOwner(courseId, instructor);
    course.setMcAttemptsMode(McAttemptsMode.UNLIMITED);

    Lab lab = buildLab(labId, instructor, LabStatusEnum.PUBLIC);
    lab.setMaxScore(2);
    CourseLab assignment = new CourseLab();
    assignment.setLab(lab);
    course.addCourseLab(assignment);

    Challenge optionChallenge =
        buildCourseChallenge(
            optionChallengeId, lab, "Pick one", ChallengeType.MULTIPLE_CHOICE, 2);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, participantId))
        .thenReturn(true);
    when(challengeRepository.findByLabIdOrderByOrderIndexAsc(labId)).thenReturn(List.of(optionChallenge));
    when(challengeCompletionRepository.findSolvedChallengeIds(participantId, List.of(optionChallengeId)))
        .thenReturn(List.of(optionChallengeId));
    when(studentOptionSubmissionRepository.findByUserIdAndChallengeId(participantId, optionChallengeId))
        .thenReturn(Optional.empty());
    when(studentFlagSubmissionRepository.findByUserIdAndChallengeId(participantId, optionChallengeId))
        .thenReturn(Optional.empty());
    when(
            courseChallengeScoreOverrideRepository.findPointsForCourseParticipantsAndChallenges(
                courseId, List.of(participantId), List.of(optionChallengeId)))
        .thenReturn(List.of());
    when(
            challengeCompletionRepository.aggregateSolvedCountsForUsersAndLabs(
                List.of(participantId), List.of(labId)))
        .thenReturn(List.<Object[]>of(new Object[] {participantId, labId, 1L, completedAt}));

    var detail =
        courseService.getCourseLabSubmissionDetails(instructorId, courseId, participantId, labId);

    assertThat(detail.getStatus()).isEqualTo(CourseLabSubmissionStatusEnum.SUBMITTED);
    assertThat(detail.getAwardedPoints()).isEqualTo(2);
    assertThat(detail.getChallenges()).singleElement().satisfies(challenge -> {
      assertThat(challenge.getCorrect()).isTrue();
      assertThat(challenge.getAwardedPoints()).isEqualTo(2);
      assertThat(challenge.getSelectedOptionText()).isNull();
    });
  }

  @Test
  void getCourseLabSubmissionDetails_whenMultipleChoiceCompletedButNoSubmission_inOnce_awardsZeroPoints() {
    UUID instructorId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID optionChallengeId = UUID.randomUUID();
    LocalDateTime completedAt = LocalDateTime.of(2026, 5, 12, 19, 15);

    User instructor = new User();
    instructor.setId(instructorId);
    Course course = buildCourseWithOwner(courseId, instructor);
    course.setMcAttemptsMode(McAttemptsMode.ONCE);

    Lab lab = buildLab(labId, instructor, LabStatusEnum.PUBLIC);
    lab.setMaxScore(2);
    CourseLab assignment = new CourseLab();
    assignment.setLab(lab);
    course.addCourseLab(assignment);

    Challenge optionChallenge =
        buildCourseChallenge(
            optionChallengeId, lab, "Pick one", ChallengeType.MULTIPLE_CHOICE, 2);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, participantId))
        .thenReturn(true);
    when(challengeRepository.findByLabIdOrderByOrderIndexAsc(labId)).thenReturn(List.of(optionChallenge));
    when(challengeCompletionRepository.findSolvedChallengeIds(participantId, List.of(optionChallengeId)))
        .thenReturn(List.of(optionChallengeId));
    when(studentOptionSubmissionRepository.findByUserIdAndChallengeId(participantId, optionChallengeId))
        .thenReturn(Optional.empty());
    when(studentFlagSubmissionRepository.findByUserIdAndChallengeId(participantId, optionChallengeId))
        .thenReturn(Optional.empty());
    when(
            courseChallengeScoreOverrideRepository.findPointsForCourseParticipantsAndChallenges(
                courseId, List.of(participantId), List.of(optionChallengeId)))
        .thenReturn(List.of());
    when(
            challengeCompletionRepository.aggregateSolvedCountsForUsersAndLabs(
                List.of(participantId), List.of(labId)))
        .thenReturn(List.<Object[]>of(new Object[] {participantId, labId, 1L, completedAt}));

    var detail =
        courseService.getCourseLabSubmissionDetails(instructorId, courseId, participantId, labId);

    assertThat(detail.getStatus()).isEqualTo(CourseLabSubmissionStatusEnum.SUBMITTED);
    assertThat(detail.getAwardedPoints()).isZero();
    assertThat(detail.getChallenges()).singleElement().satisfies(challenge -> {
      assertThat(challenge.getCorrect()).isNull();
      assertThat(challenge.getAwardedPoints()).isZero();
    });
  }

  @Test
  void getCourseLabSubmissionDetails_whenLabNotAssigned_throwsLabNotFound() {
    UUID instructorId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();

    User instructor = new User();
    instructor.setId(instructorId);
    Course course = buildCourseWithOwner(courseId, instructor);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, participantId))
        .thenReturn(true);

    assertThatThrownBy(
            () ->
                courseService.getCourseLabSubmissionDetails(
                    instructorId, courseId, participantId, labId))
        .isInstanceOf(LabNotFoundException.class);
  }

  @Test
  void updateCourseChallengeScore_createsOverrideAndReturnsRefreshedEntry() {
    UUID instructorId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID solvedChallengeId = UUID.randomUUID();
    LocalDateTime completedAt = LocalDateTime.of(2026, 5, 12, 18, 0);

    User instructor = new User();
    instructor.setId(instructorId);
    User participant = new User();
    participant.setId(participantId);
    Course course = buildCourseWithOwner(courseId, instructor);
    Lab lab = buildLab(labId, instructor, LabStatusEnum.PUBLIC);
    lab.setMaxScore(8);
    CourseLab assignment = new CourseLab();
    assignment.setLab(lab);
    course.addCourseLab(assignment);

    Challenge overridden =
        buildCourseChallenge(challengeId, lab, "Manual review", ChallengeType.FLAG, 5);
    Challenge solved =
        buildCourseChallenge(solvedChallengeId, lab, "Auto solved", ChallengeType.FLAG, 3);
    List<UUID> challengeIds = List.of(challengeId, solvedChallengeId);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, participantId))
        .thenReturn(true);
    when(userRepository.findByIdAndDeletedAtIsNull(instructorId)).thenReturn(Optional.of(instructor));
    when(userRepository.findByIdAndDeletedAtIsNull(participantId)).thenReturn(Optional.of(participant));
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(overridden));
    when(
            courseChallengeScoreOverrideRepository
                .findByCourseIdAndParticipantIdAndChallengeId(courseId, participantId, challengeId))
        .thenReturn(Optional.empty());
    when(courseChallengeScoreOverrideRepository.save(any(CourseChallengeScoreOverride.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(challengeRepository.countByLabIds(List.of(labId)))
        .thenReturn(List.<Object[]>of(new Object[] {labId, 2L}));
    when(
            challengeCompletionRepository.aggregateSolvedCountsForUsersAndLabs(
                List.of(participantId), List.of(labId)))
        .thenReturn(List.<Object[]>of(new Object[] {participantId, labId, 2L, completedAt}));
    when(challengeRepository.findByLabIdsOrderByLabIdAndOrderIndexAsc(List.of(labId)))
        .thenReturn(List.of(overridden, solved));
    when(challengeCompletionRepository.findSolvedChallengePairs(List.of(participantId), challengeIds))
        .thenReturn(List.<Object[]>of(new Object[] {participantId, solvedChallengeId}));
    when(
            courseChallengeScoreOverrideRepository.findPointsForCourseParticipantsAndChallenges(
                courseId, List.of(participantId), challengeIds))
        .thenReturn(List.<Object[]>of(new Object[] {participantId, challengeId, 4}));

    var entry =
        courseService.updateCourseChallengeScore(
            instructorId,
            courseId,
            participantId,
            challengeId,
            new UpdateCourseChallengeScoreRequestDto(4));

    assertThat(entry.getStatus()).isEqualTo(CourseLabSubmissionStatusEnum.SUBMITTED);
    assertThat(entry.getAwardedPoints()).isEqualTo(7);
    assertThat(entry.getMaxPoints()).isEqualTo(8);
    assertThat(entry.getCompletedAt()).isEqualTo(completedAt);
    verify(courseChallengeScoreOverrideRepository).save(any(CourseChallengeScoreOverride.class));
  }

  @Test
  void updateCourseChallengeScore_whenChallengeNotInCourse_throwsChallengeNotFound() {
    UUID instructorId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();

    User instructor = new User();
    instructor.setId(instructorId);
    User participant = new User();
    participant.setId(participantId);
    Course course = buildCourseWithOwner(courseId, instructor);
    Lab lab = buildLab(labId, instructor, LabStatusEnum.PUBLIC);
    Challenge challenge =
        buildCourseChallenge(challengeId, lab, "Detached challenge", ChallengeType.FLAG, 5);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, participantId))
        .thenReturn(true);
    when(userRepository.findByIdAndDeletedAtIsNull(instructorId)).thenReturn(Optional.of(instructor));
    when(userRepository.findByIdAndDeletedAtIsNull(participantId)).thenReturn(Optional.of(participant));
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

    UpdateCourseChallengeScoreRequestDto request = new UpdateCourseChallengeScoreRequestDto(1);

    assertThatThrownBy(
            () ->
                courseService.updateCourseChallengeScore(
                    instructorId, courseId, participantId, challengeId, request))
        .isInstanceOf(ChallengeNotFoundException.class)
        .hasMessageContaining("not part of this course");
  }

  @Test
  void listUpcomingDeadlines_filtersSubmittedAndIncompleteRows() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID submittedLabId = UUID.randomUUID();
    UUID openLabId = UUID.randomUUID();
    LocalDateTime dueAt = LocalDateTime.of(2026, 5, 13, 8, 0);

    when(courseLabRepository.findDeadlinesForUser(userId))
        .thenReturn(
            List.of(
                new Object[] {courseId, "Course", submittedLabId, "Submitted Lab", dueAt},
                new Object[] {courseId, "Course", openLabId, "Open Lab", dueAt},
                new Object[] {null, "Missing course", openLabId, "Lab", dueAt.plusHours(2)},
                new Object[] {courseId, "Missing due date", openLabId, "Lab", null}));

    when(challengeRepository.countByLabIds(List.of(submittedLabId, openLabId)))
        .thenReturn(
            List.of(
                new Object[] {submittedLabId, 3L},
                new Object[] {openLabId, 3L}));
    when(challengeCompletionRepository.aggregateSolvedCountsForUsersAndLabs(
            List.of(userId), List.of(submittedLabId, openLabId)))
        .thenReturn(
            List.of(
                new Object[] {userId, submittedLabId, 3L, dueAt.minusMinutes(5)},
                new Object[] {userId, openLabId, 2L, dueAt.minusMinutes(2)}));

    var deadlines = courseService.listUpcomingDeadlines(userId);

    assertThat(deadlines).hasSize(1);
    assertThat(deadlines.getFirst().getCourseId()).isEqualTo(courseId);
    assertThat(deadlines.getFirst().getCourseTitle()).isEqualTo("Course");
    assertThat(deadlines.getFirst().getLabId()).isEqualTo(openLabId);
    assertThat(deadlines.getFirst().getLabTitle()).isEqualTo("Open Lab");
    assertThat(deadlines.getFirst().getDueAt()).isEqualTo(dueAt);
  }

  @Test
  void listUpcomingDeadlines_multipleCoursesWithSameLab() {
    UUID userId = UUID.randomUUID();
    UUID[] courseIds = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
    UUID labId = UUID.randomUUID();
    String labTitle = "Lab (in 3 different courses)";
    LocalDateTime dueAt = LocalDateTime.of(2026, 5, 13, 8, 0);

    when(courseLabRepository.findDeadlinesForUser(userId))
        .thenReturn(
            List.of(
                new Object[] {courseIds[0], "Course 1", labId, labTitle, dueAt},
                new Object[] {courseIds[1], "Course 2", labId, labTitle, dueAt},
                new Object[] {courseIds[2], "Course 3", labId, labTitle, dueAt}));

    when(challengeRepository.countByLabIds(List.of(labId)))
        .thenReturn(Collections.singletonList(new Object[] {labId, 5L}));
    when(challengeCompletionRepository.aggregateSolvedCountsForUsersAndLabs(List.of(userId), List.of(labId)))
        .thenReturn(Collections.singletonList(new Object[] {userId, labId, 3L, dueAt.minusMinutes(5)}));

    var deadlines = courseService.listUpcomingDeadlines(userId);

    assertThat(deadlines).hasSize(3);
    for (int i = 0; i < courseIds.length; i++) {
      CourseLabDeadlineDto deadline = deadlines.get(i);
      assertThat(deadline.getCourseId()).isEqualTo(courseIds[i]);
      assertThat(deadline.getCourseTitle()).isEqualTo("Course " + (i + 1));
      assertThat(deadline.getLabId()).isEqualTo(labId);
      assertThat(deadline.getLabTitle()).isEqualTo(labTitle);
      assertThat(deadline.getDueAt()).isEqualTo(dueAt);
    }
  }

  @Test
  void listUpcomingDeadlines_multipleCoursesWithSameCompletedLab() {
    UUID userId = UUID.randomUUID();
    UUID[] courseIds = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
    UUID labId = UUID.randomUUID();
    String labTitle = "Lab (in 3 different courses; already completed)";
    LocalDateTime dueAt = LocalDateTime.of(2026, 5, 13, 8, 0);

    when(courseLabRepository.findDeadlinesForUser(userId))
        .thenReturn(
            List.of(
                new Object[] {courseIds[0], "Course 1", labId, labTitle, dueAt},
                new Object[] {courseIds[1], "Course 2", labId, labTitle, dueAt},
                new Object[] {courseIds[2], "Course 3", labId, labTitle, dueAt}));

    when(challengeRepository.countByLabIds(List.of(labId)))
        .thenReturn(Collections.singletonList(new Object[] {labId, 5L}));
    when(challengeCompletionRepository.aggregateSolvedCountsForUsersAndLabs(List.of(userId), List.of(labId)))
        .thenReturn(Collections.singletonList(new Object[] {userId, labId, 5L, dueAt.minusMinutes(5)}));

    var deadlines = courseService.listUpcomingDeadlines(userId);
    assertThat(deadlines).hasSize(0);
  }

  @Test
  void listUpcomingDeadlines_noDeadlines() {
    UUID userId = UUID.randomUUID();
    when(courseLabRepository.findDeadlinesForUser(userId)).thenReturn(Collections.emptyList());
    assertThat(courseService.listUpcomingDeadlines(userId)).hasSize(0);
  }

  @Test
  void joinByInviteCode_withInvalidCode_throwsInvalidInviteCodeException() {
    UUID studentId = UUID.randomUUID();

    User student = new User();
    student.setId(studentId);

    when(userRepository.findByIdAndDeletedAtIsNull(studentId)).thenReturn(Optional.of(student));
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
    course.setStatus(CourseStatusEnum.PUBLIC);

    when(userRepository.findByIdAndDeletedAtIsNull(studentId)).thenReturn(Optional.of(student));
    when(courseRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(course));

    assertThatThrownBy(() -> courseService.joinByInviteCode("ABC123", studentId))
        .isInstanceOf(InvalidInviteCodeException.class);

    verify(courseRepository, never()).save(any(Course.class));
  }

  @Test
  void joinByInviteCode_whenCourseWasSoftDeleted_throwsInvalidInviteCodeException() {
    UUID studentId = UUID.randomUUID();

    User student = new User();
    student.setId(studentId);

    // Repository-level soft-delete filtering returns no match for deleted courses.
    when(userRepository.findByIdAndDeletedAtIsNull(studentId)).thenReturn(Optional.of(student));
    when(courseRepository.findByInviteCode("DEL123")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> courseService.joinByInviteCode("DEL123", studentId))
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
    course.setStatus(CourseStatusEnum.PRIVATE);

    when(userRepository.findByIdAndDeletedAtIsNull(studentId)).thenReturn(Optional.of(student));
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
    course.setStatus(CourseStatusEnum.PRIVATE);

    when(userRepository.findByIdAndDeletedAtIsNull(studentId)).thenReturn(Optional.of(student));
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
    course.setStatus(CourseStatusEnum.PRIVATE);

    CourseInstructor relation = new CourseInstructor();
    relation.setInstructorRole(InstructorRoleEnum.COLLABORATOR);
    relation.setInstructor(instructor);
    course.addCourseInstructor(relation);

    when(userRepository.findByIdAndDeletedAtIsNull(instructorId)).thenReturn(Optional.of(instructor));
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
    course.setStatus(CourseStatusEnum.PRIVATE);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));

    assertThatThrownBy(() -> courseService.regenerateInviteCode(courseId, nonOwnerId))
        .isInstanceOf(CourseAccessDeniedException.class);

    verify(courseInviteCodeHelper, never()).assignInviteCode(any(), any());
  }

  @Test
  void regenerateInviteCode_whenCourseIsNotPrivate_stillRegeneratesCode() {
    // Owner can pre-generate an invite code before saving the visibility change to PRIVATE.
    // updateCourse nulls the code again if the saved status is not PRIVATE, so the invariant
    // "non-PRIVATE -> no inviteCode" is restored on the next save.
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = new Course();
    course.setId(courseId);
    course.setStatus(CourseStatusEnum.PUBLIC);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(courseInviteCodeHelper.generateAndAssign(courseId)).thenReturn("NEWCOD");

    Course result = courseService.regenerateInviteCode(courseId, ownerId);

    verify(courseInviteCodeHelper).generateAndAssign(courseId);
    assertThat(result.getInviteCode()).isEqualTo("NEWCOD");
  }

  @Test
  void regenerateInviteCode_whenOwnerAndPrivate_regeneratesCode() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = new Course();
    course.setId(courseId);
    course.setStatus(CourseStatusEnum.PRIVATE);
    course.setInviteCode("OLDCOD");

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(courseInviteCodeHelper.generateAndAssign(courseId)).thenReturn("NEWCOD");

    Course result = courseService.regenerateInviteCode(courseId, ownerId);

    verify(courseInviteCodeHelper).generateAndAssign(courseId);
    assertThat(result).isSameAs(course);
    assertThat(result.getInviteCode()).isEqualTo("NEWCOD");
    assertThat(result.getCourseInstructors()).hasSize(1);
  }

  @Test
  void regenerateInviteCode_whenCourseNotFound_throwsCourseNotFoundException() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> courseService.regenerateInviteCode(courseId, ownerId))
        .isInstanceOf(CourseNotFoundException.class);

    verify(courseInviteCodeHelper, never()).generateAndAssign(any());
  }

  @Test
  void regenerateInviteCode_whenHelperExhausted_throwsInviteCodeGenerationException() {
    UUID ownerId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);

    Course course = new Course();
    course.setId(courseId);
    course.setStatus(CourseStatusEnum.PRIVATE);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(courseInviteCodeHelper.generateAndAssign(courseId))
        .thenThrow(new InviteCodeGenerationException(
            "Could not generate a unique invite code after 10 attempts"));

    assertThatThrownBy(() -> courseService.regenerateInviteCode(courseId, ownerId))
        .isInstanceOf(InviteCodeGenerationException.class);
  }

  @Test
  void createCourse_whenInviteCodeExhausted_throwsInviteCodeGenerationException() {
    UUID ownerId = UUID.randomUUID();

    User owner = new User();
    owner.setId(ownerId);
    owner.setRoles(Set.of(UserRoleEnum.ROLE_INSTRUCTOR));

    when(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(Optional.of(owner));
    when(courseInviteCodeHelper.saveNewCourseWithInviteCode(any(Course.class)))
        .thenThrow(new InviteCodeGenerationException(
            "Could not generate a unique invite code after 10 attempts"));

    CreateCourseRequest request = new CreateCourseRequest(
        "Private Course",
        "Desc",
        null,
        CourseStatusEnum.PRIVATE,
        null,
        null,
        List.of(),
        McAttemptsMode.UNLIMITED);

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
    course.setStatus(CourseStatusEnum.DRAFT);

    CourseInstructor ownerRelation = new CourseInstructor();
    ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
    ownerRelation.setInstructor(owner);
    course.addCourseInstructor(ownerRelation);

    when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(courseInviteCodeHelper.generateAndAssign(courseId))
        .thenThrow(new InviteCodeGenerationException(
            "Could not generate a unique invite code after 10 attempts"));

    UpdateCourseRequest updateRequest = new UpdateCourseRequest(
        "Updated title",
        "Updated description",
        null,
        CourseStatusEnum.PRIVATE,
        null,
        null,
        List.of(),
        McAttemptsMode.UNLIMITED);

    assertThatThrownBy(() -> courseService.updateCourse(ownerId, courseId, updateRequest))
        .isInstanceOf(InviteCodeGenerationException.class);
  }
}
