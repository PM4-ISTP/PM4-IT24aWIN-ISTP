package com.pm4.istp.course.dto;

import com.pm4.istp.course.db.entities.LabDifficultyEnum;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseLabResponseDto {
  private UUID labId;
  private String labTitle;
  private LabDifficultyEnum difficulty;
  private int orderIndex;
  private LocalDateTime dueAt;
  private int maxScore;
}
