package com.pm4.istp.admin.dto;

import com.pm4.istp.course.db.entities.ChallengeDifficultyEnum;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminUpdateChallengeRequestDto {
  @NotBlank private String title;
  private String shortDescription;
  private String description;

  @NotNull private ChallengeStatusEnum status;
  @NotNull private ChallengeDifficultyEnum difficulty;

  // This is still manually maintained in the project (subtasks not implemented yet).
  private Integer maxScore;
}
