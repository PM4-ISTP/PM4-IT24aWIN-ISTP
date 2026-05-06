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
import com.pm4.istp.course.db.CreateLabRequest;
import com.pm4.istp.course.db.ChallengeRequest;
import com.pm4.istp.course.db.UpdateLabRequest;
import com.pm4.istp.course.db.entities.Lab;
import com.pm4.istp.course.db.entities.LabDifficultyEnum;
import com.pm4.istp.course.db.entities.LabStatusEnum;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.db.entities.CourseLab;
import com.pm4.istp.course.db.entities.McAttemptsMode;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.ChallengeOption;
import com.pm4.istp.course.db.entities.ChallengeType;
import com.pm4.istp.course.db.entities.ChallengeCompletion;
import com.pm4.istp.course.db.entities.StudentOptionSubmission;
import com.pm4.istp.course.dto.LabStudentDto;
import com.pm4.istp.course.dto.ListLabResponseDto;
import com.pm4.istp.course.dto.ChallengeSubmissionResponseDto;
import com.pm4.istp.course.dto.ChoiceSubmissionResponseDto;
import com.pm4.istp.course.exceptions.LabAccessDeniedException;
import com.pm4.istp.course.exceptions.LabNotFoundException;
import com.pm4.istp.course.exceptions.ChallengeAlreadySolvedException;
import com.pm4.istp.course.exceptions.ChallengeNotFoundException;
import com.pm4.istp.course.mappers.LabMapper;
import com.pm4.istp.course.repositories.LabRepository;
import com.pm4.istp.course.repositories.CourseLabRepository;
import com.pm4.istp.course.repositories.CourseEnrollmentRepository;
import com.pm4.istp.course.repositories.CourseRepository;
import com.pm4.istp.course.repositories.StudentOptionSubmissionRepository;
import com.pm4.istp.course.repositories.ChallengeCompletionRepository;
import com.pm4.istp.course.repositories.ChallengeOptionRepository;
import com.pm4.istp.course.repositories.ChallengeRepository;
import com.pm4.istp.course.services.DockerImageAvailabilityService;
import com.pm4.istp.course.services.impl.LabServiceImpl;
import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.exceptions.UserNotFoundException;
import com.pm4.istp.user.repositories.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceImplTest {

  private static final String DEFAULT_DOCKER_IMAGE = "ghcr.io/pm4-istp/default:latest";

  @Mock private UserRepository userRepository;
  @Mock private LabRepository labRepository;
  @Mock private CourseLabRepository courseLabRepository;
  @Mock private CourseRepository courseRepository;
  @Mock private ChallengeRepository challengeRepository;
  @Mock private ChallengeOptionRepository challengeOptionRepository;
  @Mock private ChallengeCompletionRepository challengeCompletionRepository;
  @Mock private StudentOptionSubmissionRepository studentOptionSubmissionRepository;
  @Mock private CourseEnrollmentRepository courseEnrollmentRepository;
  @Mock private LabMapper labMapper;
  @Mock private DockerImageAvailabilityService dockerImageAvailabilityService;
  @Mock private BadgeService badgeService;

  @InjectMocks private LabServiceImpl labService;

  private User buildUser(UUID id) {
    User user = new User();
    user.setId(id);
    return user;
  }

  private Lab buildChallenge(UUID id, User creator, LabStatusEnum status) {
    Lab lab = new Lab();
    lab.setId(id);
    lab.setTitle("Sample");
    lab.setStatus(status);
    lab.setDifficulty(LabDifficultyEnum.MEDIUM);
    lab.setCreator(creator);
    lab.setChallenges(new ArrayList<>());
    return lab;
  }

  private List<ChallengeRequest> oneChallenge() {
    return new ArrayList<>(
        List.of(new ChallengeRequest(null, "Task 1", "Find the flag", "ISTP{demo}", 0, ChallengeType.FLAG, 1, null, null)));
  }

  private CreateLabRequest createRequest(
      String title,
      String shortDesc,
      String desc,
      LabStatusEnum status,
      LabDifficultyEnum difficulty,
      String dockerImage) {
    return new CreateLabRequest(title, shortDesc, desc, status, difficulty, dockerImage, oneChallenge());
  }

  private UpdateLabRequest updateRequest(
      String title,
      String shortDesc,
      String desc,
      LabStatusEnum status,
      LabDifficultyEnum difficulty,
      String dockerImage) {
    return new UpdateLabRequest(title, shortDesc, desc, status, difficulty, dockerImage, oneChallenge());
  }

  @Test
  void createChallenge_persistsChallengeWithCreatorAndChallenges() {
    UUID creatorId = UUID.randomUUID();
    User creator = buildUser(creatorId);

    when(userRepository.findByIdAndDeletedAtIsNull(creatorId)).thenReturn(Optional.of(creator));
    when(labRepository.save(any(Lab.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CreateLabRequest request =
        new CreateLabRequest(
            "Buffer Overflow",
            "Short summary",
            "Long description",
            LabStatusEnum.DRAFT,
            LabDifficultyEnum.HARD,
            "ghcr.io/pm4-istp/buffer-overflow:latest",
            new ArrayList<>(
                List.of(
                    new ChallengeRequest(null, "Recon", "Scan the host", "ISTP{abc}", 0, ChallengeType.FLAG, 1, null, null),
                    new ChallengeRequest(null, "Exploit", "Pop a shell", null, 1, ChallengeType.FLAG, 1, null, null))));

    Lab created = labService.createChallenge(creatorId, request);

    assertThat(created.getTitle()).isEqualTo("Buffer Overflow");
    assertThat(created.getShortDescription()).isEqualTo("Short summary");
    assertThat(created.getDescription()).isEqualTo("Long description");
    assertThat(created.getStatus()).isEqualTo(LabStatusEnum.DRAFT);
    assertThat(created.getDifficulty()).isEqualTo(LabDifficultyEnum.HARD);
    assertThat(created.getDockerImage()).isEqualTo("ghcr.io/pm4-istp/buffer-overflow:latest");
    assertThat(created.getCreator()).isSameAs(creator);
    assertThat(created.getChallenges()).hasSize(2);
    assertThat(created.getChallenges().get(0).getTitle()).isEqualTo("Recon");
    assertThat(created.getChallenges().get(0).getFlag()).isEqualTo("ISTP{abc}");
    assertThat(created.getChallenges().get(0).getOrderIndex()).isZero();
    assertThat(created.getChallenges().get(1).getTitle()).isEqualTo("Exploit");
    assertThat(created.getChallenges().get(1).getFlag()).isNull();
    assertThat(created.getChallenges().get(1).getOrderIndex()).isEqualTo(1);
    assertThat(created.getMaxScore()).isEqualTo(2);
    verify(dockerImageAvailabilityService).assertImageExists("ghcr.io/pm4-istp/buffer-overflow:latest");
    verify(labRepository).save(any(Lab.class));
  }

  @Test
  void createChallenge_trimsEmptyFlagToNull() {
    UUID creatorId = UUID.randomUUID();
    User creator = buildUser(creatorId);

    when(userRepository.findByIdAndDeletedAtIsNull(creatorId)).thenReturn(Optional.of(creator));
    when(labRepository.save(any(Lab.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CreateLabRequest request =
        new CreateLabRequest(
            "T",
            "S",
            "D",
            LabStatusEnum.DRAFT,
            LabDifficultyEnum.EASY,
            DEFAULT_DOCKER_IMAGE,
            new ArrayList<>(List.of(new ChallengeRequest(null, "Only", "Just desc", "   ", 0, ChallengeType.FLAG, 1, null, null))));

    Lab created = labService.createChallenge(creatorId, request);

    assertThat(created.getChallenges().get(0).getFlag()).isNull();
  }

  @Test
  void createChallenge_whenUserNotFound_throwsUserNotFoundException() {
    UUID creatorId = UUID.randomUUID();
    when(userRepository.findByIdAndDeletedAtIsNull(creatorId)).thenReturn(Optional.empty());

    CreateLabRequest request =
        createRequest(
            "Title",
            "Short",
            "Desc",
            LabStatusEnum.DRAFT,
            LabDifficultyEnum.EASY,
            DEFAULT_DOCKER_IMAGE);

    assertThatThrownBy(() -> labService.createChallenge(creatorId, request))
        .isInstanceOf(UserNotFoundException.class);

    verify(labRepository, never()).save(any(Lab.class));
  }

  @Test
  void getChallenge_whenCallerIsCreator_returnsChallengeRegardlessOfStatus() {
    UUID creatorId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Lab lab = buildChallenge(labId, creator, LabStatusEnum.DRAFT);

    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));

    Lab result = labService.getChallenge(creatorId, labId);

    assertThat(result).isSameAs(lab);
    verifyNoInteractions(courseLabRepository);
  }

  @Test
  void getChallenge_whenDraftAndCallerIsNotCreator_throwsAccessDenied() {
    UUID creatorId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Lab lab = buildChallenge(labId, creator, LabStatusEnum.DRAFT);

    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));

    assertThatThrownBy(() -> labService.getChallenge(otherId, labId))
        .isInstanceOf(LabAccessDeniedException.class);
  }

  @Test
  void getChallenge_whenPrivateAndCallerIsInstructorOfLinkedCourse_returnsChallenge() {
    UUID creatorId = UUID.randomUUID();
    UUID instructorId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Lab lab = buildChallenge(labId, creator, LabStatusEnum.PRIVATE);

    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));
    when(courseLabRepository.existsByChallengeIdAndCourseInstructorId(
            labId, instructorId))
        .thenReturn(true);

    Lab result = labService.getChallenge(instructorId, labId);

    assertThat(result).isSameAs(lab);
  }

  @Test
  void getChallenge_whenPrivateAndCallerIsEnrolledInCourseWithChallenge_returnsChallenge() {
    UUID creatorId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Lab lab = buildChallenge(labId, creator, LabStatusEnum.PRIVATE);

    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));
    when(courseLabRepository.existsByChallengeIdAndCourseInstructorId(labId, studentId))
        .thenReturn(false);
    when(courseLabRepository.existsByChallengeIdAndEnrolledUserId(labId, studentId))
        .thenReturn(true);

    Lab result = labService.getChallenge(studentId, labId);

    assertThat(result).isSameAs(lab);
  }

  @Test
  void getChallenge_whenPrivateAndCallerIsNotInstructorAndNotEnrolled_throwsAccessDenied() {
    UUID creatorId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Lab lab = buildChallenge(labId, creator, LabStatusEnum.PRIVATE);

    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));
    when(courseLabRepository.existsByChallengeIdAndCourseInstructorId(labId, otherId))
        .thenReturn(false);
    when(courseLabRepository.existsByChallengeIdAndEnrolledUserId(labId, otherId))
        .thenReturn(false);

    assertThatThrownBy(() -> labService.getChallenge(otherId, labId))
        .isInstanceOf(LabAccessDeniedException.class);
  }

  @Test
  void getChallenge_whenPublic_isVisibleToAnyUser() {
    UUID creatorId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Lab lab = buildChallenge(labId, creator, LabStatusEnum.PUBLIC);

    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));

    Lab result = labService.getChallenge(otherId, labId);

    assertThat(result).isSameAs(lab);
    verifyNoInteractions(courseLabRepository);
  }

  @Test
  void getChallenge_whenNotFound_throwsChallengeNotFoundException() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    when(labRepository.findById(labId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> labService.getChallenge(userId, labId))
        .isInstanceOf(LabNotFoundException.class);
  }

  @Test
  void updateChallenge_whenStatusUnchanged_savesWithoutCleanup() {
    UUID creatorId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Lab lab = buildChallenge(labId, creator, LabStatusEnum.PUBLIC);

    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));
    when(labRepository.save(any(Lab.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UpdateLabRequest request =
        updateRequest(
            "Updated",
            "Updated short",
            "Updated desc",
            LabStatusEnum.PUBLIC,
            LabDifficultyEnum.EASY,
            "ghcr.io/pm4-istp/updated:1.0");

    Lab updated = labService.updateChallenge(creatorId, labId, request);

    assertThat(updated.getTitle()).isEqualTo("Updated");
    assertThat(updated.getShortDescription()).isEqualTo("Updated short");
    assertThat(updated.getDescription()).isEqualTo("Updated desc");
    assertThat(updated.getStatus()).isEqualTo(LabStatusEnum.PUBLIC);
    assertThat(updated.getDifficulty()).isEqualTo(LabDifficultyEnum.EASY);
    assertThat(updated.getDockerImage()).isEqualTo("ghcr.io/pm4-istp/updated:1.0");
    verify(dockerImageAvailabilityService).assertImageExists("ghcr.io/pm4-istp/updated:1.0");
    verify(courseLabRepository, never()).deleteByChallengeId(any());
    verify(courseLabRepository, never())
        .deleteByChallengeIdWhereCreatorNotInstructor(any(), any());
  }

  @Test
  void updateChallenge_reusesChallengesByIdAndReindexes() {
    UUID creatorId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Lab lab = buildChallenge(labId, creator, LabStatusEnum.PUBLIC);

    UUID existingId = UUID.randomUUID();
    Challenge existing = new Challenge();
    existing.setId(existingId);
    existing.setLab(lab);
    existing.setTitle("Old");
    existing.setDescription("Old desc");
    existing.setOrderIndex(0);
    lab.getChallenges().add(existing);

    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));
    when(labRepository.save(any(Lab.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UpdateLabRequest request =
        new UpdateLabRequest(
            "T",
            "S",
            "D",
            LabStatusEnum.PUBLIC,
            LabDifficultyEnum.EASY,
            DEFAULT_DOCKER_IMAGE,
            new ArrayList<>(
                List.of(
                    new ChallengeRequest(null, "New first", "desc", null, 0, ChallengeType.FLAG, 1, null, null),
                    new ChallengeRequest(existingId, "Renamed", "updated desc", "ISTP{x}", 1, ChallengeType.FLAG, 1, null, null))));

    Lab updated = labService.updateChallenge(creatorId, labId, request);

    assertThat(updated.getChallenges()).hasSize(2);
    assertThat(updated.getChallenges().get(0).getId()).isNull();
    assertThat(updated.getChallenges().get(0).getTitle()).isEqualTo("New first");
    assertThat(updated.getChallenges().get(1).getId()).isEqualTo(existingId);
    assertThat(updated.getChallenges().get(1).getTitle()).isEqualTo("Renamed");
    assertThat(updated.getChallenges().get(1).getFlag()).isEqualTo("ISTP{x}");
    assertThat(updated.getChallenges().get(1).getOrderIndex()).isEqualTo(1);
    assertThat(updated.getMaxScore()).isEqualTo(2);
  }

  @Test
  void updateChallenge_whenDowngradedToDraft_deletesAllCourseAssignments() {
    UUID creatorId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Lab lab = buildChallenge(labId, creator, LabStatusEnum.PUBLIC);

    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));
    when(labRepository.save(any(Lab.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UpdateLabRequest request =
        updateRequest(
            "Title",
            "Short",
            "Desc",
            LabStatusEnum.DRAFT,
            LabDifficultyEnum.MEDIUM,
            DEFAULT_DOCKER_IMAGE);

    labService.updateChallenge(creatorId, labId, request);

    verify(courseLabRepository).deleteByChallengeId(labId);
    verify(courseLabRepository, never())
        .deleteByChallengeIdWhereCreatorNotInstructor(any(), any());
  }

  @Test
  void updateChallenge_whenDowngradedFromPublicToPrivate_deletesCoursesWhereCreatorIsNotInstructor() {
    UUID creatorId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Lab lab = buildChallenge(labId, creator, LabStatusEnum.PUBLIC);

    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));
    when(labRepository.save(any(Lab.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UpdateLabRequest request =
        updateRequest(
            "Title",
            "Short",
            "Desc",
            LabStatusEnum.PRIVATE,
            LabDifficultyEnum.MEDIUM,
            DEFAULT_DOCKER_IMAGE);

    labService.updateChallenge(creatorId, labId, request);

    verify(courseLabRepository)
        .deleteByChallengeIdWhereCreatorNotInstructor(labId, creatorId);
    verify(courseLabRepository, never()).deleteByChallengeId(any());
  }

  @Test
  void updateChallenge_whenPromotedFromPrivateToPublic_doesNotCleanUpAssignments() {
    UUID creatorId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Lab lab = buildChallenge(labId, creator, LabStatusEnum.PRIVATE);

    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));
    when(labRepository.save(any(Lab.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UpdateLabRequest request =
        updateRequest(
            "Title",
            "Short",
            "Desc",
            LabStatusEnum.PUBLIC,
            LabDifficultyEnum.MEDIUM,
            DEFAULT_DOCKER_IMAGE);

    labService.updateChallenge(creatorId, labId, request);

    verify(courseLabRepository, never()).deleteByChallengeId(any());
    verify(courseLabRepository, never())
        .deleteByChallengeIdWhereCreatorNotInstructor(any(), any());
  }

  @Test
  void updateChallenge_whenCallerIsNotCreator_throwsAccessDenied() {
    UUID creatorId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Lab lab = buildChallenge(labId, creator, LabStatusEnum.PUBLIC);

    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));

    UpdateLabRequest request =
        updateRequest(
            "Title",
            "Short",
            "Desc",
            LabStatusEnum.PRIVATE,
            LabDifficultyEnum.MEDIUM,
            DEFAULT_DOCKER_IMAGE);

    assertThatThrownBy(() -> labService.updateChallenge(otherId, labId, request))
        .isInstanceOf(LabAccessDeniedException.class);

    verify(labRepository, never()).save(any(Lab.class));
    verify(courseLabRepository, never()).deleteByChallengeId(any());
  }

  @Test
  void updateChallenge_whenChallengeNotFound_throwsNotFound() {
    UUID creatorId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();

    when(labRepository.findById(labId)).thenReturn(Optional.empty());

    UpdateLabRequest request =
        updateRequest(
            "Title",
            "Short",
            "Desc",
            LabStatusEnum.PUBLIC,
            LabDifficultyEnum.MEDIUM,
            DEFAULT_DOCKER_IMAGE);

    assertThatThrownBy(() -> labService.updateChallenge(creatorId, labId, request))
        .isInstanceOf(LabNotFoundException.class);

    verify(labRepository, never()).save(any(Lab.class));
  }

  @Test
  void previewVisibilityImpact_whenStatusUnchanged_returnsZero() {
    UUID creatorId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Lab lab = buildChallenge(labId, creator, LabStatusEnum.PUBLIC);

    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));

    int impact =
        labService.previewVisibilityImpact(
            creatorId, labId, LabStatusEnum.PUBLIC);

    assertThat(impact).isZero();
    verify(courseLabRepository, never()).countByChallengeId(any());
    verify(courseLabRepository, never())
        .countByChallengeIdWhereCreatorNotInstructor(any(), any());
  }

  @Test
  void previewVisibilityImpact_whenDowngradingToDraft_returnsTotalCount() {
    UUID creatorId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Lab lab = buildChallenge(labId, creator, LabStatusEnum.PUBLIC);

    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));
    when(courseLabRepository.countByChallengeId(labId)).thenReturn(3L);

    int impact =
        labService.previewVisibilityImpact(
            creatorId, labId, LabStatusEnum.DRAFT);

    assertThat(impact).isEqualTo(3);
  }

  @Test
  void previewVisibilityImpact_whenDowngradingFromPublicToPrivate_returnsCreatorNotInstructorCount() {
    UUID creatorId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Lab lab = buildChallenge(labId, creator, LabStatusEnum.PUBLIC);

    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));
    when(courseLabRepository.countByChallengeIdWhereCreatorNotInstructor(
            labId, creatorId))
        .thenReturn(2L);

    int impact =
        labService.previewVisibilityImpact(
            creatorId, labId, LabStatusEnum.PRIVATE);

    assertThat(impact).isEqualTo(2);
  }

  @Test
  void previewVisibilityImpact_whenPromoting_returnsZero() {
    UUID creatorId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Lab lab = buildChallenge(labId, creator, LabStatusEnum.PRIVATE);

    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));

    int impact =
        labService.previewVisibilityImpact(
            creatorId, labId, LabStatusEnum.PUBLIC);

    assertThat(impact).isZero();
    verify(courseLabRepository, never()).countByChallengeId(any());
    verify(courseLabRepository, never())
        .countByChallengeIdWhereCreatorNotInstructor(any(), any());
  }

  @Test
  void previewVisibilityImpact_whenCallerIsNotCreator_throwsAccessDenied() {
    UUID creatorId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Lab lab = buildChallenge(labId, creator, LabStatusEnum.PUBLIC);

    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));

    assertThatThrownBy(
            () ->
                labService.previewVisibilityImpact(
                    otherId, labId, LabStatusEnum.PRIVATE))
        .isInstanceOf(LabAccessDeniedException.class);
  }

  @Test
  void previewVisibilityImpact_whenChallengeNotFound_throwsNotFound() {
    UUID creatorId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();

    when(labRepository.findById(labId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                labService.previewVisibilityImpact(
                    creatorId, labId, LabStatusEnum.DRAFT))
        .isInstanceOf(LabNotFoundException.class);
  }

  @Test
  void deleteChallenge_whenCallerIsCreator_deletesChallenge() {
    UUID creatorId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Lab lab = buildChallenge(labId, creator, LabStatusEnum.DRAFT);

    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));

    labService.deleteChallenge(creatorId, labId);

    verify(labRepository).delete(lab);
  }

  @Test
  void deleteChallenge_whenCallerIsNotCreator_throwsAccessDenied() {
    UUID creatorId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    User creator = buildUser(creatorId);
    Lab lab = buildChallenge(labId, creator, LabStatusEnum.DRAFT);

    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));

    assertThatThrownBy(() -> labService.deleteChallenge(otherId, labId))
        .isInstanceOf(LabAccessDeniedException.class);

    verify(labRepository, never()).delete(any(Lab.class));
  }

  @Test
  void deleteChallenge_whenChallengeNotFound_throwsNotFound() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();

    when(labRepository.findById(labId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> labService.deleteChallenge(userId, labId))
        .isInstanceOf(LabNotFoundException.class);
  }

  @Test
  void listChallengesForCreator_delegatesToRepository() {
    UUID creatorId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 10);
    Page<ListLabResponseDto> expected = new PageImpl<>(List.of());

    when(labRepository.findListChallengesForCreator(creatorId, pageable))
        .thenReturn(expected);

    Page<ListLabResponseDto> result =
        labService.listChallengesForCreator(creatorId, pageable);

    assertThat(result).isSameAs(expected);
    verify(labRepository).findListChallengesForCreator(creatorId, pageable);
  }

  @Test
  void searchAvailableChallenges_delegatesToRepository() {
    UUID userId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 10);
    Page<ListLabResponseDto> expected = new PageImpl<>(List.of());

    when(labRepository.searchAvailableChallenges(userId, "sql", pageable))
        .thenReturn(expected);

    Page<ListLabResponseDto> result =
        labService.searchAvailableChallenges(userId, "sql", pageable);

    assertThat(result).isSameAs(expected);
    verify(labRepository).searchAvailableChallenges(userId, "sql", pageable);
  }

  // ── getChallengeForPlay ────────────────────────────────────────────────────

  private Lab buildChallengeWithCourses(UUID labId, UUID... courseIds) {
    Lab lab = buildChallenge(labId, buildUser(UUID.randomUUID()),
        LabStatusEnum.PUBLIC);
    List<CourseLab> ccs = new ArrayList<>();
    for (UUID courseId : courseIds) {
      Course course = new Course();
      course.setId(courseId);
      CourseLab cc = new CourseLab();
      cc.setCourse(course);
      cc.setLab(lab);
      ccs.add(cc);
    }
    lab.setCourseLabs(ccs);
    return lab;
  }

  @Test
  void getChallengeForPlay_returnsStudentDto_whenEnrolledAndChallengeBelongsToCourse() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Lab lab = buildChallengeWithCourses(labId, courseId);

    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
        .thenReturn(true);
    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));
    LabStudentDto dto = new LabStudentDto();
    dto.setId(labId);
    when(labMapper.toStudentDto(lab)).thenReturn(dto);

    // Stub the course lookup used to set mcAttemptsMode on the DTO
    Course course = new Course();
    course.setId(courseId);
    course.setMcAttemptsMode(McAttemptsMode.UNLIMITED);
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    LabStudentDto result = labService.getChallengeForPlay(userId, courseId, labId);

    assertThat(result).isSameAs(dto);
    assertThat(result.getMcAttemptsMode()).isEqualTo("UNLIMITED");
  }

  @Test
  void getChallengeForPlay_whenNotEnrolled_throwsAccessDenied() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();

    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
        .thenReturn(false);

    assertThatThrownBy(() -> labService.getChallengeForPlay(userId, courseId, labId))
        .isInstanceOf(LabAccessDeniedException.class);
    verify(labRepository, never()).findById(any());
  }

  @Test
  void getChallengeForPlay_whenChallengeMissing_throwsNotFound() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();

    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
        .thenReturn(true);
    when(labRepository.findById(labId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> labService.getChallengeForPlay(userId, courseId, labId))
        .isInstanceOf(LabNotFoundException.class);
  }

  @Test
  void getChallengeForPlay_whenChallengeNotInRequestedCourse_throwsAccessDenied() {
    // Regression test for the authz-bypass: even if the user is enrolled in a course
    // and the lab exists in some *other* course, the requested courseId must
    // actually contain the lab.
    UUID userId = UUID.randomUUID();
    UUID requestedCourseId = UUID.randomUUID();
    UUID otherCourseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Lab lab = buildChallengeWithCourses(labId, otherCourseId);

    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(requestedCourseId, userId))
        .thenReturn(true);
    when(labRepository.findById(labId)).thenReturn(Optional.of(lab));

    assertThatThrownBy(
        () -> labService.getChallengeForPlay(userId, requestedCourseId, labId))
        .isInstanceOf(LabAccessDeniedException.class)
        .hasMessageContaining("not part of course");
  }

  // ── submitChallengeFlag ──────────────────────────────────────────────────────

  private Challenge buildChallenge(UUID id, Lab parent, String flag) {
    Challenge st = new Challenge();
    st.setId(id);
    st.setLab(parent);
    st.setFlag(flag);
    parent.getChallenges().add(st);
    return st;
  }

  @Test
  void submitChallengeFlag_returnsCorrectAndPersists_whenFlagMatches() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    User user = buildUser(userId);
    Lab lab = buildChallengeWithCourses(labId, courseId);
    Challenge challenge = buildChallenge(challengeId, lab, "ISTP{secret}");

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(courseLabRepository.existsByChallengeIdAndEnrolledUserId(labId, userId))
        .thenReturn(true);
    when(challengeCompletionRepository.existsByUserIdAndChallengeId(userId, challengeId))
        .thenReturn(false);
    when(challengeCompletionRepository.findSolvedChallengeIds(eq(userId), any()))
        .thenReturn(List.of(challengeId));

    ChallengeSubmissionResponseDto result =
        labService.submitChallengeFlag(userId, labId, challengeId, "ISTP{secret}");

    assertThat(result.isCorrect()).isTrue();
    assertThat(result.isChallengeSolved()).isTrue();
    verify(challengeCompletionRepository).saveAndFlush(any(ChallengeCompletion.class));
  }

  @Test
  void submitChallengeFlag_returnsIncorrect_andDoesNotPersist_whenFlagMismatches() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    User user = buildUser(userId);
    Lab lab = buildChallengeWithCourses(labId, courseId);
    Challenge challenge = buildChallenge(challengeId, lab, "ISTP{secret}");

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(courseLabRepository.existsByChallengeIdAndEnrolledUserId(labId, userId))
        .thenReturn(true);
    when(challengeCompletionRepository.existsByUserIdAndChallengeId(userId, challengeId))
        .thenReturn(false);
    when(challengeCompletionRepository.findSolvedChallengeIds(eq(userId), any()))
        .thenReturn(List.of());

    ChallengeSubmissionResponseDto result =
        labService.submitChallengeFlag(userId, labId, challengeId, "ISTP{wrong}");

    assertThat(result.isCorrect()).isFalse();
    verify(challengeCompletionRepository, never()).saveAndFlush(any());
  }

  @Test
  void submitChallengeFlag_whenChallengeNotFound_throwsNotFound() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();

    when(userRepository.findByIdAndDeletedAtIsNull(userId))
        .thenReturn(Optional.of(buildUser(userId)));
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> labService.submitChallengeFlag(userId, labId, challengeId, "ISTP{x}"))
        .isInstanceOf(ChallengeNotFoundException.class);
  }

  @Test
  void submitChallengeFlag_whenChallengeBelongsToOtherChallenge_throwsNotFound() {
    UUID userId = UUID.randomUUID();
    UUID requestedChallengeId = UUID.randomUUID();
    UUID actualChallengeId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    Lab actual = buildChallengeWithCourses(actualChallengeId, courseId);
    Challenge challenge = buildChallenge(challengeId, actual, "ISTP{secret}");

    when(userRepository.findByIdAndDeletedAtIsNull(userId))
        .thenReturn(Optional.of(buildUser(userId)));
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

    assertThatThrownBy(
        () -> labService.submitChallengeFlag(
            userId, requestedChallengeId, challengeId, "ISTP{secret}"))
        .isInstanceOf(ChallengeNotFoundException.class);
  }

  @Test
  void submitChallengeFlag_whenNotEnrolled_throwsAccessDenied() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    Lab lab = buildChallengeWithCourses(labId, courseId);
    Challenge challenge = buildChallenge(challengeId, lab, "ISTP{secret}");

    when(userRepository.findByIdAndDeletedAtIsNull(userId))
        .thenReturn(Optional.of(buildUser(userId)));
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(courseLabRepository.existsByChallengeIdAndEnrolledUserId(labId, userId))
        .thenReturn(false);

    assertThatThrownBy(
        () -> labService.submitChallengeFlag(userId, labId, challengeId, "ISTP{secret}"))
        .isInstanceOf(LabAccessDeniedException.class);
  }

  @Test
  void submitChallengeFlag_whenAlreadySolved_throws409() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    Lab lab = buildChallengeWithCourses(labId, courseId);
    Challenge challenge = buildChallenge(challengeId, lab, "ISTP{secret}");

    when(userRepository.findByIdAndDeletedAtIsNull(userId))
        .thenReturn(Optional.of(buildUser(userId)));
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(courseLabRepository.existsByChallengeIdAndEnrolledUserId(labId, userId))
        .thenReturn(true);
    when(challengeCompletionRepository.existsByUserIdAndChallengeId(userId, challengeId))
        .thenReturn(true);

    assertThatThrownBy(
        () -> labService.submitChallengeFlag(userId, labId, challengeId, "ISTP{secret}"))
        .isInstanceOf(ChallengeAlreadySolvedException.class);
  }

  @Test
  void submitChallengeFlag_whenConcurrentInsertHitsUniqueConstraint_throws409() {
    // Race condition: two concurrent correct submissions both pass the
    // existsByUserIdAndChallengeId check, then one trips the unique constraint
    // on insert. We must surface that as 409, not 500.
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    Lab lab = buildChallengeWithCourses(labId, courseId);
    Challenge challenge = buildChallenge(challengeId, lab, "ISTP{secret}");

    when(userRepository.findByIdAndDeletedAtIsNull(userId))
        .thenReturn(Optional.of(buildUser(userId)));
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(courseLabRepository.existsByChallengeIdAndEnrolledUserId(labId, userId))
        .thenReturn(true);
    when(challengeCompletionRepository.existsByUserIdAndChallengeId(userId, challengeId))
        .thenReturn(false);
    when(challengeCompletionRepository.saveAndFlush(any(ChallengeCompletion.class)))
        .thenThrow(new DataIntegrityViolationException("unique violation"));

    assertThatThrownBy(
        () -> labService.submitChallengeFlag(userId, labId, challengeId, "ISTP{secret}"))
        .isInstanceOf(ChallengeAlreadySolvedException.class);
  }

  private ChallengeOption option(UUID id, Challenge challenge, String text, boolean correct) {
    ChallengeOption option = new ChallengeOption();
    option.setId(id);
    option.setChallenge(challenge);
    option.setText(text);
    option.setCorrect(correct);
    challenge.getOptions().add(option);
    return option;
  }

  @Test
  void submitChallengeChoice_onceMode_persistsWrongAnswerAndRevealsCorrectOption() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID wrongOptionId = UUID.randomUUID();
    UUID correctOptionId = UUID.randomUUID();
    User user = buildUser(userId);
    Lab lab = buildChallengeWithCourses(labId, courseId);
    Challenge challenge = buildChallenge(challengeId, lab, null);
    challenge.setType(ChallengeType.MULTIPLE_CHOICE);
    option(wrongOptionId, challenge, "Nope", false);
    option(correctOptionId, challenge, "Yep", true);
    Course course = new Course();
    course.setId(courseId);
    course.setMcAttemptsMode(McAttemptsMode.ONCE);

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(courseLabRepository.existsByChallengeIdAndEnrolledUserId(labId, userId)).thenReturn(true);
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(studentOptionSubmissionRepository.findByUserIdAndChallengeId(userId, challengeId))
        .thenReturn(Optional.empty());
    when(challengeCompletionRepository.findSolvedChallengeIds(eq(userId), any())).thenReturn(List.of());

    ChoiceSubmissionResponseDto response =
        labService.submitChallengeChoice(userId, courseId, labId, challengeId, wrongOptionId);

    assertThat(response.isCorrect()).isFalse();
    assertThat(response.getCorrectOptionId()).isEqualTo(correctOptionId);
    verify(studentOptionSubmissionRepository).saveAndFlush(any(StudentOptionSubmission.class));
    verify(challengeCompletionRepository).saveAndFlush(any(ChallengeCompletion.class));
  }

  @Test
  void submitChallengeChoice_unlimitedWrongAnswer_doesNotPersistAttempt() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID wrongOptionId = UUID.randomUUID();
    UUID correctOptionId = UUID.randomUUID();
    Lab lab = buildChallengeWithCourses(labId, courseId);
    Challenge challenge = buildChallenge(challengeId, lab, null);
    challenge.setType(ChallengeType.MULTIPLE_CHOICE);
    option(wrongOptionId, challenge, "Nope", false);
    option(correctOptionId, challenge, "Yep", true);
    Course course = new Course();
    course.setId(courseId);
    course.setMcAttemptsMode(McAttemptsMode.UNLIMITED);

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(buildUser(userId)));
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(courseLabRepository.existsByChallengeIdAndEnrolledUserId(labId, userId)).thenReturn(true);
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(studentOptionSubmissionRepository.findByUserIdAndChallengeId(userId, challengeId))
        .thenReturn(Optional.empty());
    when(challengeCompletionRepository.findSolvedChallengeIds(eq(userId), any())).thenReturn(List.of());

    ChoiceSubmissionResponseDto response =
        labService.submitChallengeChoice(userId, courseId, labId, challengeId, wrongOptionId);

    assertThat(response.isCorrect()).isFalse();
    assertThat(response.getCorrectOptionId()).isEqualTo(correctOptionId);
    verify(studentOptionSubmissionRepository, never()).saveAndFlush(any());
    verify(challengeCompletionRepository, never()).saveAndFlush(any());
  }

  @Test
  void submitChallengeChoice_correctUnlimited_savesSubmissionCompletionAndAwardsWhenLabSolved() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID correctOptionId = UUID.randomUUID();
    User user = buildUser(userId);
    Lab lab = buildChallengeWithCourses(labId, courseId);
    Challenge challenge = buildChallenge(challengeId, lab, null);
    challenge.setType(ChallengeType.MULTIPLE_CHOICE);
    option(correctOptionId, challenge, "Yep", true);
    Course course = new Course();
    course.setId(courseId);

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(courseLabRepository.existsByChallengeIdAndEnrolledUserId(labId, userId)).thenReturn(true);
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(studentOptionSubmissionRepository.findByUserIdAndChallengeId(userId, challengeId))
        .thenReturn(Optional.empty());
    when(challengeCompletionRepository.existsByUserIdAndChallengeId(userId, challengeId))
        .thenReturn(false);
    when(challengeCompletionRepository.findSolvedChallengeIds(eq(userId), any()))
        .thenReturn(List.of(challengeId));

    ChoiceSubmissionResponseDto response =
        labService.submitChallengeChoice(userId, courseId, labId, challengeId, correctOptionId);

    assertThat(response.isCorrect()).isTrue();
    assertThat(response.isChallengeSolved()).isTrue();
    verify(studentOptionSubmissionRepository).saveAndFlush(any(StudentOptionSubmission.class));
    verify(challengeCompletionRepository).saveAndFlush(any(ChallengeCompletion.class));
    verify(badgeService).tryAwardBadgesForChallenge(userId, labId);
  }

  @Test
  void submitChallengeChoice_existingWrongSubmission_returnsStoredResult() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID wrongOptionId = UUID.randomUUID();
    UUID correctOptionId = UUID.randomUUID();
    Lab lab = buildChallengeWithCourses(labId, courseId);
    Challenge challenge = buildChallenge(challengeId, lab, null);
    challenge.setType(ChallengeType.MULTIPLE_CHOICE);
    ChallengeOption wrong = option(wrongOptionId, challenge, "Nope", false);
    option(correctOptionId, challenge, "Yep", true);
    StudentOptionSubmission submission = new StudentOptionSubmission();
    submission.setSelectedOption(wrong);
    submission.setCorrect(false);
    Course course = new Course();
    course.setId(courseId);
    course.setMcAttemptsMode(McAttemptsMode.ONCE);

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(buildUser(userId)));
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(courseLabRepository.existsByChallengeIdAndEnrolledUserId(labId, userId)).thenReturn(true);
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(studentOptionSubmissionRepository.findByUserIdAndChallengeId(userId, challengeId))
        .thenReturn(Optional.of(submission));
    when(challengeCompletionRepository.findSolvedChallengeIds(eq(userId), any())).thenReturn(List.of());

    ChoiceSubmissionResponseDto response =
        labService.submitChallengeChoice(userId, courseId, labId, challengeId, wrongOptionId);

    assertThat(response.isCorrect()).isFalse();
    assertThat(response.getCorrectOptionId()).isEqualTo(correctOptionId);
    verify(studentOptionSubmissionRepository, never()).saveAndFlush(any());
  }

  @Test
  void completeTheoryChallenge_savesCompletionAndAwardsBadgeWhenAllSolved() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    User user = buildUser(userId);
    Lab lab = buildChallengeWithCourses(labId, courseId);
    Challenge challenge = buildChallenge(challengeId, lab, null);
    challenge.setType(ChallengeType.FLAG);

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
    when(courseLabRepository.existsByChallengeIdAndEnrolledUserId(labId, userId)).thenReturn(true);
    when(challengeCompletionRepository.existsByUserIdAndChallengeId(userId, challengeId))
        .thenReturn(false);
    when(challengeCompletionRepository.findSolvedChallengeIds(eq(userId), any()))
        .thenReturn(List.of(challengeId));

    ChallengeSubmissionResponseDto response =
        labService.completeTheoryChallenge(userId, labId, challengeId);

    assertThat(response.isCorrect()).isTrue();
    assertThat(response.isChallengeSolved()).isTrue();
    verify(challengeCompletionRepository).saveAndFlush(any(ChallengeCompletion.class));
    verify(badgeService).tryAwardBadgesForChallenge(userId, labId);
  }

  @Test
  void completeTheoryChallenge_rejectsFlagBasedChallenge() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    Lab lab = buildChallengeWithCourses(labId, courseId);
    Challenge challenge = buildChallenge(challengeId, lab, "ISTP{flag}");

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(buildUser(userId)));
    when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));

    assertThatThrownBy(() -> labService.completeTheoryChallenge(userId, labId, challengeId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requires a flag submission");
  }
}
