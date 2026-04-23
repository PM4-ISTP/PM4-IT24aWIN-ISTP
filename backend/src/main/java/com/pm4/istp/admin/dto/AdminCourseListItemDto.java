package com.pm4.istp.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminCourseListItemDto {
  private UUID id;
  private String title;
  private String description;
  private String shortDescription;

  @JsonProperty("isPublished")
  private boolean isPublished;

  @JsonProperty("isPrivate")
  private boolean isPrivate;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String topic;
  private String imageUrl;
  private UUID ownerId;
  private String ownerName;
  private String ownerUsername;
}
