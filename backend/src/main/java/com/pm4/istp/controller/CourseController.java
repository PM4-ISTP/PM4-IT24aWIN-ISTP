package com.pm4.istp.controller;

import com.pm4.istp.domain.CreateCourseRequest;
import com.pm4.istp.domain.entites.Course;
import com.pm4.istp.dto.CreateCourseRequestDto;
import com.pm4.istp.dto.CreateCourseResponseDto;
import com.pm4.istp.mappers.CourseMapper;
import com.pm4.istp.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseMapper courseMapper;
    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<CreateCourseResponseDto> createCourse(
//            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCourseRequestDto createCourseRequestDto
            )
    {
        CreateCourseRequest createCourseRequest = courseMapper.fromDto(createCourseRequestDto);
//        UUID userId = JwtUtil.parseUserId(jwt);
        Course createdCourse = courseService.createCourse(createCourseRequest);
        CreateCourseResponseDto createCourseResponseDto = courseMapper.toDto(createdCourse);
        return new ResponseEntity<>(createCourseResponseDto, HttpStatus.CREATED);
    }
}
