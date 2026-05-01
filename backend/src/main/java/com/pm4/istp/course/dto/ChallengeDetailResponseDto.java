package com.pm4.istp.course.dto;

import com.pm4.istp.course.db.entities.ChallengeDifficultyEnum;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChallengeDetailResponseDto {
  private UUID id;
  private String title;
  private String shortDescription;
  private String description;
  private ChallengeStatusEnum status;
  private ChallengeDifficultyEnum difficulty;
  private int maxScore;
  private String dockerImage;
  private ChallengeCreatorResponseDto creator;
  private long courseCount;
  private List<SubTaskResponseDto> subTasks;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
