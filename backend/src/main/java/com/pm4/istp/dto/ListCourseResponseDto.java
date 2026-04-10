package com.pm4.istp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pm4.istp.domain.entites.CourseDifficultyEnum;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
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

  private String imageUrl;
  private String topic;
  private CourseDifficultyEnum difficulty;
  private String ownerName;

  // Constructor for queries that don't include ownerName (instructor dashboard)
  public ListCourseResponseDto(
      UUID id,
      String title,
      String description,
      boolean isPublished,
      long instructorCount,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.isPublished = isPublished;
    this.instructorCount = instructorCount;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  // Constructor for catalog queries that include ownerName
  public ListCourseResponseDto(
      UUID id,
      String title,
      String description,
      boolean isPublished,
      long instructorCount,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String imageUrl,
      String topic,
      CourseDifficultyEnum difficulty,
      String ownerName) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.isPublished = isPublished;
    this.instructorCount = instructorCount;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.imageUrl = imageUrl;
    this.topic = topic;
    this.difficulty = difficulty;
    this.ownerName = ownerName;
  }
}
