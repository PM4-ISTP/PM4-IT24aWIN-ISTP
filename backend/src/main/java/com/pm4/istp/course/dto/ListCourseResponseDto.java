package com.pm4.istp.course.dto;

import com.pm4.istp.course.db.entities.CourseStatusEnum;
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

  private CourseStatusEnum status;

  private long instructorCount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private String imageUrl;
  private String topic;
  private String ownerName;
  private String ownerPicture;
  private String ownerTitle;

  // Constructor for JPQL "new" expressions -- parameter order must match the queries in
  // CourseRepository
  @SuppressWarnings("java:S107")
  public ListCourseResponseDto(
      UUID id,
      String title,
      String description,
      String shortDescription,
      CourseStatusEnum status,
      long instructorCount,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String imageUrl,
      String topic,
      String ownerName,
      String ownerPicture,
      String ownerTitle) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.shortDescription = shortDescription;
    this.status = status;
    this.instructorCount = instructorCount;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.imageUrl = imageUrl;
    this.topic = topic;
    this.ownerName = ownerName;
    this.ownerPicture = ownerPicture;
    this.ownerTitle = ownerTitle;
  }
}
