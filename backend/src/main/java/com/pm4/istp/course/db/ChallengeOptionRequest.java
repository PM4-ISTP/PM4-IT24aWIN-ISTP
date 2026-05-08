package com.pm4.istp.course.db;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChallengeOptionRequest {
  private UUID id;
  private String text;
  private boolean correct;
  private int orderIndex;
}
