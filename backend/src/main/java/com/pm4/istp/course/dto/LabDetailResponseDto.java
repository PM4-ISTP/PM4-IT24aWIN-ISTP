package com.pm4.istp.course.dto;

import com.pm4.istp.course.db.entities.LabDifficultyEnum;
import com.pm4.istp.course.db.entities.LabStatusEnum;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LabDetailResponseDto {
  private UUID id;
  private String title;
  private String description;
  private LabStatusEnum status;
  private LabDifficultyEnum difficulty;
  private int maxScore;
  private String dockerImage;
  private Integer containerPort;
  private Integer podTtlSeconds;
  private LabCreatorResponseDto creator;
  private long courseCount;
  private List<ChallengeResponseDto> challenges;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
