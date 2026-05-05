package com.pm4.istp.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChallengeOptionResponseDto {
  private UUID id;
  private String text;

  @JsonProperty("isCorrect")
  private boolean correct;

  private int orderIndex;
}
