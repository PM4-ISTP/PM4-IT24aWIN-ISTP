package com.pm4.istp.course.dto;

import com.pm4.istp.course.db.entities.ChallengeDifficultyEnum;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateChallengeRequestDto {
  @NotBlank(message = "Challenge title is required")
  @Size(max = 255, message = "Challenge title must be at most 255 characters")
  private String title;

  @Size(max = 200, message = "Short description must not exceed 200 characters")
  private String shortDescription;

  @Size(max = 5000, message = "Description must be at most 5000 characters")
  private String description;

  @NotNull(message = "Challenge status is required")
  private ChallengeStatusEnum status;

  @NotNull(message = "Challenge difficulty is required")
  private ChallengeDifficultyEnum difficulty;

  @NotEmpty(message = "At least one sub task is required")
  @Valid
  private List<SubTaskRequestDto> subTasks;
}
