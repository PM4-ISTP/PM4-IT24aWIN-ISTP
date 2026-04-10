package com.pm4.istp.domain;

import com.pm4.istp.domain.entites.CourseDifficultyEnum;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCourseRequest {
  private String title;
  private String description;
  private boolean isPublished;
  private String imageUrl;
  private String topic;
  private CourseDifficultyEnum difficulty;
  private List<CreateCourseInstructorRequest> instructors;
}
