package com.pm4.istp.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChallengeSubmissionResponseDto {
  @JsonProperty("isCorrect")
  private boolean correct;

  @JsonProperty("isChallengeSolved")
  private boolean challengeSolved;

  private int solvedChallengeCount;
  private int totalChallengeCount;
}
