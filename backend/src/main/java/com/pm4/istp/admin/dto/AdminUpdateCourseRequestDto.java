package com.pm4.istp.admin.dto;

import com.pm4.istp.course.db.entities.CourseStatusEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUpdateCourseRequestDto {
  @NotBlank(message = "Title is required")
  @Size(max = 255, message = "Title must be at most 255 characters")
  private String title;

  @Size(max = 5000, message = "Description must be at most 5000 characters")
  private String description;

  @Size(max = 200, message = "Short description must be at most 200 characters")
  private String shortDescription;

  @NotNull(message = "Course status is required")
  private CourseStatusEnum status;

  @Size(max = 24, message = "Topic must be at most 24 characters")
  private String topic;

  @Size(max = 2048, message = "Image URL must be at most 2048 characters")
  private String imageUrl;
}
