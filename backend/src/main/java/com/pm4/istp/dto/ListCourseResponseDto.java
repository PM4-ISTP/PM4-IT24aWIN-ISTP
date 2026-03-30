package com.pm4.istp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListCourseResponseDto {
  private UUID id;
  private String title;
  private String description;

  @JsonProperty("isPublished")
  private boolean isPublished;

  private long instructorCount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
