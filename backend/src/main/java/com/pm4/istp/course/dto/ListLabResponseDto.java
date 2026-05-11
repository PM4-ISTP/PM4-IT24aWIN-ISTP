package com.pm4.istp.course.dto;

import com.pm4.istp.course.db.entities.LabDifficultyEnum;
import com.pm4.istp.course.db.entities.LabStatusEnum;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListLabResponseDto {
  private UUID id;
  private String title;
  private String shortDescription;
  private LabStatusEnum status;
  private LabDifficultyEnum difficulty;
  private int maxScore;
  private String dockerImage;
  private Integer containerPort;
  private Integer podTtlSeconds;
  private String creatorName;
  private long courseCount;
  private LocalDateTime updatedAt;
}
