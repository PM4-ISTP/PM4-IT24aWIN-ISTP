package com.pm4.istp.course.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCourseLabsRequestDto {
  @NotNull(message = "Challenges list is required")
  @Valid
  private List<CourseLabItemDto> labs;
}
