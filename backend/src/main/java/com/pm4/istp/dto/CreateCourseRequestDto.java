package com.pm4.istp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pm4.istp.domain.entites.CourseDifficultyEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCourseRequestDto {
  @NotBlank(message = "Course title is required")
  private String title;

  private String description;

  @JsonProperty("isPublished")
  private boolean isPublished;

  private String imageUrl;
  private String topic;
  private CourseDifficultyEnum difficulty;

  @NotNull(message = "Instructor information is required")
  private List<CreateCourseInstructorRequestDto> instructors;
}
