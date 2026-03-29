package com.pm4.istp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseDetailResponseDto {
    private UUID id;
    private String title;
    private String description;
    @JsonProperty("isPublished")
    private boolean isPublished;
    private List<CourseDetailInstructorResponseDto> courseInstructors;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
