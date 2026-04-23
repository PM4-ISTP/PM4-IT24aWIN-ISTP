package com.pm4.istp.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminUpdateCourseRequestDto {
  @NotBlank private String title;
  private String description;
  private String shortDescription;

  @JsonProperty("isPublished")
  private boolean isPublished;

  @JsonProperty("isPrivate")
  private boolean isPrivate;
  private String topic;
  private String imageUrl;
}
