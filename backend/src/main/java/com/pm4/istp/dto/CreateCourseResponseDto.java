package com.pm4.istp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCourseResponseDto {
    private UUID id;
    private String title;
    private String description;
    private boolean isPublished;
    private List<CreateCourseInstructorResponseDto> courseInstructors;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
