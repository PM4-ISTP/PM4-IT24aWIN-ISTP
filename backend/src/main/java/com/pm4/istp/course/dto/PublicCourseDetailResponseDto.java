package com.pm4.istp.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PublicCourseDetailResponseDto {
  private UUID id;
  private String title;
  private String description;
  private String shortDescription;
  private long participantCount;

  @JsonProperty("isEnrolled")
  private boolean isEnrolled;

  @JsonProperty("isPublished")
  private boolean isPublished;

  private String imageUrl;
  private String topic;

  private String inviteCode;

  private List<CourseDetailInstructorResponseDto> courseInstructors;
  private List<CourseParticipantResponseDto> participants;
  private List<ChallengeStudentDto> courseChallenges;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
