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
public class CourseLabItemDto {
  @NotNull(message = "Lab ID is required")
  private UUID labId;

  @NotNull(message = "Order index is required")
  private Integer orderIndex;

  // Optional: due date/time for this lab within the course.
  private LocalDateTime dueAt;
}
