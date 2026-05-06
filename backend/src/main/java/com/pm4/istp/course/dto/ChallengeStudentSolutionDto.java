package com.pm4.istp.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pm4.istp.course.db.entities.ChallengeDifficultyEnum;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Instructor-facing view of a student's challenge progress and submissions. Similar to {@link
 * ChallengeStudentDto}, but omits the challenge's docker image and MC attempt mode while including
 * each sub-task's submitted solution.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChallengeStudentSolutionDto {
  private UUID id;
  private String title;
  private String shortDescription;
  private String description;
  private ChallengeStatusEnum status;
  private ChallengeDifficultyEnum difficulty;
  private int maxScore;
  private ChallengeCreatorResponseDto creator;
  private List<SubTaskStudentDto> subTasks;
  private int solvedSubTaskCount;
  private int totalSubTaskCount;

  @JsonProperty("isSolved")
  private boolean solved;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
