package com.pm4.istp.admin.dto;

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
public class AdminLabListItemDto {
  private UUID id;
  private String title;
  private String shortDescription;
  private String description;
  private LabStatusEnum status;
  private LabDifficultyEnum difficulty;
  private String dockerImage;
  private Integer containerPort;
  private int maxScore;
  private long courseCount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private UUID creatorId;
  private String creatorName;
  private String creatorUsername;
}
