package com.pm4.istp.controller;

import static com.pm4.istp.util.JwtUtil.parseUserId;

import com.pm4.istp.domain.CreateCourseRequest;
import com.pm4.istp.domain.UpdateCourseRequest;
import com.pm4.istp.domain.entites.Course;
import com.pm4.istp.domain.entites.CourseEnrollment;
import com.pm4.istp.dto.*;
import com.pm4.istp.mappers.CourseMapper;
import com.pm4.istp.repositories.CourseEnrollmentRepository;
import com.pm4.istp.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Course", description = "Course endpoints for the API")
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {
  private final CourseMapper courseMapper;
  private final CourseService courseService;
  private final CourseEnrollmentRepository courseEnrollmentRepository;

  @Operation(
      summary = "Create a course",
      description =
          "Creates a new course with its primary instructor and returns the persisted course.")
  @ApiResponses(
      value = {
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
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody CreateCourseRequestDto createCourseRequestDto) {
    UUID userId = parseUserId(jwt);
    CreateCourseRequest createCourseRequest = courseMapper.fromDto(createCourseRequestDto);
    Course createdCourse = courseService.createCourse(userId, createCourseRequest);
    CreateCourseResponseDto createCourseResponseDto = courseMapper.toDto(createdCourse);
    return new ResponseEntity<>(createCourseResponseDto, HttpStatus.CREATED);
  }

  @Operation(
      summary = "Get a course by ID",
      description = "Returns the full details of a course including all instructor assignments.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Course found",
            content = @Content(schema = @Schema(implementation = CourseDetailResponseDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Course not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @GetMapping("/{id}")
  public ResponseEntity<CourseDetailResponseDto> getCourse(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    UUID userId = parseUserId(jwt);
    Course course = courseService.getCourse(userId, id);
    CourseDetailResponseDto dto = toCourseDetailResponseDto(course, userId);
    return ResponseEntity.ok(dto);
  }

  @PostMapping("/{id}/enroll")
  public ResponseEntity<CourseDetailResponseDto> enrollInCourse(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    UUID userId = parseUserId(jwt);
    Course course = courseService.enrollInCourse(userId, id);
    CourseDetailResponseDto dto = toCourseDetailResponseDto(course, userId);
    return ResponseEntity.ok(dto);
  }

  @Operation(
      summary = "Update a course",
      description = "Updates an existing course's details and instructor assignments.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Course updated successfully",
            content = @Content(schema = @Schema(implementation = CourseDetailResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data or referenced user not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Course not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PutMapping("/{id}")
  public ResponseEntity<CourseDetailResponseDto> updateCourse(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateCourseRequestDto updateCourseRequestDto) {
    UUID userId = parseUserId(jwt);
    UpdateCourseRequest updateCourseRequest = courseMapper.fromDto(updateCourseRequestDto);
    Course updatedCourse = courseService.updateCourse(userId, id, updateCourseRequest);
    CourseDetailResponseDto dto = toCourseDetailResponseDto(updatedCourse, userId);
    return ResponseEntity.ok(dto);
  }

  @Operation(
      summary = "Delete a course",
      description = "Deletes a course by ID. Only accessible to the owner of that course.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Course deleted successfully"),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Course not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCourse(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    UUID userId = parseUserId(jwt);
    courseService.deleteCourse(userId, id);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Update course challenges",
      description = "Replaces the challenge list for a course. Accepts own and public challenges.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Course challenges updated successfully",
            content = @Content(schema = @Schema(implementation = CourseDetailResponseDto.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Course or challenge not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PutMapping("/{id}/challenges")
  public ResponseEntity<CourseDetailResponseDto> updateCourseChallenges(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateCourseChallengesRequestDto request) {
    UUID userId = parseUserId(jwt);
    Course updatedCourse =
        courseService.updateCourseChallenges(userId, id, request.getChallenges());
    CourseDetailResponseDto dto = courseMapper.toCourseDetailDto(updatedCourse);
    return ResponseEntity.ok(dto);
  }

  @GetMapping
  public ResponseEntity<Page<ListCourseResponseDto>> listCourses(
      @AuthenticationPrincipal Jwt jwt, Pageable pageable) {
    UUID userId = parseUserId(jwt);
    Page<ListCourseResponseDto> courses = courseService.listCoursesForInstructors(userId, pageable);
    return ResponseEntity.ok(courses);
  }

  @GetMapping("/catalog")
  public ResponseEntity<Page<ListCourseResponseDto>> listPublishedCourses(
      @RequestParam(required = false) String query, Pageable pageable) {
    Page<ListCourseResponseDto> courses = courseService.listPublishedCourses(query, pageable);
    return ResponseEntity.ok(courses);
  }

  // ── Public catalog endpoints ── accessible to all authenticated users (including students)
  @GetMapping("/catalog/{id}")
  public ResponseEntity<CourseDetailResponseDto> getPublicCourse(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    UUID userId = parseUserId(jwt);
    Course course = courseService.getCourse(userId, id);
    CourseDetailResponseDto dto = toPublicCourseDetailResponseDto(course, userId);
    return ResponseEntity.ok(dto);
  }

  @PostMapping("/catalog/{id}/enroll")
  public ResponseEntity<CourseDetailResponseDto> enrollInPublicCourse(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    UUID userId = parseUserId(jwt);
    Course course = courseService.enrollInCourse(userId, id);
    CourseDetailResponseDto dto = toPublicCourseDetailResponseDto(course, userId);
    return ResponseEntity.ok(dto);
  }

  @PostMapping("/catalog/join")
  public ResponseEntity<CourseDetailResponseDto> joinByInviteCode(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody JoinByInviteCodeRequestDto request) {
    UUID userId = parseUserId(jwt);
    Course course = courseService.joinByInviteCode(request.getCode(), userId);
    CourseDetailResponseDto dto = toPublicCourseDetailResponseDto(course, userId);
    return ResponseEntity.ok(dto);
  }

  @PostMapping("/{id}/invite-code/regenerate")
  public ResponseEntity<CourseDetailResponseDto> regenerateInviteCode(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    UUID userId = parseUserId(jwt);
    Course course = courseService.regenerateInviteCode(id, userId);
    CourseDetailResponseDto dto = toCourseDetailResponseDto(course, userId);
    return ResponseEntity.ok(dto);
  }

  // ── Private helpers ──

  /** Full detail including participant list – for instructor/owner endpoints. */
  private CourseDetailResponseDto toCourseDetailResponseDto(Course course, UUID userId) {
    CourseDetailResponseDto dto = courseMapper.toCourseDetailDto(course);
    UUID courseId = course.getId();
    dto.setParticipantCount(courseEnrollmentRepository.countByCourseId(courseId));
    dto.setEnrolled(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId));
    List<CourseEnrollment> enrollments =
        courseEnrollmentRepository.findByCourseIdFetchParticipant(courseId);
    List<CourseParticipantResponseDto> participants =
        enrollments.stream()
            .map(
                e -> {
                  var p = e.getParticipant();
                  return new CourseParticipantResponseDto(p.getId(), p.getName(), p.getPicture());
                })
            .toList();
    dto.setParticipants(participants);
    boolean callerIsInstructor =
        course.getCourseInstructors().stream()
            .anyMatch(ci -> ci.getInstructor().getId().equals(userId));
    if (!callerIsInstructor) {
      dto.setInviteCode(null);
    }
    return dto;
  }

  /**
   * Public catalog detail – omits participant list and invite code; returns only count and
   * enrollment status.
   */
  private CourseDetailResponseDto toPublicCourseDetailResponseDto(Course course, UUID userId) {
    CourseDetailResponseDto dto = courseMapper.toCourseDetailDto(course);
    UUID courseId = course.getId();
    dto.setParticipantCount(courseEnrollmentRepository.countByCourseId(courseId));
    dto.setEnrolled(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId));
    dto.setParticipants(null);
    dto.setInviteCode(null);
    return dto;
  }
}
