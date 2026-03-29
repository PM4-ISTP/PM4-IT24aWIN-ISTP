package com.pm4.istp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCourseRequestDto {
    @NotBlank(message = "Course title is required")
    private String title;
    private String description;
    @JsonProperty("isPublished")
    private boolean isPublished;
    @NotNull(message = "Instructor information is required")
    private List<UpdateCourseInstructorRequestDto> instructors;
}
