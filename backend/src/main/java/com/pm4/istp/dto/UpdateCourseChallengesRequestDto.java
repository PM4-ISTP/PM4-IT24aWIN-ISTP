package com.pm4.istp.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCourseChallengesRequestDto {
  @NotNull(message = "Challenges list is required")
  private List<CourseChallengeItemDto> challenges;
}
