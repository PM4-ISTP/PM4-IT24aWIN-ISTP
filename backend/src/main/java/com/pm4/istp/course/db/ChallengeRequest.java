package com.pm4.istp.course.db;

import com.pm4.istp.course.db.entities.ChallengeType;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChallengeRequest {
  private UUID id;
  private String title;
  private String description;
  private String flag;
  private int orderIndex;
  private ChallengeType type;
  private int points;
  private String hint;
  private List<ChallengeOptionRequest> options;
}
