package com.pm4.istp.course.dto;

import com.pm4.istp.course.db.entities.ChallengeDifficultyEnum;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateChallengeRequestDto {
  @NotBlank(message = "Challenge title is required")
  private String title;

  @Size(max = 200, message = "Short description must not exceed 200 characters")
  private String shortDescription;

  private String description;

  @NotNull(message = "Challenge status is required")
  private ChallengeStatusEnum status;

  @NotNull(message = "Challenge difficulty is required")
  private ChallengeDifficultyEnum difficulty;
}
