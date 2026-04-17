package com.pm4.istp.course.db;

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
  private String shortDescription;
  private boolean isPublished;
  private boolean isPrivate;
  private String imageUrl;
  private String topic;
  private List<CreateCourseInstructorRequest> instructors;
}
