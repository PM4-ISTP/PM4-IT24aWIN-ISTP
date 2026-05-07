package com.pm4.istp.course.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseLabChallengeSubmissionDetailDto {
  private UUID challengeId;
  private String title;
  private String type;
  private int maxPoints;
  private boolean completed;
  private Boolean correct;
  private Integer awardedPoints;
  private Integer overridePoints;
  private String submittedFlag;
  private String selectedOptionText;
}
