package com.pm4.istp.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCourseRequest {
    private String title;
    private String description;
    private boolean isPublished;
    private List<CreateCourseInstructorRequest> instructors;
}
