package com.pm4.istp.course.services;

import com.pm4.istp.course.db.CreateLabRequest;
import com.pm4.istp.course.db.UpdateLabRequest;
import com.pm4.istp.course.db.entities.Lab;
import com.pm4.istp.course.db.entities.LabStatusEnum;
import com.pm4.istp.course.dto.ChallengeSubmissionResponseDto;
import com.pm4.istp.course.dto.ChoiceSubmissionResponseDto;
import com.pm4.istp.course.dto.LabStudentDto;
import com.pm4.istp.course.dto.ListLabResponseDto;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LabService {
  Lab createChallenge(UUID userId, CreateLabRequest request);

  Lab getChallenge(UUID userId, UUID labId);

  Lab updateChallenge(UUID userId, UUID labId, UpdateLabRequest request);

  void deleteChallenge(UUID userId, UUID labId);

  Page<ListLabResponseDto> listChallengesForCreator(UUID creatorId, Pageable pageable);

  Page<ListLabResponseDto> searchAvailableChallenges(UUID userId, String search, Pageable pageable);

  int previewVisibilityImpact(UUID userId, UUID labId, LabStatusEnum newStatus);

  /**
   * Returns the lab in its student-facing form (no flags; with per-student progress). The caller
   * must be enrolled in a course that contains this lab.
   */
  LabStudentDto getChallengeForPlay(UUID userId, UUID courseId, UUID labId);

  /**
   * Submits a flag for a challenge. Case-sensitive comparison against the plaintext flag. On a
   * correct submission the completion is persisted and cannot be re-submitted.
   */
  ChallengeSubmissionResponseDto submitChallengeFlag(
      UUID userId, UUID labId, UUID challengeId, String flag);

  /**
   * Submits a multiple-choice option for a challenge.
   *
   * <p>Behaviour depends on the course's {@code mcAttemptsMode}:
   *
   * <ul>
   *   <li>{@code ONCE} – the submission is recorded and the challenge is marked completed
   *       regardless of correctness. Points are awarded only when the answer is correct.
   *   <li>{@code UNLIMITED} – wrong answers are NOT persisted, so the student can retry. The
   *       challenge is marked completed only on a correct answer.
   * </ul>
   */
  ChoiceSubmissionResponseDto submitChallengeChoice(
      UUID userId, UUID courseId, UUID labId, UUID challengeId, UUID selectedOptionId);

  /**
   * Completes a theory challenge (FLAG type with no flag set) without requiring a flag submission.
   * The challenge is marked as solved immediately.
   */
  ChallengeSubmissionResponseDto completeTheoryChallenge(UUID userId, UUID labId, UUID challengeId);

  /** Returns the number of labs the user has fully completed (all challenges solved). */
  long countCompletedChallenges(UUID userId);
}
