package com.pm4.istp.course.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseLabSubmissionDetailDto {
  private UUID courseId;
  private UUID participantId;
  private UUID labId;
  private String labTitle;
  private LocalDateTime dueAt;
  private LocalDateTime completedAt;
  private CourseLabSubmissionStatusEnum status;
  private int awardedPoints;
  private int maxPoints;
  private List<CourseLabChallengeSubmissionDetailDto> challenges;
}

