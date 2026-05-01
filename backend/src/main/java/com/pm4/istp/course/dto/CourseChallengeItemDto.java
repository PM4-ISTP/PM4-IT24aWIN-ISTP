package com.pm4.istp.course.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseChallengeItemDto {
  @NotNull(message = "Challenge ID is required")
  private UUID challengeId;

  @NotNull(message = "Order index is required")
  private Integer orderIndex;

  // Optional: due date/time for this challenge within the course.
  private LocalDateTime dueAt;
}
