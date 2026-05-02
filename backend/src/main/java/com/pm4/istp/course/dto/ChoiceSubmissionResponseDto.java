package com.pm4.istp.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChoiceSubmissionResponseDto {
  @JsonProperty("isCorrect")
  private boolean correct;

  @JsonProperty("isChallengeSolved")
  private boolean challengeSolved;

  private int solvedCount;
  private int totalCount;

  /** ID of the correct option. Only populated when the submitted answer was wrong. */
  private UUID correctOptionId;
}
