package com.pm4.istp.course.db;

import com.pm4.istp.course.db.entities.LabDifficultyEnum;
import com.pm4.istp.course.db.entities.LabStatusEnum;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateLabRequest {
  private String title;
  private String description;
  private LabStatusEnum status;
  private LabDifficultyEnum difficulty;
  private String dockerImage;
  private Integer containerPort;
  private Integer podTtlSeconds;
  private List<ChallengeRequest> challenges;
}
