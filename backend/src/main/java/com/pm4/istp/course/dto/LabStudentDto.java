package com.pm4.istp.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pm4.istp.course.db.entities.LabDifficultyEnum;
import com.pm4.istp.course.db.entities.LabStatusEnum;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Student-facing lab DTO — used both on the public course detail page and on the play view. Carries
 * no challenge flags; instead includes per-student progress ({@code solvedChallengeCount}, {@code
 * totalChallengeCount}, {@code solved}).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LabStudentDto {
  private UUID id;
  private String title;
  private String shortDescription;
  private String description;
  private LabStatusEnum status;
  private LabDifficultyEnum difficulty;
  private String dockerImage;
  private Integer containerPort;
  private int maxScore;
  private ChallengeCreatorResponseDto creator;
  private List<ChallengeStudentDto> challenges;
  private int solvedChallengeCount;
  private int totalChallengeCount;

  /** Due date/time in the context of a course assignment (can be null). */
  private LocalDateTime dueAt;

  @JsonProperty("isSolved")
  private boolean solved;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  /**
   * MC attempt mode inherited from the course context. "ONCE" = one attempt regardless of
   * correctness; "UNLIMITED" = retry until correct. Only populated on the play view.
   */
  private String mcAttemptsMode;
}
