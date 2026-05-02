package com.pm4.istp.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pm4.istp.course.db.entities.SubTaskType;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Student-facing sub-task DTO. Intentionally omits the {@code flag} and option {@code isCorrect}
 * fields so answers are never leaked.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubTaskStudentDto {
  private UUID id;
  private String title;
  private String description;
  private int orderIndex;
  private SubTaskType type;
  private int points;
  private String hint;

  /** Options for MULTIPLE_CHOICE sub-tasks. Empty / null for FLAG type. */
  private List<SubTaskOptionStudentDto> options;

  /**
   * True when this is a theory sub-task (FLAG type with no flag set). The student can complete it
   * by simply reading and clicking Next — no submission required.
   */
  @JsonProperty("isTheory")
  private boolean theory;

  @JsonProperty("isSolved")
  private boolean solved;

  /**
   * The plaintext flag. Populated only when the requesting user has solved this sub-task (FLAG
   * type); {@code null} otherwise.
   */
  private String solvedFlag;

  /**
   * ID of the option the student selected (MULTIPLE_CHOICE type). Populated only after submission.
   */
  private UUID selectedOptionId;
}
