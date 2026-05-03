package com.pm4.istp.course.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChoiceSubmissionRequestDto {
  @NotNull(message = "selectedOptionId is required")
  private UUID selectedOptionId;

  /** The course context from which the student is solving this sub-task. Required for MC mode. */
  @NotNull(message = "courseId is required")
  private UUID courseId;
}
