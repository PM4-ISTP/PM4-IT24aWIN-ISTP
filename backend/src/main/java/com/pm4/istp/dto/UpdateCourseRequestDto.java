package com.pm4.istp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCourseRequestDto {
  @NotBlank(message = "Course title is required")
  private String title;

  private String description;

  @NotBlank(message = "Short description is required")
  @Size(max = 200, message = "Short description must be at most 200 characters")
  private String shortDescription;

  @JsonProperty("isPublished")
  private boolean isPublished;

  private String imageUrl;
  private String topic;

  @NotNull(message = "Instructor information is required")
  private List<UpdateCourseInstructorRequestDto> instructors;
}
