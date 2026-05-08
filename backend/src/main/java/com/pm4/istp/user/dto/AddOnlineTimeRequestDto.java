package com.pm4.istp.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class AddOnlineTimeRequestDto {
  /** Number of seconds to add to the user's total online time. Capped at 3600 (1 hour) per call. */
  @Min(0)
  @Max(3600)
  private long seconds;
}
