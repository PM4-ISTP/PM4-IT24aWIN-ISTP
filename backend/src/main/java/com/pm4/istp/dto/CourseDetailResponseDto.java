package com.pm4.istp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pm4.istp.domain.entites.CourseDifficultyEnum;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseDetailResponseDto {
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

  private String inviteCode;

  private List<CourseDetailInstructorResponseDto> courseInstructors;
  private List<CourseParticipantResponseDto> participants;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
