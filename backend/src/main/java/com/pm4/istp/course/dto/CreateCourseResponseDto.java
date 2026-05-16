package com.pm4.istp.course.dto;

import com.pm4.istp.course.db.entities.CourseStatusEnum;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCourseResponseDto {
  private UUID id;
  private String title;
  private String description;
  private String shortDescription;

  private CourseStatusEnum status;

  private List<CreateCourseInstructorResponseDto> courseInstructors;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
