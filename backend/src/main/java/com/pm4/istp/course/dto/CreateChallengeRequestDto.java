package com.pm4.istp.course.dto;

import com.pm4.istp.course.db.entities.LabDifficultyEnum;
import com.pm4.istp.course.db.entities.LabStatusEnum;
import com.pm4.istp.course.validation.DockerImageReference;
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
public class CreateChallengeRequestDto {
  @NotBlank(message = "Lab title is required")
  @Size(max = 255, message = "Lab title must be at most 255 characters")
  private String title;

  @Size(max = 200, message = "Short description must not exceed 200 characters")
  private String shortDescription;

  @Size(max = 5000, message = "Description must be at most 5000 characters")
  private String description;

  @NotNull(message = "Lab status is required")
  private LabStatusEnum status;

  @NotNull(message = "Lab difficulty is required")
  private LabDifficultyEnum difficulty;

  @NotBlank(message = "Docker image is required")
  @Pattern(
      regexp = DockerImageReference.GHCR_IMAGE_REGEXP,
      message = DockerImageReference.GHCR_IMAGE_MESSAGE)
  private String dockerImage;

  @NotEmpty(message = "At least one sub task is required")
  @Valid
  private List<ChallengeRequestDto> challenges;
}
