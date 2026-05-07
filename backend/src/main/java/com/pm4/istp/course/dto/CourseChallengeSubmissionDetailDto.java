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
public class CourseChallengeSubmissionDetailDto {
  private UUID courseId;
  private UUID participantId;
  private String participantName;
  private String participantEmail;
  private UUID challengeId;
  private String challengeTitle;

  private int awardedPoints;
  private int maxPoints;

  private LocalDateTime dueAt;
  private LocalDateTime completedAt;
  private CourseChallengeSubmissionStatusEnum status;

  private List<CourseSubTaskSubmissionDetailDto> subTasks;
}
