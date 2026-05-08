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
  private UUID labId;
  private int solvedChallengeCount;
  private int totalChallengeCount;
  private int awardedPoints;
  private int maxPoints;
  private LocalDateTime completedAt;
  private CourseLabSubmissionStatusEnum status;
}
