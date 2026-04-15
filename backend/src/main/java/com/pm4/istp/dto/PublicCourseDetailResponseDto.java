package com.pm4.istp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pm4.istp.domain.entites.CourseDifficultyEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
  private CourseDifficultyEnum difficulty;

  private List<CourseDetailInstructorResponseDto> courseInstructors;
  private List<CourseParticipantResponseDto> participants;
  private List<ChallengeDetailResponseDto> courseChallenges;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
