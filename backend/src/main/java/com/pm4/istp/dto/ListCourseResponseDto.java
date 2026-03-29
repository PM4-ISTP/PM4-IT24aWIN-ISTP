package com.pm4.istp.dto;

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
  private boolean isPublished;
  private int instructorCount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
