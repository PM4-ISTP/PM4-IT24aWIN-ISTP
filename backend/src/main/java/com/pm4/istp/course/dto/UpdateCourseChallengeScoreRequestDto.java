package com.pm4.istp.course.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCourseChallengeScoreRequestDto {
  @Min(0)
  private Integer points;
}
