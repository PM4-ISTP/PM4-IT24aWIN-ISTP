package com.pm4.istp.course.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CourseLabDeadlineDto {
  private UUID courseId;
  private String courseTitle;
  private UUID labId;
  private String labTitle;
  private LocalDateTime dueAt;
}
