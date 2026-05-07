package com.pm4.istp.course.dto;

import com.pm4.istp.course.db.entities.SubTaskType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseSubTaskSubmissionDetailDto {
  private UUID subTaskId;
  private int orderIndex;
  private String title;
  private SubTaskType type;
  private int points;

  /** True when the platform considers this sub-task completed (attempted/solved). */
  private boolean completed;

  /** Null when there is no submission available. */
  private Boolean correct;

  private LocalDateTime submittedAt;

  /** Present for FLAG sub-tasks when the student submitted something. */
  private String submittedFlag;

  /** Present for MULTIPLE_CHOICE sub-tasks when the student selected an option. */
  private UUID selectedOptionId;

  private String selectedOptionText;

  /** Present for MULTIPLE_CHOICE sub-tasks to show all options to the instructor. */
  private List<SubTaskOptionResponseDto> options;
}
