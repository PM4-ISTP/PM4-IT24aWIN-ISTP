package com.pm4.istp.course.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseChallengeSubmissionEntryDto {
  private UUID participantId;
  private UUID challengeId;
  private int solvedSubTaskCount;
  private int totalSubTaskCount;
  private LocalDateTime completedAt;
  private CourseChallengeSubmissionStatusEnum status;
}

