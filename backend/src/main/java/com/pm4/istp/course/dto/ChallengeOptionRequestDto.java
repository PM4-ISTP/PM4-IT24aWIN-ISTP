package com.pm4.istp.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChallengeOptionRequestDto {
  private UUID id;

  @NotBlank(message = "Option text is required")
  @Size(max = 500, message = "Option text must not exceed 500 characters")
  private String text;

  @JsonProperty("isCorrect")
  private boolean correct;

  private int orderIndex;
}
