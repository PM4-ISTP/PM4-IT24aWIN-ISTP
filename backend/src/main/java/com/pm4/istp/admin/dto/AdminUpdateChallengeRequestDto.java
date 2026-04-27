package com.pm4.istp.admin.dto;

import com.pm4.istp.course.db.entities.ChallengeDifficultyEnum;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.constraints.Size;

@Data
public class AdminUpdateChallengeRequestDto {
  @NotBlank(message = "Title is required")
  @Size(max = 255, message = "Title must be at most 255 characters")
  private String title;

  @Size(max = 200, message = "Short description must be at most 200 characters")
  private String shortDescription;

  @Size(max = 5000, message = "Description must be at most 5000 characters")
  private String description;

  @NotNull private ChallengeStatusEnum status;
  @NotNull private ChallengeDifficultyEnum difficulty;

  // This is still manually maintained in the project (subtasks not implemented yet).
  @Min(value = 0, message = "Max score must be at least 0")
  private Integer maxScore;
}
