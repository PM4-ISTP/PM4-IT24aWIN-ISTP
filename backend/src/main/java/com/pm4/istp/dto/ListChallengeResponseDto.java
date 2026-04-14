package com.pm4.istp.dto;

import com.pm4.istp.domain.entites.ChallengeDifficultyEnum;
import com.pm4.istp.domain.entites.ChallengeStatusEnum;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListChallengeResponseDto {
  private UUID id;
  private String title;
  private String shortDescription;
  private ChallengeStatusEnum status;
  private ChallengeDifficultyEnum difficulty;
  private int maxScore;
  private String creatorName;
  private long courseCount;
  private LocalDateTime updatedAt;
}
