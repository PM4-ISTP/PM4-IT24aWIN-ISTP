package com.pm4.istp.course.dto;

import com.pm4.istp.course.db.entities.ChallengeDifficultyEnum;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
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

  @NotBlank(message = "Docker image is required")
  @Pattern(
      regexp = "^[\\w.\\-/]+(:[\\w.\\-]+)?$",
      message =
          "Docker image must be a valid image reference (e.g. image, registry/image, registry/image:tag)")
  private String dockerImage;
  @NotEmpty(message = "At least one sub task is required")
  @Valid
  private List<SubTaskRequestDto> subTasks;
}
