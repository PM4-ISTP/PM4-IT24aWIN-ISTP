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

  @JsonProperty("isPrivate")
  private boolean isPrivate;

  private String imageUrl;
  private String topic;

  private String inviteCode;

  private List<CourseDetailInstructorResponseDto> courseInstructors;
  private List<CourseParticipantResponseDto> participants;
  private List<CourseLabResponseDto> courseLabs;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  /** "ONCE" or "UNLIMITED" – controls MC attempt behaviour for students. */
  private String mcAttemptsMode;
}
