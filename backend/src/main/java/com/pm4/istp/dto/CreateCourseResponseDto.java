package com.pm4.istp.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCourseResponseDto {
  private UUID id;
  private String title;
  private String description;
  @JsonProperty("isPublished")
  private boolean isPublished;
  private List<CreateCourseInstructorResponseDto> courseInstructors;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
