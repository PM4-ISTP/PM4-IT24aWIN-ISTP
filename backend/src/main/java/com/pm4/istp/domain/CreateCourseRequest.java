package com.pm4.istp.domain;

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
  private List<CreateCourseInstructorRequest> instructors;
}
