package com.pm4.istp.course.dto;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseLabSubmissionsResponseDto {
  private UUID courseId;
  private List<CourseParticipantResponseDto> participants;
  private List<CourseLabResponseDto> labs;
  private List<CourseLabSubmissionEntryDto> submissions;
}
