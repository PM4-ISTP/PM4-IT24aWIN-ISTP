package com.pm4.istp.course.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Student-facing option DTO. Intentionally omits {@code isCorrect} so the answer is never leaked.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubTaskOptionStudentDto {
  private UUID id;
  private String text;
  private int orderIndex;
}
