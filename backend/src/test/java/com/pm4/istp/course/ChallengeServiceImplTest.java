package com.pm4.istp.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.pm4.istp.badge.services.BadgeService;
import com.pm4.istp.course.db.CreateChallengeRequest;
import com.pm4.istp.course.db.SubTaskRequest;
import com.pm4.istp.course.db.UpdateChallengeRequest;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.ChallengeDifficultyEnum;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.db.entities.CourseChallenge;
import com.pm4.istp.course.db.entities.McAttemptsMode;
import com.pm4.istp.course.db.entities.SubTask;
import com.pm4.istp.course.db.entities.SubTaskType;
import com.pm4.istp.course.db.entities.SubTaskCompletion;
import com.pm4.istp.course.dto.ChallengeStudentDto;
import com.pm4.istp.course.dto.ListChallengeResponseDto;
import com.pm4.istp.course.dto.SubTaskSubmissionResponseDto;
import com.pm4.istp.course.exceptions.ChallengeAccessDeniedException;
import com.pm4.istp.course.exceptions.ChallengeNotFoundException;
import com.pm4.istp.course.exceptions.SubTaskAlreadySolvedException;
import com.pm4.istp.course.exceptions.SubTaskNotFoundException;
import com.pm4.istp.course.mappers.ChallengeMapper;
import com.pm4.istp.course.repositories.ChallengeRepository;
import com.pm4.istp.course.repositories.CourseChallengeRepository;
import com.pm4.istp.course.repositories.CourseChallengeScoreOverrideRepository;
import com.pm4.istp.course.repositories.CourseEnrollmentRepository;
import com.pm4.istp.course.repositories.CourseRepository;
import com.pm4.istp.course.repositories.StudentFlagSubmissionRepository;
import com.pm4.istp.course.repositories.StudentOptionSubmissionRepository;
import com.pm4.istp.course.repositories.SubTaskCompletionRepository;
import com.pm4.istp.course.repositories.SubTaskOptionRepository;
import com.pm4.istp.course.repositories.SubTaskRepository;
import com.pm4.istp.course.services.DockerImageAvailabilityService;
import com.pm4.istp.course.services.impl.ChallengeServiceImpl;
import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.exceptions.UserNotFoundException;
import com.pm4.istp.user.repositories.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceImplTest {

  private static final String DEFAULT_DOCKER_IMAGE = "ghcr.io/pm4-istp/default:latest";

  @Mock private UserRepository userRepository;
  @Mock private ChallengeRepository challengeRepository;
  @Mock private CourseChallengeRepository courseChallengeRepository;
  @Mock private CourseRepository courseRepository;
  @Mock private CourseChallengeScoreOverrideRepository courseChallengeScoreOverrideRepository;
  @Mock private SubTaskRepository subTaskRepository;
  @Mock private SubTaskOptionRepository subTaskOptionRepository;
  @Mock private SubTaskCompletionRepository subTaskCompletionRepository;
  @Mock private StudentOptionSubmissionRepository studentOptionSubmissionRepository;
  @Mock private StudentFlagSubmissionRepository studentFlagSubmissionRepository;
  @Mock private CourseEnrollmentRepository courseEnrollmentRepository;
  @Mock private ChallengeMapper challengeMapper;
  @Mock private DockerImageAvailabilityService dockerImageAvailabilityService;
  @Mock private BadgeService badgeService;

  @InjectMocks private ChallengeServiceImpl challengeService;

  private User buildUser(UUID id) {
    User user = new User();
    user.setId(id);
    return user;
  }

  private Challenge buildChallenge(UUID id, User creator, ChallengeStatusEnum status) {
    Challenge challenge = new Challenge();
    challenge.setId(id);
    challenge.setTitle("Sample");
    challenge.setStatus(status);
    challenge.setDifficulty(ChallengeDifficultyEnum.MEDIUM);
    challenge.setCreator(creator);
    challenge.setSubTasks(new ArrayList<>());
    return challenge;
  }

  private List<SubTaskRequest> oneSubTask() {
    return new ArrayList<>(
        List.of(new SubTaskRequest(null, "Task 1", "Find the flag", "ISTP{demo}", 0, SubTaskType.FLAG, 1, null, null)));
  }

  private CreateChallengeRequest createRequest(
      String title,
      String shortDesc,
      String desc,
      ChallengeStatusEnum status,
      ChallengeDifficultyEnum difficulty) {
    return new CreateChallengeRequest(
        title, shortDesc, desc, status, difficulty, DEFAULT_DOCKER_IMAGE, oneSubTask());
  }

  private CreateChallengeRequest createRequest(
      String title,
      String shortDesc,
      String desc,
      ChallengeStatusEnum status,
      ChallengeDifficultyEnum difficulty,
      String dockerImage) {
    return new CreateChallengeRequest(title, shortDesc, desc, status, difficulty, dockerImage, oneSubTask());
  }

  private UpdateChallengeRequest updateRequest(
      String title,
      String shortDesc,
      String desc,
      ChallengeStatusEnum status,
      ChallengeDifficultyEnum difficulty) {
    return new UpdateChallengeRequest(
        title, shortDesc, desc, status, difficulty, DEFAULT_DOCKER_IMAGE, oneSubTask());
  }

  private UpdateChallengeRequest updateRequest(
      String title,
      String shortDesc,
      String desc,
      ChallengeStatusEnum status,
      ChallengeDifficultyEnum difficulty,
      String dockerImage) {
    return new UpdateChallengeRequest(title, shortDesc, desc, status, difficulty, dockerImage, oneSubTask());
  }

  @Test
  void createChallenge_persistsChallengeWithCreatorAndSubTasks() {
    UUID creatorId = UUID.randomUUID();
    User creator = buildUser(creatorId);

    when(userRepository.findByIdAndDeletedAtIsNull(creatorId)).thenReturn(Optional.of(creator));
    when(challengeRepository.save(any(Challenge.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CreateChallengeRequest request =
        new CreateChallengeRequest(
            "Buffer Overflow",
            "Short summary",
            "Long description",
            ChallengeStatusEnum.DRAFT,
            ChallengeDifficultyEnum.HARD,
            "ghcr.io/pm4-istp/buffer-overflow:latest",
            new ArrayList<>(
                List.of(
                    new SubTaskRequest(null, "Recon", "Scan the host", "ISTP{abc}", 0, SubTaskType.FLAG, 1, null, null),
                    new SubTaskRequest(null, "Exploit", "Pop a shell", null, 1, SubTaskType.FLAG, 1, null, null))));

    Challenge created = challengeService.createChallenge(creatorId, request);

    assertThat(created.getTitle()).isEqualTo("Buffer Overflow");
    assertThat(created.getShortDescription()).isEqualTo("Short summary");
    assertThat(created.getDescription()).isEqualTo("Long description");
    assertThat(created.getStatus()).isEqualTo(ChallengeStatusEnum.DRAFT);
    assertThat(created.getDifficulty()).isEqualTo(ChallengeDifficultyEnum.HARD);
    assertThat(created.getDockerImage()).isEqualTo("ghcr.io/pm4-istp/buffer-overflow:latest");
    assertThat(created.getCreator()).isSameAs(creator);
    assertThat(created.getSubTasks()).hasSize(2);
    assertThat(created.getSubTasks().get(0).getTitle()).isEqualTo("Recon");
    assertThat(created.getSubTasks().get(0).getFlag()).isEqualTo("ISTP{abc}");
    assertThat(created.getSubTasks().get(0).getOrderIndex()).isZero();
    assertThat(created.getSubTasks().get(1).getTitle()).isEqualTo("Exploit");
    assertThat(created.getSubTasks().get(1).getFlag()).isNull();
    assertThat(created.getSubTasks().get(1).getOrderIndex()).isEqualTo(1);
    assertThat(created.getMaxScore()).isEqualTo(2);
    verify(dockerImageAvailabilityService).assertImageExists("ghcr.io/pm4-istp/buffer-overflow:latest");
    verify(challengeRepository).save(any(Challenge.class));
  }

  @Test
  void createChallenge_trimsEmptyFlagToNull() {
    UUID creatorId = UUID.randomUUID();
    User creator = buildUser(creatorId);

    when(userRepository.findByIdAndDeletedAtIsNull(creatorId)).thenReturn(Optional.of(creator));
    when(challengeRepository.save(any(Challenge.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CreateChallengeRequest request =
        new CreateChallengeRequest(
            "T",
            "S",
            "D",
            ChallengeStatusEnum.DRAFT,
            ChallengeDifficultyEnum.EASY,
            DEFAULT_DOCKER_IMAGE,
            new ArrayList<>(List.of(new SubTaskRequest(null, "Only", "Just desc", "   ", 0, SubTaskType.FLAG, 1, null, null))));

    Challenge created = challengeService.createChallenge(creatorId, request);

    assertThat(created.getSubTasks().get(0).getFlag()).isNull();
  }

  @Test
  void createChallenge_whenUserNotFound_throwsUserNotFoundException() {
    UUID creatorId = UUID.randomUUID();
    when(userRepository.findByIdAndDeletedAtIsNull(creatorId)).thenReturn(Optional.empty());

    CreateChallengeRequest request =
        createRequest(
            "Title",
            "Short",
            "Desc",
            ChallengeStatusEnum.DRAFT,
            ChallengeDifficultyEnum.EASY,
            DEFAULT_DOCKER_IMAGE);

    assertThatThrownBy(() -> challengeService.createChallenge(creatorId, request))
        .isInstanceOf(UserNotFoundException.class);

    verify(challengeRepository, never()).save(any(Challenge.class));
  }

  @Test
  void getChallenge_whenCallerIsCreator_returnsChallengeRegardlessOfStatus() {
    UUID creatorId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Challenge challenge = buildChallenge(challengeId, creator, ChallengeStatusEnum.DRAFT);

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

    Challenge result = challengeService.getChallenge(creatorId, challengeId);

    assertThat(result).isSameAs(challenge);
    verifyNoInteractions(courseChallengeRepository);
  }

  @Test
  void getChallenge_whenDraftAndCallerIsNotCreator_throwsAccessDenied() {
    UUID creatorId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Challenge challenge = buildChallenge(challengeId, creator, ChallengeStatusEnum.DRAFT);

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

    assertThatThrownBy(() -> challengeService.getChallenge(otherId, challengeId))
        .isInstanceOf(ChallengeAccessDeniedException.class);
  }

  @Test
  void getChallenge_whenPrivateAndCallerIsInstructorOfLinkedCourse_returnsChallenge() {
    UUID creatorId = UUID.randomUUID();
    UUID instructorId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Challenge challenge = buildChallenge(challengeId, creator, ChallengeStatusEnum.PRIVATE);

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(courseChallengeRepository.existsByChallengeIdAndCourseInstructorId(
            challengeId, instructorId))
        .thenReturn(true);

    Challenge result = challengeService.getChallenge(instructorId, challengeId);

    assertThat(result).isSameAs(challenge);
  }

  @Test
  void getChallenge_whenPrivateAndCallerIsEnrolledInCourseWithChallenge_returnsChallenge() {
    UUID creatorId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Challenge challenge = buildChallenge(challengeId, creator, ChallengeStatusEnum.PRIVATE);

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(courseChallengeRepository.existsByChallengeIdAndCourseInstructorId(challengeId, studentId))
        .thenReturn(false);
    when(courseChallengeRepository.existsByChallengeIdAndEnrolledUserId(challengeId, studentId))
        .thenReturn(true);

    Challenge result = challengeService.getChallenge(studentId, challengeId);

    assertThat(result).isSameAs(challenge);
  }

  @Test
  void getChallenge_whenPrivateAndCallerIsNotInstructorAndNotEnrolled_throwsAccessDenied() {
    UUID creatorId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Challenge challenge = buildChallenge(challengeId, creator, ChallengeStatusEnum.PRIVATE);

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(courseChallengeRepository.existsByChallengeIdAndCourseInstructorId(challengeId, otherId))
        .thenReturn(false);
    when(courseChallengeRepository.existsByChallengeIdAndEnrolledUserId(challengeId, otherId))
        .thenReturn(false);

    assertThatThrownBy(() -> challengeService.getChallenge(otherId, challengeId))
        .isInstanceOf(ChallengeAccessDeniedException.class);
  }

  @Test
  void getChallenge_whenPublic_isVisibleToAnyUser() {
    UUID creatorId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Challenge challenge = buildChallenge(challengeId, creator, ChallengeStatusEnum.PUBLIC);

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

    Challenge result = challengeService.getChallenge(otherId, challengeId);

    assertThat(result).isSameAs(challenge);
    verifyNoInteractions(courseChallengeRepository);
  }

  @Test
  void getChallenge_whenNotFound_throwsChallengeNotFoundException() {
    UUID userId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> challengeService.getChallenge(userId, challengeId))
        .isInstanceOf(ChallengeNotFoundException.class);
  }

  @Test
  void updateChallenge_whenStatusUnchanged_savesWithoutCleanup() {
    UUID creatorId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Challenge challenge = buildChallenge(challengeId, creator, ChallengeStatusEnum.PUBLIC);

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(challengeRepository.save(any(Challenge.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UpdateChallengeRequest request =
        updateRequest(
            "Updated",
            "Updated short",
            "Updated desc",
            ChallengeStatusEnum.PUBLIC,
            ChallengeDifficultyEnum.EASY,
            "ghcr.io/pm4-istp/updated:1.0");

    Challenge updated = challengeService.updateChallenge(creatorId, challengeId, request);

    assertThat(updated.getTitle()).isEqualTo("Updated");
    assertThat(updated.getShortDescription()).isEqualTo("Updated short");
    assertThat(updated.getDescription()).isEqualTo("Updated desc");
    assertThat(updated.getStatus()).isEqualTo(ChallengeStatusEnum.PUBLIC);
    assertThat(updated.getDifficulty()).isEqualTo(ChallengeDifficultyEnum.EASY);
    assertThat(updated.getDockerImage()).isEqualTo("ghcr.io/pm4-istp/updated:1.0");
    verify(dockerImageAvailabilityService).assertImageExists("ghcr.io/pm4-istp/updated:1.0");
    verify(courseChallengeRepository, never()).deleteByChallengeId(any());
    verify(courseChallengeRepository, never())
        .deleteByChallengeIdWhereCreatorNotInstructor(any(), any());
  }

  @Test
  void updateChallenge_reusesSubTasksByIdAndReindexes() {
    UUID creatorId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Challenge challenge = buildChallenge(challengeId, creator, ChallengeStatusEnum.PUBLIC);

    UUID existingId = UUID.randomUUID();
    SubTask existing = new SubTask();
    existing.setId(existingId);
    existing.setChallenge(challenge);
    existing.setTitle("Old");
    existing.setDescription("Old desc");
    existing.setOrderIndex(0);
    challenge.getSubTasks().add(existing);

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(challengeRepository.save(any(Challenge.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UpdateChallengeRequest request =
        new UpdateChallengeRequest(
            "T",
            "S",
            "D",
            ChallengeStatusEnum.PUBLIC,
            ChallengeDifficultyEnum.EASY,
            DEFAULT_DOCKER_IMAGE,
            new ArrayList<>(
                List.of(
                    new SubTaskRequest(null, "New first", "desc", null, 0, SubTaskType.FLAG, 1, null, null),
                    new SubTaskRequest(existingId, "Renamed", "updated desc", "ISTP{x}", 1, SubTaskType.FLAG, 1, null, null))));

    Challenge updated = challengeService.updateChallenge(creatorId, challengeId, request);

    assertThat(updated.getSubTasks()).hasSize(2);
    assertThat(updated.getSubTasks().get(0).getId()).isNull();
    assertThat(updated.getSubTasks().get(0).getTitle()).isEqualTo("New first");
    assertThat(updated.getSubTasks().get(1).getId()).isEqualTo(existingId);
    assertThat(updated.getSubTasks().get(1).getTitle()).isEqualTo("Renamed");
    assertThat(updated.getSubTasks().get(1).getFlag()).isEqualTo("ISTP{x}");
    assertThat(updated.getSubTasks().get(1).getOrderIndex()).isEqualTo(1);
    assertThat(updated.getMaxScore()).isEqualTo(2);
  }

  @Test
  void updateChallenge_whenDowngradedToDraft_deletesAllCourseAssignments() {
    UUID creatorId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Challenge challenge = buildChallenge(challengeId, creator, ChallengeStatusEnum.PUBLIC);

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(challengeRepository.save(any(Challenge.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UpdateChallengeRequest request =
        updateRequest(
            "Title",
            "Short",
            "Desc",
            ChallengeStatusEnum.DRAFT,
            ChallengeDifficultyEnum.MEDIUM,
            DEFAULT_DOCKER_IMAGE);

    challengeService.updateChallenge(creatorId, challengeId, request);

    verify(courseChallengeRepository).deleteByChallengeId(challengeId);
    verify(courseChallengeRepository, never())
        .deleteByChallengeIdWhereCreatorNotInstructor(any(), any());
  }

  @Test
  void updateChallenge_whenDowngradedFromPublicToPrivate_deletesCoursesWhereCreatorIsNotInstructor() {
    UUID creatorId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Challenge challenge = buildChallenge(challengeId, creator, ChallengeStatusEnum.PUBLIC);

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(challengeRepository.save(any(Challenge.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UpdateChallengeRequest request =
        updateRequest(
            "Title",
            "Short",
            "Desc",
            ChallengeStatusEnum.PRIVATE,
            ChallengeDifficultyEnum.MEDIUM,
            DEFAULT_DOCKER_IMAGE);

    challengeService.updateChallenge(creatorId, challengeId, request);

    verify(courseChallengeRepository)
        .deleteByChallengeIdWhereCreatorNotInstructor(challengeId, creatorId);
    verify(courseChallengeRepository, never()).deleteByChallengeId(any());
  }

  @Test
  void updateChallenge_whenPromotedFromPrivateToPublic_doesNotCleanUpAssignments() {
    UUID creatorId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Challenge challenge = buildChallenge(challengeId, creator, ChallengeStatusEnum.PRIVATE);

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(challengeRepository.save(any(Challenge.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UpdateChallengeRequest request =
        updateRequest(
            "Title",
            "Short",
            "Desc",
            ChallengeStatusEnum.PUBLIC,
            ChallengeDifficultyEnum.MEDIUM,
            DEFAULT_DOCKER_IMAGE);

    challengeService.updateChallenge(creatorId, challengeId, request);

    verify(courseChallengeRepository, never()).deleteByChallengeId(any());
    verify(courseChallengeRepository, never())
        .deleteByChallengeIdWhereCreatorNotInstructor(any(), any());
  }

  @Test
  void updateChallenge_whenCallerIsNotCreator_throwsAccessDenied() {
    UUID creatorId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Challenge challenge = buildChallenge(challengeId, creator, ChallengeStatusEnum.PUBLIC);

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

    UpdateChallengeRequest request =
        updateRequest(
            "Title",
            "Short",
            "Desc",
            ChallengeStatusEnum.PRIVATE,
            ChallengeDifficultyEnum.MEDIUM,
            DEFAULT_DOCKER_IMAGE);

    assertThatThrownBy(() -> challengeService.updateChallenge(otherId, challengeId, request))
        .isInstanceOf(ChallengeAccessDeniedException.class);

    verify(challengeRepository, never()).save(any(Challenge.class));
    verify(courseChallengeRepository, never()).deleteByChallengeId(any());
  }

  @Test
  void updateChallenge_whenChallengeNotFound_throwsNotFound() {
    UUID creatorId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.empty());

    UpdateChallengeRequest request =
        updateRequest(
            "Title",
            "Short",
            "Desc",
            ChallengeStatusEnum.PUBLIC,
            ChallengeDifficultyEnum.MEDIUM,
            DEFAULT_DOCKER_IMAGE);

    assertThatThrownBy(() -> challengeService.updateChallenge(creatorId, challengeId, request))
        .isInstanceOf(ChallengeNotFoundException.class);

    verify(challengeRepository, never()).save(any(Challenge.class));
  }

  @Test
  void previewVisibilityImpact_whenStatusUnchanged_returnsZero() {
    UUID creatorId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Challenge challenge = buildChallenge(challengeId, creator, ChallengeStatusEnum.PUBLIC);

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

    int impact =
        challengeService.previewVisibilityImpact(
            creatorId, challengeId, ChallengeStatusEnum.PUBLIC);

    assertThat(impact).isZero();
    verify(courseChallengeRepository, never()).countByChallengeId(any());
    verify(courseChallengeRepository, never())
        .countByChallengeIdWhereCreatorNotInstructor(any(), any());
  }

  @Test
  void previewVisibilityImpact_whenDowngradingToDraft_returnsTotalCount() {
    UUID creatorId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Challenge challenge = buildChallenge(challengeId, creator, ChallengeStatusEnum.PUBLIC);

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(courseChallengeRepository.countByChallengeId(challengeId)).thenReturn(3L);

    int impact =
        challengeService.previewVisibilityImpact(
            creatorId, challengeId, ChallengeStatusEnum.DRAFT);

    assertThat(impact).isEqualTo(3);
  }

  @Test
  void previewVisibilityImpact_whenDowngradingFromPublicToPrivate_returnsCreatorNotInstructorCount() {
    UUID creatorId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Challenge challenge = buildChallenge(challengeId, creator, ChallengeStatusEnum.PUBLIC);

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(courseChallengeRepository.countByChallengeIdWhereCreatorNotInstructor(
            challengeId, creatorId))
        .thenReturn(2L);

    int impact =
        challengeService.previewVisibilityImpact(
            creatorId, challengeId, ChallengeStatusEnum.PRIVATE);

    assertThat(impact).isEqualTo(2);
  }

  @Test
  void previewVisibilityImpact_whenPromoting_returnsZero() {
    UUID creatorId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Challenge challenge = buildChallenge(challengeId, creator, ChallengeStatusEnum.PRIVATE);

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

    int impact =
        challengeService.previewVisibilityImpact(
            creatorId, challengeId, ChallengeStatusEnum.PUBLIC);

    assertThat(impact).isZero();
    verify(courseChallengeRepository, never()).countByChallengeId(any());
    verify(courseChallengeRepository, never())
        .countByChallengeIdWhereCreatorNotInstructor(any(), any());
  }

  @Test
  void previewVisibilityImpact_whenCallerIsNotCreator_throwsAccessDenied() {
    UUID creatorId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Challenge challenge = buildChallenge(challengeId, creator, ChallengeStatusEnum.PUBLIC);

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

    assertThatThrownBy(
            () ->
                challengeService.previewVisibilityImpact(
                    otherId, challengeId, ChallengeStatusEnum.PRIVATE))
        .isInstanceOf(ChallengeAccessDeniedException.class);
  }

  @Test
  void previewVisibilityImpact_whenChallengeNotFound_throwsNotFound() {
    UUID creatorId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                challengeService.previewVisibilityImpact(
                    creatorId, challengeId, ChallengeStatusEnum.DRAFT))
        .isInstanceOf(ChallengeNotFoundException.class);
  }

  @Test
  void deleteChallenge_whenCallerIsCreator_deletesChallenge() {
    UUID creatorId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Challenge challenge = buildChallenge(challengeId, creator, ChallengeStatusEnum.DRAFT);

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

    challengeService.deleteChallenge(creatorId, challengeId);

    verify(challengeRepository).delete(challenge);
  }

  @Test
  void deleteChallenge_whenCallerIsNotCreator_throwsAccessDenied() {
    UUID creatorId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Challenge challenge = buildChallenge(challengeId, creator, ChallengeStatusEnum.DRAFT);

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

    assertThatThrownBy(() -> challengeService.deleteChallenge(otherId, challengeId))
        .isInstanceOf(ChallengeAccessDeniedException.class);

    verify(challengeRepository, never()).delete(any(Challenge.class));
  }

  @Test
  void deleteChallenge_whenChallengeNotFound_throwsNotFound() {
    UUID userId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();

    when(challengeRepository.findById(challengeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> challengeService.deleteChallenge(userId, challengeId))
        .isInstanceOf(ChallengeNotFoundException.class);
  }

  @Test
  void listChallengesForCreator_delegatesToRepository() {
    UUID creatorId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 10);
    Page<ListChallengeResponseDto> expected = new PageImpl<>(List.of());

    when(challengeRepository.findListChallengesForCreator(creatorId, pageable))
        .thenReturn(expected);

    Page<ListChallengeResponseDto> result =
        challengeService.listChallengesForCreator(creatorId, pageable);

    assertThat(result).isSameAs(expected);
    verify(challengeRepository).findListChallengesForCreator(creatorId, pageable);
  }

  @Test
  void searchAvailableChallenges_delegatesToRepository() {
    UUID userId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 10);
    Page<ListChallengeResponseDto> expected = new PageImpl<>(List.of());

    when(challengeRepository.searchAvailableChallenges(eq(userId), eq("sql"), eq(pageable)))
        .thenReturn(expected);

    Page<ListChallengeResponseDto> result =
        challengeService.searchAvailableChallenges(userId, "sql", pageable);

    assertThat(result).isSameAs(expected);
    verify(challengeRepository).searchAvailableChallenges(userId, "sql", pageable);
  }

  // ── getChallengeForPlay ────────────────────────────────────────────────────

  private Challenge buildChallengeWithCourses(UUID challengeId, UUID... courseIds) {
    Challenge challenge = buildChallenge(challengeId, buildUser(UUID.randomUUID()),
        ChallengeStatusEnum.PUBLIC);
    List<CourseChallenge> ccs = new ArrayList<>();
    for (UUID courseId : courseIds) {
      Course course = new Course();
      course.setId(courseId);
      CourseChallenge cc = new CourseChallenge();
      cc.setCourse(course);
      cc.setChallenge(challenge);
      ccs.add(cc);
    }
    challenge.setCourseChallenges(ccs);
    return challenge;
  }

  @Test
  void getChallengeForPlay_returnsStudentDto_whenEnrolledAndChallengeBelongsToCourse() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Challenge challenge = buildChallengeWithCourses(challengeId, courseId);

    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
        .thenReturn(true);
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    ChallengeStudentDto dto = new ChallengeStudentDto();
    dto.setId(challengeId);
    when(challengeMapper.toStudentDto(challenge)).thenReturn(dto);

    // Stub the course lookup used to set mcAttemptsMode on the DTO
    Course course = new Course();
    course.setId(courseId);
    course.setMcAttemptsMode(McAttemptsMode.UNLIMITED);
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(
            courseChallengeScoreOverrideRepository.findByCourseIdAndParticipantIdAndChallengeId(
                courseId, userId, challengeId))
        .thenReturn(Optional.empty());

    ChallengeStudentDto result = challengeService.getChallengeForPlay(userId, courseId, challengeId);

    assertThat(result).isSameAs(dto);
    assertThat(result.getMcAttemptsMode()).isEqualTo("UNLIMITED");
  }

  @Test
  void getChallengeForPlay_whenNotEnrolled_throwsAccessDenied() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();

    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
        .thenReturn(false);

    assertThatThrownBy(() -> challengeService.getChallengeForPlay(userId, courseId, challengeId))
        .isInstanceOf(ChallengeAccessDeniedException.class);
    verify(challengeRepository, never()).findById(any());
  }

  @Test
  void getChallengeForPlay_whenChallengeMissing_throwsNotFound() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();

    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
        .thenReturn(true);
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> challengeService.getChallengeForPlay(userId, courseId, challengeId))
        .isInstanceOf(ChallengeNotFoundException.class);
  }

  @Test
  void getChallengeForPlay_whenChallengeNotInRequestedCourse_throwsAccessDenied() {
    // Regression test for the authz-bypass: even if the user is enrolled in a course
    // and the challenge exists in some *other* course, the requested courseId must
    // actually contain the challenge.
    UUID userId = UUID.randomUUID();
    UUID requestedCourseId = UUID.randomUUID();
    UUID otherCourseId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Challenge challenge = buildChallengeWithCourses(challengeId, otherCourseId);

    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(requestedCourseId, userId))
        .thenReturn(true);
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

    assertThatThrownBy(
        () -> challengeService.getChallengeForPlay(userId, requestedCourseId, challengeId))
        .isInstanceOf(ChallengeAccessDeniedException.class)
        .hasMessageContaining("not part of course");
  }

  // ── submitSubTaskFlag ──────────────────────────────────────────────────────

  private SubTask buildSubTask(UUID id, Challenge parent, String flag) {
    SubTask st = new SubTask();
    st.setId(id);
    st.setChallenge(parent);
    st.setFlag(flag);
    parent.getSubTasks().add(st);
    return st;
  }

  @Test
  void submitSubTaskFlag_returnsCorrectAndPersists_whenFlagMatches() {
    UUID userId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID subTaskId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    User user = buildUser(userId);
    Challenge challenge = buildChallengeWithCourses(challengeId, courseId);
    SubTask subTask = buildSubTask(subTaskId, challenge, "ISTP{secret}");

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
    when(subTaskRepository.findById(subTaskId)).thenReturn(Optional.of(subTask));
    when(courseChallengeRepository.existsByChallengeIdAndEnrolledUserId(challengeId, userId))
        .thenReturn(true);
    when(subTaskCompletionRepository.existsByUserIdAndSubTaskId(userId, subTaskId))
        .thenReturn(false);
    when(subTaskCompletionRepository.findSolvedSubTaskIds(eq(userId), any()))
        .thenReturn(List.of(subTaskId));
    when(studentFlagSubmissionRepository.findByUserIdAndSubTaskId(userId, subTaskId))
        .thenReturn(Optional.empty());

    SubTaskSubmissionResponseDto result =
        challengeService.submitSubTaskFlag(userId, challengeId, subTaskId, "ISTP{secret}");

    assertThat(result.isCorrect()).isTrue();
    assertThat(result.isChallengeSolved()).isTrue();
    verify(subTaskCompletionRepository).saveAndFlush(any(SubTaskCompletion.class));
  }

  @Test
  void submitSubTaskFlag_returnsIncorrect_andDoesNotPersist_whenFlagMismatches() {
    UUID userId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID subTaskId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    User user = buildUser(userId);
    Challenge challenge = buildChallengeWithCourses(challengeId, courseId);
    SubTask subTask = buildSubTask(subTaskId, challenge, "ISTP{secret}");

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
    when(subTaskRepository.findById(subTaskId)).thenReturn(Optional.of(subTask));
    when(courseChallengeRepository.existsByChallengeIdAndEnrolledUserId(challengeId, userId))
        .thenReturn(true);
    when(subTaskCompletionRepository.existsByUserIdAndSubTaskId(userId, subTaskId))
        .thenReturn(false);
    when(subTaskCompletionRepository.findSolvedSubTaskIds(eq(userId), any()))
        .thenReturn(List.of());
    when(studentFlagSubmissionRepository.findByUserIdAndSubTaskId(userId, subTaskId))
        .thenReturn(Optional.empty());

    SubTaskSubmissionResponseDto result =
        challengeService.submitSubTaskFlag(userId, challengeId, subTaskId, "ISTP{wrong}");

    assertThat(result.isCorrect()).isFalse();
    verify(subTaskCompletionRepository, never()).saveAndFlush(any());
  }

  @Test
  void submitSubTaskFlag_whenSubTaskNotFound_throwsNotFound() {
    UUID userId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID subTaskId = UUID.randomUUID();

    when(userRepository.findByIdAndDeletedAtIsNull(userId))
        .thenReturn(Optional.of(buildUser(userId)));
    when(subTaskRepository.findById(subTaskId)).thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> challengeService.submitSubTaskFlag(userId, challengeId, subTaskId, "ISTP{x}"))
        .isInstanceOf(SubTaskNotFoundException.class);
  }

  @Test
  void submitSubTaskFlag_whenSubTaskBelongsToOtherChallenge_throwsNotFound() {
    UUID userId = UUID.randomUUID();
    UUID requestedChallengeId = UUID.randomUUID();
    UUID actualChallengeId = UUID.randomUUID();
    UUID subTaskId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    Challenge actual = buildChallengeWithCourses(actualChallengeId, courseId);
    SubTask subTask = buildSubTask(subTaskId, actual, "ISTP{secret}");

    when(userRepository.findByIdAndDeletedAtIsNull(userId))
        .thenReturn(Optional.of(buildUser(userId)));
    when(subTaskRepository.findById(subTaskId)).thenReturn(Optional.of(subTask));

    assertThatThrownBy(
        () -> challengeService.submitSubTaskFlag(
            userId, requestedChallengeId, subTaskId, "ISTP{secret}"))
        .isInstanceOf(SubTaskNotFoundException.class);
  }

  @Test
  void submitSubTaskFlag_whenNotEnrolled_throwsAccessDenied() {
    UUID userId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID subTaskId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    Challenge challenge = buildChallengeWithCourses(challengeId, courseId);
    SubTask subTask = buildSubTask(subTaskId, challenge, "ISTP{secret}");

    when(userRepository.findByIdAndDeletedAtIsNull(userId))
        .thenReturn(Optional.of(buildUser(userId)));
    when(subTaskRepository.findById(subTaskId)).thenReturn(Optional.of(subTask));
    when(courseChallengeRepository.existsByChallengeIdAndEnrolledUserId(challengeId, userId))
        .thenReturn(false);

    assertThatThrownBy(
        () -> challengeService.submitSubTaskFlag(userId, challengeId, subTaskId, "ISTP{secret}"))
        .isInstanceOf(ChallengeAccessDeniedException.class);
  }

  @Test
  void submitSubTaskFlag_whenAlreadySolved_throws409() {
    UUID userId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID subTaskId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    Challenge challenge = buildChallengeWithCourses(challengeId, courseId);
    SubTask subTask = buildSubTask(subTaskId, challenge, "ISTP{secret}");

    when(userRepository.findByIdAndDeletedAtIsNull(userId))
        .thenReturn(Optional.of(buildUser(userId)));
    when(subTaskRepository.findById(subTaskId)).thenReturn(Optional.of(subTask));
    when(courseChallengeRepository.existsByChallengeIdAndEnrolledUserId(challengeId, userId))
        .thenReturn(true);
    when(subTaskCompletionRepository.existsByUserIdAndSubTaskId(userId, subTaskId))
        .thenReturn(true);

    assertThatThrownBy(
        () -> challengeService.submitSubTaskFlag(userId, challengeId, subTaskId, "ISTP{secret}"))
        .isInstanceOf(SubTaskAlreadySolvedException.class);
  }

  @Test
  void submitSubTaskFlag_whenConcurrentInsertHitsUniqueConstraint_throws409() {
    // Race condition: two concurrent correct submissions both pass the
    // existsByUserIdAndSubTaskId check, then one trips the unique constraint
    // on insert. We must surface that as 409, not 500.
    UUID userId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID subTaskId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    Challenge challenge = buildChallengeWithCourses(challengeId, courseId);
    SubTask subTask = buildSubTask(subTaskId, challenge, "ISTP{secret}");

    when(userRepository.findByIdAndDeletedAtIsNull(userId))
        .thenReturn(Optional.of(buildUser(userId)));
    when(subTaskRepository.findById(subTaskId)).thenReturn(Optional.of(subTask));
    when(courseChallengeRepository.existsByChallengeIdAndEnrolledUserId(challengeId, userId))
        .thenReturn(true);
    when(subTaskCompletionRepository.existsByUserIdAndSubTaskId(userId, subTaskId))
        .thenReturn(false);
    when(subTaskCompletionRepository.saveAndFlush(any(SubTaskCompletion.class)))
        .thenThrow(new DataIntegrityViolationException("unique violation"));
    when(studentFlagSubmissionRepository.findByUserIdAndSubTaskId(userId, subTaskId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> challengeService.submitSubTaskFlag(userId, challengeId, subTaskId, "ISTP{secret}"))
        .isInstanceOf(SubTaskAlreadySolvedException.class);
  }
}
