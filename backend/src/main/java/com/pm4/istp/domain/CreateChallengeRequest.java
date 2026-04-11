package com.pm4.istp.domain;

import com.pm4.istp.domain.entites.ChallengeDifficultyEnum;
import com.pm4.istp.domain.entites.ChallengeStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateChallengeRequest {
  private String title;
  private String shortDescription;
  private String description;
  private ChallengeStatusEnum status;
  private ChallengeDifficultyEnum difficulty;
}
