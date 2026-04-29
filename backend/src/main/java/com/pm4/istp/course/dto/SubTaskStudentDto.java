package com.pm4.istp.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Student-facing sub-task DTO. Intentionally omits the {@code flag} field so it never leaks to
 * non-creator callers.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubTaskStudentDto {
  private UUID id;
  private String title;
  private String description;
  private int orderIndex;

  @JsonProperty("isSolved")
  private boolean solved;

  /**
   * The plaintext flag. Populated only when the requesting user has solved this sub-task; {@code
   * null} otherwise. Returning it back to the solver lets the frontend re-display what they
   * originally submitted.
   */
  private String solvedFlag;
}
