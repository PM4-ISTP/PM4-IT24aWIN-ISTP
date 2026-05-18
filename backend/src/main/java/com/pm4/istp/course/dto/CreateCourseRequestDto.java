package com.pm4.istp.course.dto;

import com.pm4.istp.course.db.entities.CourseStatusEnum;
import com.pm4.istp.course.db.entities.McAttemptsMode;
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
public class CreateCourseRequestDto {
  @NotBlank(message = "Course title is required")
  @Size(max = 255, message = "Course title must be at most 255 characters")
  private String title;

  @NotBlank(message = "Description is required")
  @Size(max = 5000, message = "Description must be at most 5000 characters")
  private String description;

  @NotBlank(message = "Short description is required")
  @Size(max = 200, message = "Short description must be at most 200 characters")
  private String shortDescription;

  @NotNull(message = "Course status is required")
  private CourseStatusEnum status;

  @Size(max = 2048, message = "Image URL must be at most 2048 characters")
  private String imageUrl;

  @Size(max = 24, message = "Topic must be at most 24 characters")
  private String topic;

  @NotNull(message = "Instructor information is required")
  private List<CreateCourseInstructorRequestDto> instructors;

  /**
   * How many MC attempts students get. ONCE = 1 attempt (graded); UNLIMITED = retry until correct
   * (self-learning). Defaults to UNLIMITED.
   */
  private McAttemptsMode mcAttemptsMode = McAttemptsMode.UNLIMITED;
}
