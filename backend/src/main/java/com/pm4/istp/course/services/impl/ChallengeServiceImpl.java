package com.pm4.istp.course.services.impl;

import com.pm4.istp.badge.services.BadgeService;
import com.pm4.istp.course.db.CreateChallengeRequest;
import com.pm4.istp.course.db.SubTaskOptionRequest;
import com.pm4.istp.course.db.SubTaskRequest;
import com.pm4.istp.course.db.UpdateChallengeRequest;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.db.entities.McAttemptsMode;
import com.pm4.istp.course.db.entities.StudentOptionSubmission;
import com.pm4.istp.course.db.entities.SubTask;
import com.pm4.istp.course.db.entities.SubTaskCompletion;
import com.pm4.istp.course.db.entities.SubTaskOption;
import com.pm4.istp.course.db.entities.SubTaskType;
import com.pm4.istp.course.dto.ChallengeStudentDto;
import com.pm4.istp.course.dto.ChoiceSubmissionResponseDto;
import com.pm4.istp.course.dto.ListChallengeResponseDto;
import com.pm4.istp.course.dto.SubTaskStudentDto;
import com.pm4.istp.course.dto.SubTaskSubmissionResponseDto;
import com.pm4.istp.course.exceptions.ChallengeAccessDeniedException;
import com.pm4.istp.course.exceptions.ChallengeNotFoundException;
import com.pm4.istp.course.exceptions.CourseNotFoundException;
import com.pm4.istp.course.exceptions.SubTaskAlreadySolvedException;
import com.pm4.istp.course.exceptions.SubTaskNotFoundException;
import com.pm4.istp.course.mappers.ChallengeMapper;
import com.pm4.istp.course.repositories.ChallengeRepository;
import com.pm4.istp.course.repositories.CourseChallengeRepository;
import com.pm4.istp.course.repositories.CourseEnrollmentRepository;
import com.pm4.istp.course.repositories.CourseRepository;
import com.pm4.istp.course.repositories.StudentOptionSubmissionRepository;
import com.pm4.istp.course.repositories.SubTaskCompletionRepository;
import com.pm4.istp.course.repositories.SubTaskOptionRepository;
import com.pm4.istp.course.repositories.SubTaskRepository;
import com.pm4.istp.course.services.ChallengeService;
import com.pm4.istp.course.services.DockerImageAvailabilityService;
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
public class ChallengeServiceImpl implements ChallengeService {

  private static final String USER_NOT_FOUND_MSG = "User with ID '%s' not found";
  private static final String CHALLENGE_NOT_FOUND_MSG = "Challenge with ID '%s' not found";
  private static final String SUB_TASK_NOT_FOUND_MSG = "Sub-task with ID '%s' not found";

  private static final String COURSE_NOT_FOUND_MSG = "Course with ID '%s' not found";

  private final UserRepository userRepository;
  private final ChallengeRepository challengeRepository;
  private final CourseChallengeRepository courseChallengeRepository;
  private final CourseRepository courseRepository;
  private final SubTaskRepository subTaskRepository;
  private final SubTaskOptionRepository subTaskOptionRepository;
  private final SubTaskCompletionRepository subTaskCompletionRepository;
  private final StudentOptionSubmissionRepository studentOptionSubmissionRepository;
  private final CourseEnrollmentRepository courseEnrollmentRepository;
  private final ChallengeMapper challengeMapper;
  private final DockerImageAvailabilityService dockerImageAvailabilityService;
  private final BadgeService badgeService;

  @Override
  @Transactional
  public Challenge createChallenge(UUID userId, CreateChallengeRequest request) {
    User creator =
        userRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(
                () -> new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId)));

    Challenge challenge = new Challenge();
    dockerImageAvailabilityService.assertImageExists(request.getDockerImage());

    challenge.setTitle(request.getTitle());
    challenge.setShortDescription(request.getShortDescription());
    challenge.setDescription(request.getDescription());
    challenge.setStatus(request.getStatus());
    challenge.setDifficulty(request.getDifficulty());
    challenge.setDockerImage(request.getDockerImage());
    challenge.setCreator(creator);

    List<SubTask> subTasks = buildSubTasksForCreate(request.getSubTasks(), challenge);
    challenge.getSubTasks().addAll(subTasks);
    challenge.setMaxScore(totalPoints(challenge.getSubTasks()));

    return challengeRepository.save(challenge);
  }

  @Override
  @Transactional(readOnly = true)
  public Challenge getChallenge(UUID userId, UUID challengeId) {
    Challenge challenge =
        challengeRepository
            .findById(challengeId)
            .orElseThrow(
                () ->
                    new ChallengeNotFoundException(
                        String.format(CHALLENGE_NOT_FOUND_MSG, challengeId)));

    verifyVisibility(challenge, userId);
    return challenge;
  }

  @Override
  @Transactional
  public Challenge updateChallenge(UUID userId, UUID challengeId, UpdateChallengeRequest request) {
    Challenge challenge =
        challengeRepository
            .findById(challengeId)
            .orElseThrow(
                () ->
                    new ChallengeNotFoundException(
                        String.format(CHALLENGE_NOT_FOUND_MSG, challengeId)));

    verifyCreator(challenge, userId);

    ChallengeStatusEnum oldStatus = challenge.getStatus();
    ChallengeStatusEnum newStatus = request.getStatus();
    dockerImageAvailabilityService.assertImageExists(request.getDockerImage());

    challenge.setTitle(request.getTitle());
    challenge.setShortDescription(request.getShortDescription());
    challenge.setDescription(request.getDescription());
    challenge.setStatus(newStatus);
    challenge.setDifficulty(request.getDifficulty());
    challenge.setDockerImage(request.getDockerImage());

    applySubTaskUpdates(challenge, request.getSubTasks());
    challenge.setMaxScore(totalPoints(challenge.getSubTasks()));

    Challenge saved = challengeRepository.save(challenge);
    cleanupCourseChallengesForVisibilityChange(challengeId, userId, oldStatus, newStatus);

    return saved;
  }

  // -------------------------------------------------------------------------
  // Sub-task builders
  // -------------------------------------------------------------------------

  private List<SubTask> buildSubTasksForCreate(List<SubTaskRequest> requests, Challenge parent) {
    List<SubTask> result = new ArrayList<>();
    if (requests == null) {
      return result;
    }
    int idx = 0;
    for (SubTaskRequest req : requests) {
      SubTask st = new SubTask();
      st.setChallenge(parent);
      st.setTitle(req.getTitle());
      st.setDescription(req.getDescription());
      st.setFlag(normalizeFlag(req.getFlag()));
      st.setOrderIndex(idx++);
      st.setType(req.getType() != null ? req.getType() : SubTaskType.FLAG);
      st.setPoints(req.getPoints() > 0 ? req.getPoints() : 1);
      st.setHint(req.getHint());
      applyOptions(st, req.getOptions());
      result.add(st);
    }
    return result;
  }

  private void applySubTaskUpdates(Challenge challenge, List<SubTaskRequest> requests) {
    List<SubTaskRequest> incoming = requests == null ? List.of() : requests;

    Map<UUID, SubTask> existingById = new HashMap<>();
    for (SubTask existing : challenge.getSubTasks()) {
      if (existing.getId() != null) {
        existingById.put(existing.getId(), existing);
      }
    }

    List<SubTask> retained = new ArrayList<>();
    int idx = 0;
    for (SubTaskRequest req : incoming) {
      SubTask target = req.getId() != null ? existingById.remove(req.getId()) : null;
      if (target == null) {
        target = new SubTask();
        target.setChallenge(challenge);
      }
      target.setTitle(req.getTitle());
      target.setDescription(req.getDescription());
      target.setFlag(normalizeFlag(req.getFlag()));
      target.setOrderIndex(idx++);
      target.setType(req.getType() != null ? req.getType() : SubTaskType.FLAG);
      target.setPoints(req.getPoints() > 0 ? req.getPoints() : 1);
      target.setHint(req.getHint());
      applyOptions(target, req.getOptions());
      retained.add(target);
    }

    challenge.getSubTasks().clear();
    challenge.getSubTasks().addAll(retained);
  }

  private void applyOptions(SubTask subTask, List<SubTaskOptionRequest> optionRequests) {
    subTask.getOptions().clear();
    if (optionRequests == null || optionRequests.isEmpty()) {
      return;
    }
    int idx = 0;
    for (SubTaskOptionRequest req : optionRequests) {
      SubTaskOption opt = new SubTaskOption();
      opt.setSubTask(subTask);
      opt.setText(req.getText());
      opt.setCorrect(req.isCorrect());
      opt.setOrderIndex(idx++);
      subTask.getOptions().add(opt);
    }
  }

  private int totalPoints(List<SubTask> subTasks) {
    int sum = 0;
    for (SubTask st : subTasks) {
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
  public int previewVisibilityImpact(UUID userId, UUID challengeId, ChallengeStatusEnum newStatus) {
    Challenge challenge =
        challengeRepository
            .findById(challengeId)
            .orElseThrow(
                () ->
                    new ChallengeNotFoundException(
                        String.format(CHALLENGE_NOT_FOUND_MSG, challengeId)));

    verifyCreator(challenge, userId);

    return countAffectedCourses(challengeId, userId, challenge.getStatus(), newStatus);
  }

  private int countAffectedCourses(
      UUID challengeId,
      UUID creatorId,
      ChallengeStatusEnum oldStatus,
      ChallengeStatusEnum newStatus) {
    if (oldStatus == newStatus) {
      return 0;
    }
    if (newStatus == ChallengeStatusEnum.DRAFT) {
      return (int) courseChallengeRepository.countByChallengeId(challengeId);
    }
    if (newStatus == ChallengeStatusEnum.PRIVATE && oldStatus == ChallengeStatusEnum.PUBLIC) {
      return (int)
          courseChallengeRepository.countByChallengeIdWhereCreatorNotInstructor(
              challengeId, creatorId);
    }
    return 0;
  }

  private void cleanupCourseChallengesForVisibilityChange(
      UUID challengeId,
      UUID creatorId,
      ChallengeStatusEnum oldStatus,
      ChallengeStatusEnum newStatus) {
    if (oldStatus == newStatus) {
      return;
    }
    if (newStatus == ChallengeStatusEnum.DRAFT) {
      courseChallengeRepository.deleteByChallengeId(challengeId);
      return;
    }
    if (newStatus == ChallengeStatusEnum.PRIVATE && oldStatus == ChallengeStatusEnum.PUBLIC) {
      courseChallengeRepository.deleteByChallengeIdWhereCreatorNotInstructor(
          challengeId, creatorId);
    }
  }

  @Override
  @Transactional
  public void deleteChallenge(UUID userId, UUID challengeId) {
    Challenge challenge =
        challengeRepository
            .findById(challengeId)
            .orElseThrow(
                () ->
                    new ChallengeNotFoundException(
                        String.format(CHALLENGE_NOT_FOUND_MSG, challengeId)));

    verifyCreator(challenge, userId);
    challengeRepository.delete(challenge);
  }

  @Override
  public Page<ListChallengeResponseDto> listChallengesForCreator(
      UUID creatorId, Pageable pageable) {
    return challengeRepository.findListChallengesForCreator(creatorId, pageable);
  }

  @Override
  public Page<ListChallengeResponseDto> searchAvailableChallenges(
      UUID userId, String search, Pageable pageable) {
    return challengeRepository.searchAvailableChallenges(userId, search, pageable);
  }

  private void verifyCreator(Challenge challenge, UUID userId) {
    if (!challenge.getCreator().getId().equals(userId)) {
      throw new ChallengeAccessDeniedException(
          String.format(
              "User with ID '%s' is not the creator of challenge '%s'", userId, challenge.getId()));
    }
  }

  private void verifyVisibility(Challenge challenge, UUID userId) {
    if (challenge.getCreator().getId().equals(userId)) {
      return;
    }

    if (challenge.getStatus() == ChallengeStatusEnum.DRAFT) {
      throw new ChallengeAccessDeniedException(
          String.format(
              "User with ID '%s' cannot access draft challenge '%s'", userId, challenge.getId()));
    }

    if (challenge.getStatus() == ChallengeStatusEnum.PRIVATE) {
      boolean isInstructorOfCourseWithChallenge =
          courseChallengeRepository.existsByChallengeIdAndCourseInstructorId(
              challenge.getId(), userId);
      boolean isEnrolledInCourseWithChallenge =
          courseChallengeRepository.existsByChallengeIdAndEnrolledUserId(challenge.getId(), userId);
      if (!isInstructorOfCourseWithChallenge && !isEnrolledInCourseWithChallenge) {
        throw new ChallengeAccessDeniedException(
            String.format(
                "User with ID '%s' cannot access private challenge '%s'",
                userId, challenge.getId()));
      }
    }
  }

  // -------------------------------------------------------------------------
  // Student play
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public ChallengeStudentDto getChallengeForPlay(UUID userId, UUID courseId, UUID challengeId) {
    verifyEnrollment(userId, courseId);

    Challenge challenge =
        challengeRepository
            .findById(challengeId)
            .orElseThrow(
                () ->
                    new ChallengeNotFoundException(
                        String.format(CHALLENGE_NOT_FOUND_MSG, challengeId)));

    boolean challengeBelongsToCourse =
        challenge.getCourseChallenges().stream()
            .anyMatch(cc -> cc.getCourse().getId().equals(courseId));
    if (!challengeBelongsToCourse) {
      throw new ChallengeAccessDeniedException(
          String.format("Challenge '%s' is not part of course '%s'", challengeId, courseId));
    }

    ChallengeStudentDto dto = challengeMapper.toStudentDto(challenge);
    populateStudentProgress(dto, userId, challenge);

    // Expose the course's MC attempt mode so the frontend can render the UI correctly
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));
    McAttemptsMode mode =
        course.getMcAttemptsMode() != null ? course.getMcAttemptsMode() : McAttemptsMode.UNLIMITED;
    dto.setMcAttemptsMode(mode.name());

    return dto;
  }

  // -------------------------------------------------------------------------
  // Flag submission
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public SubTaskSubmissionResponseDto submitSubTaskFlag(
      UUID userId, UUID challengeId, UUID subTaskId, String flag) {
    User user =
        userRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(
                () -> new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId)));

    SubTask subTask =
        subTaskRepository
            .findById(subTaskId)
            .orElseThrow(
                () ->
                    new SubTaskNotFoundException(String.format(SUB_TASK_NOT_FOUND_MSG, subTaskId)));

    if (!subTask.getChallenge().getId().equals(challengeId)) {
      throw new SubTaskNotFoundException(
          String.format("Sub-task '%s' does not belong to challenge '%s'", subTaskId, challengeId));
    }

    verifyEnrolledInChallengeCourse(userId, subTask.getChallenge());

    if (subTaskCompletionRepository.existsByUserIdAndSubTaskId(userId, subTaskId)) {
      throw new SubTaskAlreadySolvedException(
          String.format("Sub-task '%s' already solved by user '%s'", subTaskId, userId));
    }

    boolean correct = subTask.getFlag() != null && subTask.getFlag().equals(flag);
    if (correct) {
      SubTaskCompletion completion = new SubTaskCompletion();
      completion.setUser(user);
      completion.setSubTask(subTask);
      completion.setSolvedAt(LocalDateTime.now());
      try {
        subTaskCompletionRepository.saveAndFlush(completion);
      } catch (DataIntegrityViolationException ex) {
        throw new SubTaskAlreadySolvedException(
            String.format("Sub-task '%s' already solved by user '%s'", subTaskId, userId), ex);
      }
    }

    List<SubTask> siblings = subTask.getChallenge().getSubTasks();
    List<UUID> siblingIds = siblingsIds(siblings);
    Set<UUID> solvedIds = solvedSubTaskIds(userId, siblingIds);
    int solvedCount = solvedIds.size();
    int totalCount = siblings.size();
    boolean challengeSolved = totalCount > 0 && solvedCount == totalCount;

    if (correct && challengeSolved) {
      badgeService.tryAwardBadgesForChallenge(userId, challengeId);
    }

    return new SubTaskSubmissionResponseDto(correct, challengeSolved, solvedCount, totalCount);
  }

  // -------------------------------------------------------------------------
  // Multiple-choice submission
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public ChoiceSubmissionResponseDto submitSubTaskChoice(
      UUID userId, UUID courseId, UUID challengeId, UUID subTaskId, UUID selectedOptionId) {
    User user =
        userRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(
                () -> new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId)));

    SubTask subTask =
        subTaskRepository
            .findById(subTaskId)
            .orElseThrow(
                () ->
                    new SubTaskNotFoundException(String.format(SUB_TASK_NOT_FOUND_MSG, subTaskId)));

    if (!subTask.getChallenge().getId().equals(challengeId)) {
      throw new SubTaskNotFoundException(
          String.format("Sub-task '%s' does not belong to challenge '%s'", subTaskId, challengeId));
    }

    verifyEnrolledInChallengeCourse(userId, subTask.getChallenge());

    // Determine which attempt mode this course uses
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));
    McAttemptsMode mode =
        course.getMcAttemptsMode() != null ? course.getMcAttemptsMode() : McAttemptsMode.UNLIMITED;

    // Return existing submission if already saved (applies to ONCE mode and correct UNLIMITED)
    Optional<StudentOptionSubmission> existing =
        studentOptionSubmissionRepository.findByUserIdAndSubTaskId(userId, subTaskId);
    if (existing.isPresent()) {
      StudentOptionSubmission prev = existing.get();
      return buildChoiceResponse(
          prev.isCorrect(),
          userId,
          subTask.getChallenge(),
          challengeId,
          prev.isCorrect() ? null : subTask);
    }

    SubTaskOption selectedOption =
        subTask.getOptions().stream()
            .filter(o -> o.getId().equals(selectedOptionId))
            .findFirst()
            .orElseThrow(
                () ->
                    new SubTaskNotFoundException(
                        String.format(
                            "Option '%s' does not belong to sub-task '%s'",
                            selectedOptionId, subTaskId)));

    boolean correct = selectedOption.isCorrect();

    if (mode == McAttemptsMode.ONCE) {
      // -----------------------------------------------------------------------
      // ONCE mode: always persist the submission (even if wrong) and always mark
      // the sub-task as completed so progress is counted.
      // -----------------------------------------------------------------------
      StudentOptionSubmission submission = new StudentOptionSubmission();
      submission.setUser(user);
      submission.setSubTask(subTask);
      submission.setSelectedOption(selectedOption);
      submission.setCorrect(correct);
      submission.setSubmittedAt(LocalDateTime.now());
      try {
        studentOptionSubmissionRepository.saveAndFlush(submission);
      } catch (DataIntegrityViolationException ex) {
        // Concurrent submission — return current state
        return buildChoiceResponse(
            correct, userId, subTask.getChallenge(), challengeId, correct ? null : subTask);
      }

      // Mark as completed regardless of correctness (ONCE = attempted = done)
      if (!subTaskCompletionRepository.existsByUserIdAndSubTaskId(userId, subTaskId)) {
        SubTaskCompletion completion = new SubTaskCompletion();
        completion.setUser(user);
        completion.setSubTask(subTask);
        completion.setSolvedAt(LocalDateTime.now());
        try {
          subTaskCompletionRepository.saveAndFlush(completion);
        } catch (DataIntegrityViolationException ignored) {
          // Already recorded by concurrent request
        }
      }

      ChoiceSubmissionResponseDto response =
          buildChoiceResponse(
              correct, userId, subTask.getChallenge(), challengeId, correct ? null : subTask);
      if (response.isChallengeSolved()) {
        badgeService.tryAwardBadgesForChallenge(userId, challengeId);
      }
      return response;

    } else {
      // -----------------------------------------------------------------------
      // UNLIMITED mode: only persist and complete when the answer is correct.
      // Wrong answers are NOT saved, so the student can retry.
      // -----------------------------------------------------------------------
      if (!correct) {
        // Return temporary response without persisting anything
        return buildChoiceResponse(false, userId, subTask.getChallenge(), challengeId, subTask);
      }

      // Correct answer — persist submission and completion
      StudentOptionSubmission submission = new StudentOptionSubmission();
      submission.setUser(user);
      submission.setSubTask(subTask);
      submission.setSelectedOption(selectedOption);
      submission.setCorrect(true);
      submission.setSubmittedAt(LocalDateTime.now());
      try {
        studentOptionSubmissionRepository.saveAndFlush(submission);
      } catch (DataIntegrityViolationException ex) {
        return buildChoiceResponse(true, userId, subTask.getChallenge(), challengeId, null);
      }

      if (!subTaskCompletionRepository.existsByUserIdAndSubTaskId(userId, subTaskId)) {
        SubTaskCompletion completion = new SubTaskCompletion();
        completion.setUser(user);
        completion.setSubTask(subTask);
        completion.setSolvedAt(LocalDateTime.now());
        try {
          subTaskCompletionRepository.saveAndFlush(completion);
        } catch (DataIntegrityViolationException ignored) {
          // Already recorded by concurrent request
        }
      }

      ChoiceSubmissionResponseDto response =
          buildChoiceResponse(true, userId, subTask.getChallenge(), challengeId, null);
      if (response.isChallengeSolved()) {
        badgeService.tryAwardBadgesForChallenge(userId, challengeId);
      }
      return response;
    }
  }

  private ChoiceSubmissionResponseDto buildChoiceResponse(
      boolean correct, UUID userId, Challenge challenge, UUID challengeId, SubTask subTask) {
    List<SubTask> siblings = challenge.getSubTasks();
    List<UUID> siblingIds = siblingsIds(siblings);
    Set<UUID> solvedIds = solvedSubTaskIds(userId, siblingIds);
    int solvedCount = solvedIds.size();
    int totalCount = siblings.size();
    boolean challengeSolved = totalCount > 0 && solvedCount == totalCount;
    UUID correctOptionId = null;
    if (!correct && subTask != null) {
      correctOptionId =
          subTask.getOptions().stream()
              .filter(SubTaskOption::isCorrect)
              .map(SubTaskOption::getId)
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
      throw new ChallengeAccessDeniedException(
          String.format("User '%s' is not enrolled in course '%s'", userId, courseId));
    }
  }

  private void verifyEnrolledInChallengeCourse(UUID userId, Challenge challenge) {
    boolean enrolled =
        courseChallengeRepository.existsByChallengeIdAndEnrolledUserId(challenge.getId(), userId);
    if (!enrolled) {
      throw new ChallengeAccessDeniedException(
          String.format(
              "User '%s' is not enrolled in any course containing challenge '%s'",
              userId, challenge.getId()));
    }
  }

  // -------------------------------------------------------------------------
  // Progress population
  // -------------------------------------------------------------------------

  private void populateStudentProgress(ChallengeStudentDto dto, UUID userId, Challenge entity) {
    List<SubTaskStudentDto> subTasks = dto.getSubTasks() == null ? List.of() : dto.getSubTasks();
    List<UUID> subTaskIds = new ArrayList<>();
    for (SubTaskStudentDto st : subTasks) {
      subTaskIds.add(st.getId());
    }
    Set<UUID> solvedIds =
        subTaskIds.isEmpty()
            ? Set.of()
            : new HashSet<>(subTaskCompletionRepository.findSolvedSubTaskIds(userId, subTaskIds));

    Map<UUID, String> flagsById = new HashMap<>();
    for (SubTask st : entity.getSubTasks()) {
      flagsById.put(st.getId(), st.getFlag());
    }

    // Load MC submissions for this user in one pass
    Map<UUID, UUID> selectedOptionBySubTask = new HashMap<>();
    Map<UUID, UUID> correctOptionBySubTask = new HashMap<>();
    for (SubTask st : entity.getSubTasks()) {
      if (st.getType() == SubTaskType.MULTIPLE_CHOICE) {
        studentOptionSubmissionRepository
            .findByUserIdAndSubTaskId(userId, st.getId())
            .ifPresent(
                sub -> {
                  selectedOptionBySubTask.put(st.getId(), sub.getSelectedOption().getId());
                  // Expose correct option only when the student got it wrong
                  if (!sub.isCorrect()) {
                    st.getOptions().stream()
                        .filter(SubTaskOption::isCorrect)
                        .map(SubTaskOption::getId)
                        .findFirst()
                        .ifPresent(cid -> correctOptionBySubTask.put(st.getId(), cid));
                  }
                });
      }
    }

    int solvedCount = 0;
    for (SubTaskStudentDto st : subTasks) {
      boolean solved = solvedIds.contains(st.getId());
      st.setSolved(solved);
      if (solved) {
        if (st.getType() == SubTaskType.FLAG) {
          st.setSolvedFlag(flagsById.get(st.getId()));
        }
        solvedCount++;
      }
      if (selectedOptionBySubTask.containsKey(st.getId())) {
        st.setSelectedOptionId(selectedOptionBySubTask.get(st.getId()));
      }
      if (correctOptionBySubTask.containsKey(st.getId())) {
        st.setCorrectOptionId(correctOptionBySubTask.get(st.getId()));
      }
    }
    dto.setTotalSubTaskCount(subTasks.size());
    dto.setSolvedSubTaskCount(solvedCount);
    dto.setSolved(!subTasks.isEmpty() && solvedCount == subTasks.size());
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private List<UUID> siblingsIds(List<SubTask> siblings) {
    List<UUID> ids = new ArrayList<>();
    for (SubTask s : siblings) {
      ids.add(s.getId());
    }
    return ids;
  }

  private Set<UUID> solvedSubTaskIds(UUID userId, List<UUID> subTaskIds) {
    if (subTaskIds.isEmpty()) {
      return Set.of();
    }
    return new HashSet<>(subTaskCompletionRepository.findSolvedSubTaskIds(userId, subTaskIds));
  }

  @Override
  public long countCompletedChallenges(UUID userId) {
    return subTaskCompletionRepository.countCompletedChallenges(userId);
  }

  @Override
  @Transactional
  public SubTaskSubmissionResponseDto completeTheorySubTask(
      UUID userId, UUID challengeId, UUID subTaskId) {
    User user =
        userRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(
                () -> new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId)));

    SubTask subTask =
        subTaskRepository
            .findById(subTaskId)
            .orElseThrow(
                () ->
                    new SubTaskNotFoundException(String.format(SUB_TASK_NOT_FOUND_MSG, subTaskId)));

    if (!subTask.getChallenge().getId().equals(challengeId)) {
      throw new SubTaskNotFoundException(
          String.format("Sub-task '%s' does not belong to challenge '%s'", subTaskId, challengeId));
    }

    if (subTask.getFlag() != null && !subTask.getFlag().isBlank()) {
      throw new IllegalArgumentException(
          "This sub-task requires a flag submission and cannot be auto-completed.");
    }

    verifyEnrolledInChallengeCourse(userId, subTask.getChallenge());

    if (!subTaskCompletionRepository.existsByUserIdAndSubTaskId(userId, subTaskId)) {
      SubTaskCompletion completion = new SubTaskCompletion();
      completion.setUser(user);
      completion.setSubTask(subTask);
      completion.setSolvedAt(LocalDateTime.now());
      try {
        subTaskCompletionRepository.saveAndFlush(completion);
      } catch (DataIntegrityViolationException ex) {
        // already solved by concurrent request — that's fine
      }
    }

    List<SubTask> siblings = subTask.getChallenge().getSubTasks();
    List<UUID> siblingIds = siblingsIds(siblings);
    Set<UUID> solvedIds = solvedSubTaskIds(userId, siblingIds);
    int solvedCount = solvedIds.size();
    int totalCount = siblings.size();
    boolean challengeSolved = totalCount > 0 && solvedCount == totalCount;

    if (challengeSolved) {
      badgeService.tryAwardBadgesForChallenge(userId, challengeId);
    }

    return new SubTaskSubmissionResponseDto(true, challengeSolved, solvedCount, totalCount);
  }
}
