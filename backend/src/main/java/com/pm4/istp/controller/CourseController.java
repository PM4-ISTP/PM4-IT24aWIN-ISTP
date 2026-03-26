package com.pm4.istp.controller;

import com.pm4.istp.domain.CreateCourseRequest;
import com.pm4.istp.domain.entites.Course;
import com.pm4.istp.dto.CreateCourseRequestDto;
import com.pm4.istp.dto.CreateCourseResponseDto;
import com.pm4.istp.dto.ErrorDto;
import com.pm4.istp.dto.ListCourseResponseDto;
import com.pm4.istp.mappers.CourseMapper;
import com.pm4.istp.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.pm4.istp.util.JwtUtil.parseUserId;

@Tag(name = "Course", description = "Course endpoints for the API")
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseMapper courseMapper;
    private final CourseService courseService;

    @Operation(
            summary = "Create a course",
            description = "Creates a new course with its primary instructor and returns the persisted course.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Course created successfully",
                    content = @Content(schema = @Schema(implementation = CreateCourseResponseDto.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or referenced user not found",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @PostMapping
    public ResponseEntity<CreateCourseResponseDto> createCourse(
            @AuthenticationPrincipal
            @Valid @RequestBody CreateCourseRequestDto createCourseRequestDto
            )
    {
        CreateCourseRequest createCourseRequest = courseMapper.fromDto(createCourseRequestDto);
        Course createdCourse = courseService.createCourse(createCourseRequest);
        CreateCourseResponseDto createCourseResponseDto = courseMapper.toDto(createdCourse);
        return new ResponseEntity<>(createCourseResponseDto, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<ListCourseResponseDto>> listCourses(
            @AuthenticationPrincipal Jwt jwt, Pageable pageable
    ) {
        UUID userId = parseUserId(jwt);
        Page<Course> courses = courseService.listCoursesForInstructors(userId, pageable);
        return ResponseEntity.ok(
                courses.map(courseMapper::toListCourseResponseDto)
        );
    }
}
