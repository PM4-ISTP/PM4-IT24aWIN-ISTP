package com.pm4.istp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pm4.istp.domain.entites.CourseDifficultyEnum;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
public class ListCourseResponseDto {
  private UUID id;
  private String title;
  private String description;
  private String shortDescription;

  @JsonProperty("isPublished")
  private boolean isPublished;

  private long instructorCount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private String imageUrl;
  private String topic;
  private CourseDifficultyEnum difficulty;
  private String ownerName;
  private String ownerPicture;
  private String ownerTitle;

  // Constructor for JPQL "new" expressions — parameter order must match the queries in
  // CourseRepository
  @SuppressWarnings("java:S107")
  public ListCourseResponseDto(
      UUID id,
      String title,
      String description,
      String shortDescription,
      boolean isPublished,
      long instructorCount,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String imageUrl,
      String topic,
      CourseDifficultyEnum difficulty,
      String ownerName,
      String ownerPicture,
      String ownerTitle) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.shortDescription = shortDescription;
    this.isPublished = isPublished;
    this.instructorCount = instructorCount;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.imageUrl = imageUrl;
    this.topic = topic;
    this.difficulty = difficulty;
    this.ownerName = ownerName;
    this.ownerPicture = ownerPicture;
    this.ownerTitle = ownerTitle;
  }
}
