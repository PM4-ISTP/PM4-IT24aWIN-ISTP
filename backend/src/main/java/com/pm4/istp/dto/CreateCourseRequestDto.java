package com.pm4.istp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCourseRequestDto {
    @NotBlank(message = "Course title is required")
    private String title;
    private String description;
    private boolean isPublished;
    @NotNull(message = "Instructor information is required")
    private List<CreateCourseInstructorRequestDto> instructors;
}
