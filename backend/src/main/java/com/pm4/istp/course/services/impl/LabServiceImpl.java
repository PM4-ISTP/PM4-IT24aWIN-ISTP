package com.pm4.istp.course.services.impl;

import com.pm4.istp.badge.services.BadgeService;
import com.pm4.istp.course.db.ChallengeOptionRequest;
import com.pm4.istp.course.db.ChallengeRequest;
import com.pm4.istp.course.db.CreateLabRequest;
import com.pm4.istp.course.db.UpdateLabRequest;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.ChallengeCompletion;
import com.pm4.istp.course.db.entities.ChallengeOption;
import com.pm4.istp.course.db.entities.ChallengeType;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.db.entities.CourseLab;
import com.pm4.istp.course.db.entities.Lab;
import com.pm4.istp.course.db.entities.LabStatusEnum;
import com.pm4.istp.course.db.entities.McAttemptsMode;
import com.pm4.istp.course.db.entities.StudentFlagSubmission;
import com.pm4.istp.course.db.entities.StudentOptionSubmission;
import com.pm4.istp.course.dto.ChallengeStudentDto;
import com.pm4.istp.course.dto.ChallengeSubmissionResponseDto;
import com.pm4.istp.course.dto.ChoiceSubmissionResponseDto;
import com.pm4.istp.course.dto.LabStudentDto;
import com.pm4.istp.course.dto.ListLabResponseDto;
import com.pm4.istp.course.exceptions.ChallengeAlreadySolvedException;
import com.pm4.istp.course.exceptions.ChallengeNotFoundException;
import com.pm4.istp.course.exceptions.CourseNotFoundException;
import com.pm4.istp.course.exceptions.LabAccessDeniedException;
import com.pm4.istp.course.exceptions.LabNotFoundException;
import com.pm4.istp.course.exceptions.LabSubmissionClosedException;
import com.pm4.istp.course.mappers.LabMapper;
import com.pm4.istp.course.repositories.ChallengeCompletionRepository;
import com.pm4.istp.course.repositories.ChallengeOptionRepository;
import com.pm4.istp.course.repositories.ChallengeRepository;
import com.pm4.istp.course.repositories.CourseChallengeScoreOverrideRepository;
import com.pm4.istp.course.repositories.CourseEnrollmentRepository;
import com.pm4.istp.course.repositories.CourseLabRepository;
import com.pm4.istp.course.repositories.CourseRepository;
import com.pm4.istp.course.repositories.LabRepository;
import com.pm4.istp.course.repositories.StudentFlagSubmissionRepository;
import com.pm4.istp.course.repositories.StudentOptionSubmissionRepository;
import com.pm4.istp.course.services.DockerImageAvailabilityService;
import com.pm4.istp.course.services.LabService;
import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.exceptions.UserNotFoundException;
import com.pm4.istp.user.repositories.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabServiceImpl implements LabService {

  private static final String USER_NOT_FOUND_MSG = "User with ID '%s' not found";
  private static final String LAB_NOT_FOUND_MSG = "Lab with ID '%s' not found";
  private static final String CHALLENGE_NOT_FOUND_MSG = "Challenge with ID '%s' not found";
  private static final String CHALLENGE_NOT_IN_LAB_MSG =
      "Challenge '%s' does not belong to lab '%s'";

  private static final String COURSE_NOT_FOUND_MSG = "Course with ID '%s' not found";

  private final UserRepository userRepository;
  private final LabRepository labRepository;
  private final CourseLabRepository courseLabRepository;
  private final CourseRepository courseRepository;
  private final ChallengeRepository challengeRepository;
  private final ChallengeOptionRepository challengeOptionRepository;
  private final ChallengeCompletionRepository challengeCompletionRepository;
  private final StudentOptionSubmissionRepository studentOptionSubmissionRepository;
  private final StudentFlagSubmissionRepository studentFlagSubmissionRepository;
  private final CourseChallengeScoreOverrideRepository courseChallengeScoreOverrideRepository;
  private final CourseEnrollmentRepository courseEnrollmentRepository;
  private final LabMapper labMapper;
  private final DockerImageAvailabilityService dockerImageAvailabilityService;
  private final BadgeService badgeService;

  @Override
  @Transactional
  public Lab createLab(UUID userId, CreateLabRequest request) {
    User creator =
        userRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(
                () -> new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId)));

    Lab lab = new Lab();
    dockerImageAvailabilityService.assertImageExists(request.getDockerImage());

    lab.setTitle(request.getTitle());
    lab.setDescription(request.getDescription());
    lab.setStatus(request.getStatus());
    lab.setDifficulty(request.getDifficulty());
    lab.setDockerImage(request.getDockerImage());
    lab.setContainerPort(resolveContainerPort(request.getContainerPort()));
    lab.setPodTtlSeconds(request.getPodTtlSeconds());
    lab.setCreator(creator);

    List<Challenge> challenges = buildChallengesForCreate(request.getChallenges(), lab);
    lab.getChallenges().addAll(challenges);
    lab.setMaxScore(totalPoints(lab.getChallenges()));

    return labRepository.save(lab);
  }

  @Override
  @Transactional(readOnly = true)
  public Lab getLab(UUID userId, UUID labId) {
    Lab lab =
        labRepository
            .findByIdAndDeletedAtIsNull(labId)
            .orElseThrow(() -> new LabNotFoundException(String.format(LAB_NOT_FOUND_MSG, labId)));

    verifyVisibility(lab, userId);
    return lab;
  }

  @Override
  @Transactional
  public Lab updateLab(UUID userId, UUID labId, UpdateLabRequest request) {
    Lab lab =
        labRepository
            .findByIdAndDeletedAtIsNull(labId)
            .orElseThrow(() -> new LabNotFoundException(String.format(LAB_NOT_FOUND_MSG, labId)));

    verifyCreator(lab, userId);

    LabStatusEnum oldStatus = lab.getStatus();
    LabStatusEnum newStatus = request.getStatus();
    dockerImageAvailabilityService.assertImageExists(request.getDockerImage());

    lab.setTitle(request.getTitle());
    lab.setDescription(request.getDescription());
    lab.setStatus(newStatus);
    lab.setDifficulty(request.getDifficulty());
    lab.setDockerImage(request.getDockerImage());
    lab.setContainerPort(resolveContainerPort(request.getContainerPort()));
    lab.setPodTtlSeconds(request.getPodTtlSeconds());

    applyChallengeUpdates(lab, request.getChallenges());
    lab.setMaxScore(totalPoints(lab.getChallenges()));

    Lab saved = labRepository.save(lab);
    cleanupCourseChallengesForVisibilityChange(labId, userId, oldStatus, newStatus);

    return saved;
  }

  // -------------------------------------------------------------------------
  // Challenge builders
  // -------------------------------------------------------------------------

  private List<Challenge> buildChallengesForCreate(List<ChallengeRequest> requests, Lab parent) {
    List<Challenge> result = new ArrayList<>();
    if (requests == null) {
      return result;
    }
    int idx = 0;
    for (ChallengeRequest req : requests) {
      Challenge st = new Challenge();
      st.setLab(parent);
      st.setTitle(req.getTitle());
      st.setDescription(req.getDescription());
      st.setFlag(normalizeFlag(req.getFlag()));
      st.setOrderIndex(idx++);
      st.setType(req.getType() != null ? req.getType() : ChallengeType.FLAG);
      st.setPoints(req.getPoints());
      st.setHint(req.getHint());
      applyOptions(st, req.getOptions());
      result.add(st);
    }
    return result;
  }

  private int resolveContainerPort(Integer containerPort) {
    return containerPort != null ? containerPort : Lab.DEFAULT_CONTAINER_PORT;
  }

  private void applyChallengeUpdates(Lab lab, List<ChallengeRequest> requests) {
    List<ChallengeRequest> incoming = requests == null ? List.of() : requests;

    Map<UUID, Challenge> existingById = new HashMap<>();
    for (Challenge existing : lab.getChallenges()) {
      if (existing.getId() != null) {
        existingById.put(existing.getId(), existing);
      }
    }

    List<Challenge> retained = new ArrayList<>();
    int idx = 0;
    for (ChallengeRequest req : incoming) {
      Challenge target = req.getId() != null ? existingById.remove(req.getId()) : null;
      if (target == null) {
        target = new Challenge();
        target.setLab(lab);
      }
      target.setTitle(req.getTitle());
      target.setDescription(req.getDescription());
      target.setFlag(normalizeFlag(req.getFlag()));
      target.setOrderIndex(idx++);
      target.setType(req.getType() != null ? req.getType() : ChallengeType.FLAG);
      target.setPoints(req.getPoints());
      target.setHint(req.getHint());
      applyOptions(target, req.getOptions());
      retained.add(target);
    }

    lab.getChallenges().clear();
    lab.getChallenges().addAll(retained);
  }

  private void applyOptions(Challenge challenge, List<ChallengeOptionRequest> optionRequests) {
    challenge.getOptions().clear();
    if (optionRequests == null || optionRequests.isEmpty()) {
      return;
    }
    int idx = 0;
    for (ChallengeOptionRequest req : optionRequests) {
      ChallengeOption opt = new ChallengeOption();
      opt.setChallenge(challenge);
      opt.setText(req.getText());
      opt.setCorrect(req.isCorrect());
      opt.setOrderIndex(idx++);
      challenge.getOptions().add(opt);
    }
  }

  private int totalPoints(List<Challenge> challenges) {
    int sum = 0;
    for (Challenge st : challenges) {
      sum += st.getPoints();
    }
    return sum;
  }

  private String normalizeFlag(String flag) {
    if (flag == null) {
      return null;
    }
    String trimmed = flag.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  // -------------------------------------------------------------------------
  // Visibility / access helpers
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public int previewVisibilityImpact(UUID userId, UUID labId, LabStatusEnum newStatus) {
    Lab lab =
        labRepository
            .findByIdAndDeletedAtIsNull(labId)
            .orElseThrow(() -> new LabNotFoundException(String.format(LAB_NOT_FOUND_MSG, labId)));

    verifyCreator(lab, userId);

    return countAffectedCourses(labId, userId, lab.getStatus(), newStatus);
  }

  private int countAffectedCourses(
      UUID labId, UUID creatorId, LabStatusEnum oldStatus, LabStatusEnum newStatus) {
    if (oldStatus == newStatus) {
      return 0;
    }
    if (newStatus == LabStatusEnum.DRAFT) {
      return (int) courseLabRepository.countByChallengeId(labId);
    }
    if (newStatus == LabStatusEnum.PRIVATE && oldStatus == LabStatusEnum.PUBLIC) {
      return (int)
          courseLabRepository.countByChallengeIdWhereCreatorNotInstructor(labId, creatorId);
    }
    return 0;
  }

  private void cleanupCourseChallengesForVisibilityChange(
      UUID labId, UUID creatorId, LabStatusEnum oldStatus, LabStatusEnum newStatus) {
    if (oldStatus == newStatus) {
      return;
    }
    if (newStatus == LabStatusEnum.DRAFT) {
      courseLabRepository.deleteByChallengeId(labId);
      return;
    }
    if (newStatus == LabStatusEnum.PRIVATE && oldStatus == LabStatusEnum.PUBLIC) {
      courseLabRepository.deleteByChallengeIdWhereCreatorNotInstructor(labId, creatorId);
    }
  }

  @Override
  @Transactional
  public void deleteLab(UUID userId, UUID labId) {
    Lab lab =
        labRepository
            .findByIdAndDeletedAtIsNull(labId)
            .orElseThrow(() -> new LabNotFoundException(String.format(LAB_NOT_FOUND_MSG, labId)));

    verifyCreator(lab, userId);
    courseLabRepository.deleteByChallengeId(labId);
    lab.setDeletedByUsername(
        userRepository.findByIdAndDeletedAtIsNull(userId).map(User::getUsername).orElse("unknown"));
    lab.setDeletedAt(LocalDateTime.now());
    labRepository.save(lab);
  }

  @Override
  public Page<ListLabResponseDto> listLabsForCreator(UUID creatorId, Pageable pageable) {
    return labRepository.findListChallengesForCreator(creatorId, pageable);
  }

  @Override
  public Page<ListLabResponseDto> searchAvailableLabs(
      UUID userId, String search, Pageable pageable) {
    return labRepository.searchAvailableLabs(userId, search, pageable);
  }

  private void verifyCreator(Lab lab, UUID userId) {
    if (!lab.getCreator().getId().equals(userId)) {
      throw new LabAccessDeniedException(
          String.format("User with ID '%s' is not the creator of lab '%s'", userId, lab.getId()));
    }
  }

  private void verifyVisibility(Lab lab, UUID userId) {
    if (lab.getCreator().getId().equals(userId)) {
      return;
    }

    if (lab.getStatus() == LabStatusEnum.DRAFT) {
      throw new LabAccessDeniedException(
          String.format("User with ID '%s' cannot access draft lab '%s'", userId, lab.getId()));
    }

    if (lab.getStatus() == LabStatusEnum.PRIVATE) {
      boolean isInstructorOfCourseWithChallenge =
          courseLabRepository.existsByChallengeIdAndCourseInstructorId(lab.getId(), userId);
      boolean isEnrolledInCourseWithChallenge =
          courseLabRepository.existsByChallengeIdAndEnrolledUserId(lab.getId(), userId);
      if (!isInstructorOfCourseWithChallenge && !isEnrolledInCourseWithChallenge) {
        throw new LabAccessDeniedException(
            String.format("User with ID '%s' cannot access private lab '%s'", userId, lab.getId()));
      }
    }
  }

  // -------------------------------------------------------------------------
  // Student play
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public LabStudentDto getLabForPlay(UUID userId, UUID courseId, UUID labId) {
    verifyEnrollment(userId, courseId);

    Lab lab =
        labRepository
            .findByIdAndDeletedAtIsNull(labId)
            .orElseThrow(() -> new LabNotFoundException(String.format(LAB_NOT_FOUND_MSG, labId)));

    boolean challengeBelongsToCourse =
        lab.getCourseLabs().stream().anyMatch(cc -> cc.getCourse().getId().equals(courseId));
    if (!challengeBelongsToCourse) {
      throw new LabAccessDeniedException(
          String.format("Lab '%s' is not part of course '%s'", labId, courseId));
    }

    LabStudentDto dto = labMapper.toStudentDto(lab);
    populateStudentProgress(dto, userId, lab);

    // Expose the course's MC attempt mode so the frontend can render the UI correctly
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));
    McAttemptsMode mode =
        course.getMcAttemptsMode() != null ? course.getMcAttemptsMode() : McAttemptsMode.UNLIMITED;
    dto.setMcAttemptsMode(mode.name());

    // Attach due date/time from the course assignment (if any) for student visibility.
    courseLabRepository
        .findByCourseIdAndLabId(courseId, labId)
        .ifPresent(courseLab -> dto.setDueAt(courseLab.getDueAt()));

    return dto;
  }

  // -------------------------------------------------------------------------
  // Flag submission
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public ChallengeSubmissionResponseDto submitChallengeFlag(
      UUID userId, UUID courseId, UUID labId, UUID challengeId, String flag) {
    User user = findActiveUser(userId);

    Challenge challenge =
        challengeRepository
            .findById(challengeId)
            .orElseThrow(
                () ->
                    new ChallengeNotFoundException(
                        String.format(CHALLENGE_NOT_FOUND_MSG, challengeId)));

    if (!challenge.getLab().getId().equals(labId)) {
      throw new ChallengeNotFoundException(
          String.format(CHALLENGE_NOT_IN_LAB_MSG, challengeId, labId));
    }

    verifySubmissionAllowed(userId, courseId, challenge.getLab().getId());

    if (challengeCompletionRepository.existsByUserIdAndChallengeId(userId, challengeId)) {
      throw new ChallengeAlreadySolvedException(
          String.format("Challenge '%s' already solved by user '%s'", challengeId, userId));
    }

    boolean correct = challenge.getFlag() != null && challenge.getFlag().equals(flag);

    StudentFlagSubmission submission =
        studentFlagSubmissionRepository
            .findByUserIdAndChallengeId(userId, challengeId)
            .orElseGet(StudentFlagSubmission::new);
    submission.setUser(user);
    submission.setChallenge(challenge);
    submission.setSubmittedFlag(flag);
    submission.setCorrect(correct);
    submission.setSubmittedAt(LocalDateTime.now());
    studentFlagSubmissionRepository.save(submission);

    if (correct) {
      ChallengeCompletion completion = new ChallengeCompletion();
      completion.setUser(user);
      completion.setChallenge(challenge);
      completion.setSolvedAt(LocalDateTime.now());
      try {
        challengeCompletionRepository.saveAndFlush(completion);
      } catch (DataIntegrityViolationException ex) {
        throw new ChallengeAlreadySolvedException(
            String.format("Challenge '%s' already solved by user '%s'", challengeId, userId), ex);
      }
    }

    List<Challenge> siblings = challenge.getLab().getChallenges();
    List<UUID> siblingIds = siblingsIds(siblings);
    Set<UUID> solvedIds = solvedChallengeIds(userId, siblingIds);
    int solvedCount = solvedIds.size();
    int totalCount = siblings.size();
    boolean challengeSolved = totalCount > 0 && solvedCount == totalCount;

    if (correct && challengeSolved) {
      badgeService.tryAwardBadgesForChallenge(userId, labId);
    }

    return new ChallengeSubmissionResponseDto(correct, challengeSolved, solvedCount, totalCount);
  }

  // -------------------------------------------------------------------------
  // Multiple-choice submission
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public ChoiceSubmissionResponseDto submitChallengeChoice(
      UUID userId, UUID courseId, UUID labId, UUID challengeId, UUID selectedOptionId) {
    User user = findActiveUser(userId);
    Challenge challenge = findChallengeInChallenge(challengeId, labId);
    verifySubmissionAllowed(userId, courseId, challenge.getLab().getId());

    McAttemptsMode mode = getCourseMcAttemptsMode(courseId);
    Optional<StudentOptionSubmission> existing =
        studentOptionSubmissionRepository.findByUserIdAndChallengeId(userId, challengeId);
    if (existing.isPresent()) {
      return buildChoiceResponseForExistingSubmission(existing.get(), userId, challenge);
    }

    ChallengeOption selectedOption = findSelectedOption(challenge, selectedOptionId);
    boolean correct = selectedOption.isCorrect();

    if (mode == McAttemptsMode.ONCE) {
      return handleOnceChoiceSubmission(user, userId, labId, challenge, selectedOption, correct);
    }
    if (!correct) {
      return buildChoiceResponse(false, userId, challenge.getLab(), challenge);
    }
    return handleCorrectUnlimitedChoiceSubmission(user, userId, labId, challenge, selectedOption);
  }

  private ChoiceSubmissionResponseDto buildChoiceResponseForExistingSubmission(
      StudentOptionSubmission submission, UUID userId, Challenge challenge) {
    return buildChoiceResponse(
        submission.isCorrect(),
        userId,
        challenge.getLab(),
        submission.isCorrect() ? null : challenge);
  }

  private User findActiveUser(UUID userId) {
    return userRepository
        .findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId)));
  }

  private Challenge findChallengeInChallenge(UUID challengeId, UUID labId) {
    Challenge challenge =
        challengeRepository
            .findById(challengeId)
            .orElseThrow(
                () ->
                    new ChallengeNotFoundException(
                        String.format(CHALLENGE_NOT_FOUND_MSG, challengeId)));
    if (!challenge.getLab().getId().equals(labId)) {
      throw new ChallengeNotFoundException(
          String.format(CHALLENGE_NOT_IN_LAB_MSG, challengeId, labId));
    }
    return challenge;
  }

  private McAttemptsMode getCourseMcAttemptsMode(UUID courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));
    return course.getMcAttemptsMode() != null
        ? course.getMcAttemptsMode()
        : McAttemptsMode.UNLIMITED;
  }

  private ChallengeOption findSelectedOption(Challenge challenge, UUID selectedOptionId) {
    return challenge.getOptions().stream()
        .filter(option -> option.getId().equals(selectedOptionId))
        .findFirst()
        .orElseThrow(
            () ->
                new ChallengeNotFoundException(
                    String.format(
                        "Option '%s' does not belong to challenge '%s'",
                        selectedOptionId, challenge.getId())));
  }

  private ChoiceSubmissionResponseDto handleOnceChoiceSubmission(
      User user,
      UUID userId,
      UUID labId,
      Challenge challenge,
      ChallengeOption selectedOption,
      boolean correct) {
    try {
      saveChoiceSubmission(user, challenge, selectedOption, correct);
    } catch (DataIntegrityViolationException ex) {
      return buildChoiceResponse(correct, userId, challenge.getLab(), correct ? null : challenge);
    }
    saveCompletionIfMissing(user, userId, challenge);
    ChoiceSubmissionResponseDto response =
        buildChoiceResponse(correct, userId, challenge.getLab(), correct ? null : challenge);
    awardBadgeIfChallengeSolved(response, userId, labId);
    return response;
  }

  private ChoiceSubmissionResponseDto handleCorrectUnlimitedChoiceSubmission(
      User user, UUID userId, UUID labId, Challenge challenge, ChallengeOption selectedOption) {
    try {
      saveChoiceSubmission(user, challenge, selectedOption, true);
    } catch (DataIntegrityViolationException ex) {
      return buildChoiceResponse(true, userId, challenge.getLab(), null);
    }
    saveCompletionIfMissing(user, userId, challenge);
    ChoiceSubmissionResponseDto response =
        buildChoiceResponse(true, userId, challenge.getLab(), null);
    awardBadgeIfChallengeSolved(response, userId, labId);
    return response;
  }

  private void saveChoiceSubmission(
      User user, Challenge challenge, ChallengeOption selectedOption, boolean correct) {
    StudentOptionSubmission submission = new StudentOptionSubmission();
    submission.setUser(user);
    submission.setChallenge(challenge);
    submission.setSelectedOption(selectedOption);
    submission.setCorrect(correct);
    submission.setSubmittedAt(LocalDateTime.now());
    studentOptionSubmissionRepository.saveAndFlush(submission);
  }

  private void saveCompletionIfMissing(User user, UUID userId, Challenge challenge) {
    if (challengeCompletionRepository.existsByUserIdAndChallengeId(userId, challenge.getId())) {
      return;
    }
    ChallengeCompletion completion = new ChallengeCompletion();
    completion.setUser(user);
    completion.setChallenge(challenge);
    completion.setSolvedAt(LocalDateTime.now());
    try {
      challengeCompletionRepository.saveAndFlush(completion);
    } catch (DataIntegrityViolationException ignored) {
      // Already recorded by concurrent request
    }
  }

  private void awardBadgeIfChallengeSolved(
      ChoiceSubmissionResponseDto response, UUID userId, UUID labId) {
    if (response.isChallengeSolved()) {
      badgeService.tryAwardBadgesForChallenge(userId, labId);
    }
  }

  private ChoiceSubmissionResponseDto buildChoiceResponse(
      boolean correct, UUID userId, Lab lab, Challenge challenge) {
    List<Challenge> siblings = lab.getChallenges();
    List<UUID> siblingIds = siblingsIds(siblings);
    Set<UUID> solvedIds = solvedChallengeIds(userId, siblingIds);
    int solvedCount = solvedIds.size();
    int totalCount = siblings.size();
    boolean challengeSolved = totalCount > 0 && solvedCount == totalCount;
    UUID correctOptionId = null;
    if (!correct && challenge != null) {
      correctOptionId =
          challenge.getOptions().stream()
              .filter(ChallengeOption::isCorrect)
              .map(ChallengeOption::getId)
              .findFirst()
              .orElse(null);
    }
    return new ChoiceSubmissionResponseDto(
        correct, challengeSolved, solvedCount, totalCount, correctOptionId);
  }

  // -------------------------------------------------------------------------
  // Enrollment guards
  // -------------------------------------------------------------------------

  private void verifyEnrollment(UUID userId, UUID courseId) {
    if (!courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId)) {
      throw new LabAccessDeniedException(
          String.format("User '%s' is not enrolled in course '%s'", userId, courseId));
    }
  }

  private void verifySubmissionAllowed(UUID userId, UUID courseId, UUID labId) {
    verifyEnrollment(userId, courseId);

    CourseLab courseLab =
        courseLabRepository
            .findByCourseIdAndLabId(courseId, labId)
            .orElseThrow(
                () ->
                    new LabAccessDeniedException(
                        String.format(
                            "Lab '%s' is not part of course '%s' for user '%s'",
                            labId, courseId, userId)));

    if (courseLab.getDueAt() != null && LocalDateTime.now().isAfter(courseLab.getDueAt())) {
      throw new LabSubmissionClosedException("Submission deadline has passed for this lab.");
    }
  }

  // -------------------------------------------------------------------------
  // Progress population
  // -------------------------------------------------------------------------

  private void populateStudentProgress(LabStudentDto dto, UUID userId, Lab entity) {
    List<ChallengeStudentDto> challenges =
        dto.getChallenges() == null ? List.of() : dto.getChallenges();
    List<UUID> challengeIds = challengeIds(challenges);
    Set<UUID> solvedIds =
        challengeIds.isEmpty()
            ? Set.of()
            : new HashSet<>(
                challengeCompletionRepository.findSolvedChallengeIds(userId, challengeIds));

    Map<UUID, String> flagsById = new HashMap<>();
    for (Challenge st : entity.getChallenges()) {
      flagsById.put(st.getId(), st.getFlag());
    }

    Map<UUID, UUID> selectedOptionByChallenge = loadSelectedOptions(userId, entity.getChallenges());
    Map<UUID, UUID> correctOptionByChallenge =
        loadCorrectOptionsForWrongAnswers(userId, entity.getChallenges());

    int solvedCount = 0;
    for (ChallengeStudentDto st : challenges) {
      solvedCount +=
          applyChallengeProgress(
              st, solvedIds, flagsById, selectedOptionByChallenge, correctOptionByChallenge);
    }
    dto.setTotalChallengeCount(challenges.size());
    dto.setSolvedChallengeCount(solvedCount);
    dto.setSolved(!challenges.isEmpty() && solvedCount == challenges.size());
  }

  private List<UUID> challengeIds(List<ChallengeStudentDto> challenges) {
    List<UUID> ids = new ArrayList<>();
    for (ChallengeStudentDto challenge : challenges) {
      ids.add(challenge.getId());
    }
    return ids;
  }

  private Map<UUID, UUID> loadSelectedOptions(UUID userId, List<Challenge> challenges) {
    Map<UUID, UUID> selectedOptionByChallenge = new HashMap<>();
    for (Challenge challenge : challenges) {
      if (challenge.getType() == ChallengeType.MULTIPLE_CHOICE) {
        studentOptionSubmissionRepository
            .findByUserIdAndChallengeId(userId, challenge.getId())
            .ifPresent(
                sub ->
                    selectedOptionByChallenge.put(
                        challenge.getId(), sub.getSelectedOption().getId()));
      }
    }
    return selectedOptionByChallenge;
  }

  private Map<UUID, UUID> loadCorrectOptionsForWrongAnswers(
      UUID userId, List<Challenge> challenges) {
    Map<UUID, UUID> correctOptionByChallenge = new HashMap<>();
    for (Challenge challenge : challenges) {
      if (challenge.getType() == ChallengeType.MULTIPLE_CHOICE) {
        studentOptionSubmissionRepository
            .findByUserIdAndChallengeId(userId, challenge.getId())
            .filter(sub -> !sub.isCorrect())
            .flatMap(sub -> findCorrectOptionId(challenge))
            .ifPresent(correctId -> correctOptionByChallenge.put(challenge.getId(), correctId));
      }
    }
    return correctOptionByChallenge;
  }

  private Optional<UUID> findCorrectOptionId(Challenge challenge) {
    return challenge.getOptions().stream()
        .filter(ChallengeOption::isCorrect)
        .map(ChallengeOption::getId)
        .findFirst();
  }

  private int applyChallengeProgress(
      ChallengeStudentDto challenge,
      Set<UUID> solvedIds,
      Map<UUID, String> flagsById,
      Map<UUID, UUID> selectedOptionByChallenge,
      Map<UUID, UUID> correctOptionByChallenge) {
    UUID challengeId = challenge.getId();
    boolean solved = solvedIds.contains(challengeId);
    UUID selectedOptionId = selectedOptionByChallenge.get(challengeId);
    boolean attemptedMc =
        challenge.getType() == ChallengeType.MULTIPLE_CHOICE && selectedOptionId != null;

    boolean completed = solved || attemptedMc;
    challenge.setSolved(completed);
    if (solved && challenge.getType() == ChallengeType.FLAG) {
      challenge.setSolvedFlag(flagsById.get(challengeId));
    }
    challenge.setSelectedOptionId(selectedOptionId);
    challenge.setCorrectOptionId(correctOptionByChallenge.get(challengeId));
    return completed ? 1 : 0;
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private List<UUID> siblingsIds(List<Challenge> siblings) {
    List<UUID> ids = new ArrayList<>();
    for (Challenge s : siblings) {
      ids.add(s.getId());
    }
    return ids;
  }

  private Set<UUID> solvedChallengeIds(UUID userId, List<UUID> challengeIds) {
    if (challengeIds.isEmpty()) {
      return Set.of();
    }
    return new HashSet<>(
        challengeCompletionRepository.findSolvedChallengeIds(userId, challengeIds));
  }

  @Override
  public long countCompletedLabs(UUID userId) {
    return challengeCompletionRepository.countCompletedLabs(userId);
  }

  @Override
  @Transactional
  public ChallengeSubmissionResponseDto completeTheoryChallenge(
      UUID userId, UUID courseId, UUID labId, UUID challengeId) {
    User user =
        userRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(
                () -> new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId)));

    Challenge challenge =
        challengeRepository
            .findById(challengeId)
            .orElseThrow(
                () ->
                    new ChallengeNotFoundException(
                        String.format(CHALLENGE_NOT_FOUND_MSG, challengeId)));

    if (!challenge.getLab().getId().equals(labId)) {
      throw new ChallengeNotFoundException(
          String.format(CHALLENGE_NOT_IN_LAB_MSG, challengeId, labId));
    }

    if (challenge.getFlag() != null && !challenge.getFlag().isBlank()) {
      throw new IllegalArgumentException(
          "This challenge requires a flag submission and cannot be auto-completed.");
    }

    verifySubmissionAllowed(userId, courseId, challenge.getLab().getId());

    if (!challengeCompletionRepository.existsByUserIdAndChallengeId(userId, challengeId)) {
      ChallengeCompletion completion = new ChallengeCompletion();
      completion.setUser(user);
      completion.setChallenge(challenge);
      completion.setSolvedAt(LocalDateTime.now());
      try {
        challengeCompletionRepository.saveAndFlush(completion);
      } catch (DataIntegrityViolationException ex) {
        // already solved by concurrent request — that's fine
      }
    }

    List<Challenge> siblings = challenge.getLab().getChallenges();
    List<UUID> siblingIds = siblingsIds(siblings);
    Set<UUID> solvedIds = solvedChallengeIds(userId, siblingIds);
    int solvedCount = solvedIds.size();
    int totalCount = siblings.size();
    boolean challengeSolved = totalCount > 0 && solvedCount == totalCount;

    if (challengeSolved) {
      badgeService.tryAwardBadgesForChallenge(userId, labId);
    }

    return new ChallengeSubmissionResponseDto(true, challengeSolved, solvedCount, totalCount);
  }
}
