package com.pm4.istp.course.services;

import com.pm4.istp.course.db.CreateChallengeRequest;
import com.pm4.istp.course.db.UpdateChallengeRequest;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import com.pm4.istp.course.dto.ChallengeStudentDto;
import com.pm4.istp.course.dto.ChoiceSubmissionResponseDto;
import com.pm4.istp.course.dto.ListChallengeResponseDto;
import com.pm4.istp.course.dto.SubTaskSubmissionResponseDto;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChallengeService {
  Challenge createChallenge(UUID userId, CreateChallengeRequest request);

  Challenge getChallenge(UUID userId, UUID challengeId);

  Challenge updateChallenge(UUID userId, UUID challengeId, UpdateChallengeRequest request);

  void deleteChallenge(UUID userId, UUID challengeId);

  Page<ListChallengeResponseDto> listChallengesForCreator(UUID creatorId, Pageable pageable);

  Page<ListChallengeResponseDto> searchAvailableChallenges(
      UUID userId, String search, Pageable pageable);

  int previewVisibilityImpact(UUID userId, UUID challengeId, ChallengeStatusEnum newStatus);

  /**
   * Returns the challenge in its student-facing form (no flags; with per-student progress). The
   * caller must be enrolled in a course that contains this challenge.
   */
  ChallengeStudentDto getChallengeForPlay(UUID userId, UUID courseId, UUID challengeId);

  /**
   * Submits a flag for a sub-task. Case-sensitive comparison against the plaintext flag. On a
   * correct submission the completion is persisted and cannot be re-submitted.
   */
  SubTaskSubmissionResponseDto submitSubTaskFlag(
      UUID userId, UUID challengeId, UUID subTaskId, String flag);

  /**
   * Submits a multiple-choice option for a sub-task. Automatically awards points when correct. A
   * student may only submit once; re-submission returns the existing result.
   */
  ChoiceSubmissionResponseDto submitSubTaskChoice(
      UUID userId, UUID challengeId, UUID subTaskId, UUID selectedOptionId);

  /** Returns the number of challenges the user has fully completed (all sub-tasks solved). */
  long countCompletedChallenges(UUID userId);
}
