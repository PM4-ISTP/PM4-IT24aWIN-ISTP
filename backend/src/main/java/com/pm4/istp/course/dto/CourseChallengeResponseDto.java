package com.pm4.istp.course.dto;

import com.pm4.istp.course.db.entities.ChallengeDifficultyEnum;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseChallengeResponseDto {
  private UUID challengeId;
  private String challengeTitle;
  private ChallengeDifficultyEnum difficulty;
  private int orderIndex;
}
