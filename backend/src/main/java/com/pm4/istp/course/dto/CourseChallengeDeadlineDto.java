package com.pm4.istp.course.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CourseChallengeDeadlineDto {
  private UUID courseId;
  private String courseTitle;
  private UUID challengeId;
  private String challengeTitle;
  private LocalDateTime dueAt;
}
