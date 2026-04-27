package com.pm4.istp.course.controllers;

import static com.pm4.istp.shared.util.JwtUtil.parseUserId;

import com.pm4.istp.course.db.CreateCourseRequest;
import com.pm4.istp.course.db.UpdateCourseRequest;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.db.entities.CourseEnrollment;
import com.pm4.istp.course.dto.ChallengeDetailResponseDto;
import com.pm4.istp.course.dto.CourseDetailInstructorResponseDto;
import com.pm4.istp.course.dto.CourseDetailResponseDto;
import com.pm4.istp.course.dto.CourseParticipantResponseDto;
import com.pm4.istp.course.dto.CreateCourseRequestDto;
import com.pm4.istp.course.dto.CreateCourseResponseDto;
import com.pm4.istp.course.dto.JoinByInviteCodeRequestDto;
import com.pm4.istp.course.dto.ListCourseResponseDto;
import com.pm4.istp.course.dto.PublicCourseDetailResponseDto;
import com.pm4.istp.course.dto.UpdateCourseChallengesRequestDto;
import com.pm4.istp.course.dto.UpdateCourseRequestDto;
import com.pm4.istp.course.mappers.CourseMapper;
import com.pm4.istp.course.repositories.CourseEnrollmentRepository;
import com.pm4.istp.course.services.CourseService;
import com.pm4.istp.shared.dto.ErrorDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
      summary = "Remove a participant from a course",
      description = "Removes a student (participant) from a course. Only accessible to the owner.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Participant removed successfully"),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Course or participant not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @DeleteMapping("/{id}/participants/{participantId}")
  public ResponseEntity<Void> removeParticipant(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable UUID participantId) {
    UUID userId = parseUserId(jwt);
    courseService.removeParticipant(userId, id, participantId);
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
  public ResponseEntity<PublicCourseDetailResponseDto> getPublicCourse(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    UUID userId = parseUserId(jwt);
    Course course = courseService.getCourse(userId, id);
    PublicCourseDetailResponseDto dto = toPublicCourseDetailResponseDto(course, userId);
    return ResponseEntity.ok(dto);
  }

  @PostMapping("/catalog/{id}/enroll")
  public ResponseEntity<PublicCourseDetailResponseDto> enrollInPublicCourse(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    UUID userId = parseUserId(jwt);
    Course course = courseService.enrollInCourse(userId, id);
    PublicCourseDetailResponseDto dto = toPublicCourseDetailResponseDto(course, userId);
    return ResponseEntity.ok(dto);
  }

  @GetMapping("/my-enrollments")
  public ResponseEntity<Page<ListCourseResponseDto>> listEnrollments(
      @AuthenticationPrincipal Jwt jwt, Pageable pageable) {
    UUID userId = parseUserId(jwt);
    Page<ListCourseResponseDto> courses = courseService.listUserEnrollments(userId, pageable);
    return ResponseEntity.ok(courses);
  }

  @PostMapping("/catalog/join")
  public ResponseEntity<PublicCourseDetailResponseDto> joinByInviteCode(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody JoinByInviteCodeRequestDto request) {
    UUID userId = parseUserId(jwt);
    Course course = courseService.joinByInviteCode(request.getCode(), userId);
    PublicCourseDetailResponseDto dto = toPublicCourseDetailResponseDto(course, userId);
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

  /** Public catalog detail – omits participant list; returns only count and enrollment status. */
  private PublicCourseDetailResponseDto toPublicCourseDetailResponseDto(
      Course course, UUID userId) {
    PublicCourseDetailResponseDto dto = courseMapper.toPublicCourseDetailDto(course);
    UUID courseId = course.getId();
    dto.setParticipantCount(courseEnrollmentRepository.countByCourseId(courseId));
    dto.setEnrolled(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId));
    dto.setParticipants(null);
    filterOutNonPublicChallenges(dto);
    setInstructorIdsToNull(dto.getCourseInstructors());
    setChallengeCreatorIdsToNull(dto.getCourseChallenges());
    dto.setInviteCode(null);
    return dto;
  }

  private void filterOutNonPublicChallenges(PublicCourseDetailResponseDto dto) {
    List<ChallengeDetailResponseDto> challenges = new ArrayList<>();
    for (ChallengeDetailResponseDto challenge : dto.getCourseChallenges()) {
      if (challenge.getStatus() == ChallengeStatusEnum.PUBLIC) {
        challenges.add(challenge);
      }
    }
    dto.setCourseChallenges(List.copyOf(challenges));
  }

  private void setInstructorIdsToNull(List<CourseDetailInstructorResponseDto> courseInstructors) {
    for (CourseDetailInstructorResponseDto courseInstructor : courseInstructors) {
      courseInstructor.setId(null);
      courseInstructor.getInstructor().setId(null);
    }
  }

  private void setChallengeCreatorIdsToNull(List<ChallengeDetailResponseDto> courseChallenges) {
    for (ChallengeDetailResponseDto challengeDetailResponseDto : courseChallenges) {
      challengeDetailResponseDto.getCreator().setId(null);
    }
  }
}
