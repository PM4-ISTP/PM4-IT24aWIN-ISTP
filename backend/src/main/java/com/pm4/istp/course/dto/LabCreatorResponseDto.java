package com.pm4.istp.course.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChallengeCreatorResponseDto {
  private UUID id;
  private String name;
}
