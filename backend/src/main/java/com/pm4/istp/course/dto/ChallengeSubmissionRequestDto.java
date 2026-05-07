package com.pm4.istp.course.dto;

import com.pm4.istp.shared.validation.ValidFlag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChallengeSubmissionRequestDto {
  @NotNull(message = "courseId is required")
  private UUID courseId;

  @NotBlank(message = "Flag must not be blank")
  @ValidFlag
  private String flag;
}
