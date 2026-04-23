package com.pm4.istp.course.services.impl;

import com.pm4.istp.course.db.CreateChallengeRequest;
import com.pm4.istp.course.db.SubTaskRequest;
import com.pm4.istp.course.db.UpdateChallengeRequest;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import com.pm4.istp.course.db.entities.SubTask;
import com.pm4.istp.course.dto.ListChallengeResponseDto;
import com.pm4.istp.course.exceptions.ChallengeAccessDeniedException;
import com.pm4.istp.course.exceptions.ChallengeNotFoundException;
import com.pm4.istp.course.repositories.ChallengeRepository;
import com.pm4.istp.course.repositories.CourseChallengeRepository;
import com.pm4.istp.course.services.ChallengeService;
import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.exceptions.UserNotFoundException;
import com.pm4.istp.user.repositories.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

  private static final String USER_NOT_FOUND_MSG = "User with ID '%s' not found";
  private static final String CHALLENGE_NOT_FOUND_MSG = "Challenge with ID '%s' not found";

  private final UserRepository userRepository;
  private final ChallengeRepository challengeRepository;
  private final CourseChallengeRepository courseChallengeRepository;

  @Override
  @Transactional
  public Challenge createChallenge(UUID userId, CreateChallengeRequest request) {
    User creator =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId)));

    Challenge challenge = new Challenge();
    challenge.setTitle(request.getTitle());
    challenge.setShortDescription(request.getShortDescription());
    challenge.setDescription(request.getDescription());
    challenge.setStatus(request.getStatus());
    challenge.setDifficulty(request.getDifficulty());
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

    challenge.setTitle(request.getTitle());
    challenge.setShortDescription(request.getShortDescription());
    challenge.setDescription(request.getDescription());
    challenge.setStatus(newStatus);
    challenge.setDifficulty(request.getDifficulty());

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
      if (!isInstructorOfCourseWithChallenge) {
        throw new ChallengeAccessDeniedException(
            String.format(
                "User with ID '%s' cannot access private challenge '%s'",
                userId, challenge.getId()));
      }
    }

    // PUBLIC: anyone can see
  }
}
