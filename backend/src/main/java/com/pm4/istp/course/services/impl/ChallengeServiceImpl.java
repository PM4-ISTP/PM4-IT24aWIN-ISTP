package com.pm4.istp.course.services.impl;

import com.pm4.istp.badge.services.BadgeService;
import com.pm4.istp.course.db.CreateChallengeRequest;
import com.pm4.istp.course.db.SubTaskRequest;
import com.pm4.istp.course.db.UpdateChallengeRequest;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import com.pm4.istp.course.db.entities.SubTask;
import com.pm4.istp.course.db.entities.SubTaskCompletion;
import com.pm4.istp.course.dto.ChallengeStudentDto;
import com.pm4.istp.course.dto.ListChallengeResponseDto;
import com.pm4.istp.course.dto.SubTaskStudentDto;
import com.pm4.istp.course.dto.SubTaskSubmissionResponseDto;
import com.pm4.istp.course.exceptions.ChallengeAccessDeniedException;
import com.pm4.istp.course.exceptions.ChallengeNotFoundException;
import com.pm4.istp.course.exceptions.SubTaskAlreadySolvedException;
import com.pm4.istp.course.exceptions.SubTaskNotFoundException;
import com.pm4.istp.course.mappers.ChallengeMapper;
import com.pm4.istp.course.repositories.ChallengeRepository;
import com.pm4.istp.course.repositories.CourseChallengeRepository;
import com.pm4.istp.course.repositories.CourseEnrollmentRepository;
import com.pm4.istp.course.repositories.SubTaskCompletionRepository;
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

  private final UserRepository userRepository;
  private final ChallengeRepository challengeRepository;
  private final CourseChallengeRepository courseChallengeRepository;
  private final SubTaskRepository subTaskRepository;
  private final SubTaskCompletionRepository subTaskCompletionRepository;
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
    challenge.setMaxScore(challenge.getSubTasks().size());

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
    challenge.setMaxScore(challenge.getSubTasks().size());

    Challenge saved = challengeRepository.save(challenge);
    cleanupCourseChallengesForVisibilityChange(challengeId, userId, oldStatus, newStatus);

    return saved;
  }

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
      retained.add(target);
    }

    challenge.getSubTasks().clear();
    challenge.getSubTasks().addAll(retained);
  }

  private String normalizeFlag(String flag) {
    if (flag == null) {
      return null;
    }
    String trimmed = flag.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

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
    // Creator can always see their own challenges
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

    // PUBLIC: anyone can see
  }

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
    return dto;
  }

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
        // Concurrent submission slipped past the existsByUserIdAndSubTaskId check
        // and tripped the unique (user, sub_task) constraint. Surface as 409
        // instead of a 500.
        throw new SubTaskAlreadySolvedException(
            String.format("Sub-task '%s' already solved by user '%s'", subTaskId, userId), ex);
      }
    }

    List<SubTask> siblings = subTask.getChallenge().getSubTasks();
    List<UUID> siblingIds = new ArrayList<>();
    for (SubTask s : siblings) {
      siblingIds.add(s.getId());
    }
    Set<UUID> solvedIds =
        siblingIds.isEmpty()
            ? Set.of()
            : new HashSet<>(subTaskCompletionRepository.findSolvedSubTaskIds(userId, siblingIds));
    int solvedCount = solvedIds.size();
    int totalCount = siblings.size();
    boolean challengeSolved = totalCount > 0 && solvedCount == totalCount;

    if (correct && challengeSolved) {
      badgeService.tryAwardBadgesForChallenge(userId, challengeId);
    }

    return new SubTaskSubmissionResponseDto(correct, challengeSolved, solvedCount, totalCount);
  }

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

    int solvedCount = 0;
    for (SubTaskStudentDto st : subTasks) {
      boolean solved = solvedIds.contains(st.getId());
      st.setSolved(solved);
      if (solved) {
        st.setSolvedFlag(flagsById.get(st.getId()));
        solvedCount++;
      }
    }
    dto.setTotalSubTaskCount(subTasks.size());
    dto.setSolvedSubTaskCount(solvedCount);
    dto.setSolved(!subTasks.isEmpty() && solvedCount == subTasks.size());
  }

  @Override
  public long countCompletedChallenges(UUID userId) {
    return subTaskCompletionRepository.countCompletedChallenges(userId);
  }
}
