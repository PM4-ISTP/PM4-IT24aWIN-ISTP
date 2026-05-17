package com.pm4.istp.course.controllers;

import static com.pm4.istp.shared.util.JwtUtil.parseUserId;

import com.pm4.istp.course.db.CreateCourseRequest;
import com.pm4.istp.course.db.InstructorRoleEnum;
import com.pm4.istp.course.db.UpdateCourseRequest;
import com.pm4.istp.course.db.entities.ChallengeType;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.db.entities.CourseEnrollment;
import com.pm4.istp.course.db.entities.LabStatusEnum;
import com.pm4.istp.course.db.entities.StudentOptionSubmission;
import com.pm4.istp.course.dto.ChallengeStudentDto;
import com.pm4.istp.course.dto.CourseDetailInstructorResponseDto;
import com.pm4.istp.course.dto.CourseDetailResponseDto;
import com.pm4.istp.course.dto.CourseLabDeadlineDto;
import com.pm4.istp.course.dto.CourseLabSubmissionDetailDto;
import com.pm4.istp.course.dto.CourseLabSubmissionEntryDto;
import com.pm4.istp.course.dto.CourseLabSubmissionsResponseDto;
import com.pm4.istp.course.dto.CourseParticipantResponseDto;
import com.pm4.istp.course.dto.CreateCourseRequestDto;
import com.pm4.istp.course.dto.CreateCourseResponseDto;
import com.pm4.istp.course.dto.JoinByInviteCodeRequestDto;
import com.pm4.istp.course.dto.LabStudentDto;
import com.pm4.istp.course.dto.ListCourseResponseDto;
import com.pm4.istp.course.dto.PublicCourseDetailResponseDto;
import com.pm4.istp.course.dto.UpdateCourseChallengeScoreRequestDto;
import com.pm4.istp.course.dto.UpdateCourseLabsRequestDto;
import com.pm4.istp.course.dto.UpdateCourseRequestDto;
import com.pm4.istp.course.mappers.CourseMapper;
import com.pm4.istp.course.repositories.ChallengeCompletionRepository;
import com.pm4.istp.course.repositories.ChallengeRepository;
import com.pm4.istp.course.repositories.CourseEnrollmentRepository;
import com.pm4.istp.course.repositories.StudentOptionSubmissionRepository;
import com.pm4.istp.course.services.CourseService;
import com.pm4.istp.course.services.CourseTopicService;
import com.pm4.istp.shared.dto.ErrorDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  private final CourseTopicService courseTopicService;
  private final ChallengeCompletionRepository challengeCompletionRepository;
  private final ChallengeRepository challengeRepository;
  private final StudentOptionSubmissionRepository studentOptionSubmissionRepository;

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
      summary = "Leave a course",
      description =
          "Allows the authenticated student to remove themselves from a course they are enrolled in.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Successfully left the course"),
        @ApiResponse(
            responseCode = "404",
            description = "Course not found or user not enrolled",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @DeleteMapping("/catalog/{id}/leave")
  public ResponseEntity<Void> leaveCourse(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    UUID userId = parseUserId(jwt);
    courseService.leaveCourse(userId, id);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Update course labs",
      description = "Replaces the lab list for a course. Accepts own and public labs.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Course labs updated successfully",
            content = @Content(schema = @Schema(implementation = CourseDetailResponseDto.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Course or lab not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PutMapping("/{id}/labs")
  public ResponseEntity<CourseDetailResponseDto> updateCourseLabs(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateCourseLabsRequestDto request) {
    UUID userId = parseUserId(jwt);
    Course updatedCourse = courseService.updateCourseLabs(userId, id, request.getLabs());
    CourseDetailResponseDto dto = courseMapper.toCourseDetailDto(updatedCourse);
    return ResponseEntity.ok(dto);
  }

  @Operation(
      summary = "Get course lab submissions",
      description =
          "Returns a per-student/per-lab submission matrix with NOT_STARTED/IN_PROGRESS/SUBMITTED status based on challenge completion.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Submissions loaded successfully",
            content =
                @Content(schema = @Schema(implementation = CourseLabSubmissionsResponseDto.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Course not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @GetMapping("/{id}/submissions")
  public ResponseEntity<CourseLabSubmissionsResponseDto> getCourseSubmissions(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    UUID userId = parseUserId(jwt);
    return ResponseEntity.ok(courseService.getCourseLabSubmissions(userId, id));
  }

  @Operation(
      summary = "Get course lab submission details",
      description =
          "Returns per-challenge details for one participant in one lab, including selected options / submitted flags and the current awarded points (manual overrides included).")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Submission details loaded successfully",
            content =
                @Content(schema = @Schema(implementation = CourseLabSubmissionDetailDto.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Course, participant or lab not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @GetMapping("/{id}/submissions/{participantId}/{labId}")
  public ResponseEntity<CourseLabSubmissionDetailDto> getCourseLabSubmissionDetails(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @PathVariable UUID participantId,
      @PathVariable UUID labId) {
    UUID userId = parseUserId(jwt);
    return ResponseEntity.ok(
        courseService.getCourseLabSubmissionDetails(userId, id, participantId, labId));
  }

  @Operation(
      summary = "Update participant score for a course challenge",
      description =
          "Allows an instructor to manually override points for one participant in one challenge. Valid range is 0 to the challenge max score.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Score updated successfully",
            content =
                @Content(schema = @Schema(implementation = CourseLabSubmissionEntryDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Course, participant or challenge not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PutMapping("/{id}/submissions/{participantId}/{challengeId}/score")
  public ResponseEntity<CourseLabSubmissionEntryDto> updateCourseChallengeScore(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @PathVariable UUID participantId,
      @PathVariable UUID challengeId,
      @Valid @RequestBody UpdateCourseChallengeScoreRequestDto request) {
    UUID userId = parseUserId(jwt);
    return ResponseEntity.ok(
        courseService.updateCourseChallengeScore(userId, id, participantId, challengeId, request));
  }

  @Operation(
      summary = "Get upcoming lab deadlines for current user",
      description =
          "Returns lab assignments (course + lab + dueAt) for courses where the user is enrolled. Only entries with a dueAt deadline are returned. Already submitted labs are not returned (labs with the submission status `SUBMITTED`).")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Deadlines loaded successfully",
            content =
                @Content(
                    array =
                        @ArraySchema(
                            schema = @Schema(implementation = CourseLabDeadlineDto.class))))
      })
  @GetMapping("/my-deadlines")
  public ResponseEntity<List<CourseLabDeadlineDto>> listMyDeadlines(
      @AuthenticationPrincipal Jwt jwt) {
    UUID userId = parseUserId(jwt);
    return ResponseEntity.ok(courseService.listUpcomingDeadlines(userId));
  }

  @Operation(
      summary = "Get courses for which the user is their instructor",
      description =
          "Returns a paginated list of courses for which the user is their instructor (owner or collaborator).")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Courses found"),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @GetMapping
  public ResponseEntity<Page<ListCourseResponseDto>> listCourses(
      @AuthenticationPrincipal Jwt jwt, Pageable pageable) {
    UUID userId = parseUserId(jwt);
    Page<ListCourseResponseDto> courses = courseService.listCoursesForInstructors(userId, pageable);
    return ResponseEntity.ok(courses);
  }

  // ── Public catalog endpoints ── accessible to all authenticated users (including students)

  @Operation(
      summary = "Get published courses",
      description = "Returns a paginated list of published courses.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Courses found"),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @GetMapping("/catalog")
  public ResponseEntity<Page<ListCourseResponseDto>> listPublishedCourses(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) String topic,
      Pageable pageable) {
    String normalizedTopic = courseTopicService.normalizeAndValidate(topic);
    Page<ListCourseResponseDto> courses =
        courseService.listPublishedCourses(query, normalizedTopic, pageable);
    return ResponseEntity.ok(courses);
  }

  @GetMapping("/topics")
  @Operation(
      operationId = "listCourseTopics",
      summary = "List course topics",
      description = "Returns the list of allowed course topics for topic selection UIs.")
  public ResponseEntity<List<String>> listCourseTopics() {
    return ResponseEntity.ok(courseTopicService.listActiveTopics());
  }

  @Operation(
      summary = "Get a course by ID as a student",
      description =
          "Returns a detailed response of a course for a student. User IDs are omitted due to security concerns.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Course found",
            content =
                @Content(schema = @Schema(implementation = PublicCourseDetailResponseDto.class))),
        @ApiResponse(
            responseCode = "403",
            description =
                "This can indicate one of two things. First this course is private and the user is neither an instructor of the course nor enrolled in the course. Second this course is a draft and the user is not an instructor of the course.",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @GetMapping("/catalog/{id}")
  public ResponseEntity<PublicCourseDetailResponseDto> getPublicCourse(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    UUID userId = parseUserId(jwt);
    Course course = courseService.getCourse(userId, id);
    PublicCourseDetailResponseDto dto = toPublicCourseDetailResponseDto(course, userId);
    return ResponseEntity.ok(dto);
  }

  @Operation(
      summary = "Enroll in a course",
      description =
          "Enroll in a course and returns the course in which the student enrolled themselves.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Enrolled in course",
            content =
                @Content(schema = @Schema(implementation = PublicCourseDetailResponseDto.class))),
        @ApiResponse(
            responseCode = "500",
            description =
                "Unexpected server error. Might occur, when user or course does not exist or the course is not public.",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PostMapping("/catalog/{id}/enroll")
  public ResponseEntity<PublicCourseDetailResponseDto> enrollInPublicCourse(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    UUID userId = parseUserId(jwt);
    Course course = courseService.enrollInCourse(userId, id);
    PublicCourseDetailResponseDto dto = toPublicCourseDetailResponseDto(course, userId);
    return ResponseEntity.ok(dto);
  }

  @Operation(
      summary = "Get enrolled courses of user (public and private)",
      description = "Returns a paginated list of the enrolled courses of the user.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Enrollments found"),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @GetMapping("/my-enrollments")
  public ResponseEntity<Page<ListCourseResponseDto>> listEnrollments(
      @AuthenticationPrincipal Jwt jwt, Pageable pageable) {
    UUID userId = parseUserId(jwt);
    Page<ListCourseResponseDto> courses = courseService.listUserEnrollments(userId, pageable);
    return ResponseEntity.ok(courses);
  }

  @Operation(
      summary = "Join a private course by invite code",
      description =
          "Enrolls the authenticated user into a private course using a 6-character invite code.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Joined course successfully",
            content =
                @Content(schema = @Schema(implementation = PublicCourseDetailResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload or user not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Invalid or expired invite code",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PostMapping("/catalog/join")
  public ResponseEntity<PublicCourseDetailResponseDto> joinByInviteCode(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody JoinByInviteCodeRequestDto request) {
    UUID userId = parseUserId(jwt);
    Course course = courseService.joinByInviteCode(request.getCode(), userId);
    PublicCourseDetailResponseDto dto = toPublicCourseDetailResponseDto(course, userId);
    return ResponseEntity.ok(dto);
  }

  @Operation(
      summary = "Regenerate invite code",
      description =
          "Regenerates the invite code of a private course. Only the course owner can perform this action.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Invite code regenerated successfully",
            content = @Content(schema = @Schema(implementation = CourseDetailResponseDto.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied or invite code regeneration not allowed for public course",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Course not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PostMapping("/{id}/invite-code/regenerate")
  public ResponseEntity<CourseDetailResponseDto> regenerateInviteCode(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    UUID userId = parseUserId(jwt);
    Course course = courseService.regenerateInviteCode(id, userId);
    CourseDetailResponseDto dto = toCourseDetailResponseDto(course, userId);
    return ResponseEntity.ok(dto);
  }

  // -- Private helpers --

  /** Full detail including participant list - for instructor/owner endpoints. */
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
                  return new CourseParticipantResponseDto(
                      p.getId(), p.getName(), p.getPicture(), p.getEmail());
                })
            .toList();
    dto.setParticipants(participants);
    boolean callerIsInstructor =
        course.getCourseInstructors().stream()
            .anyMatch(ci -> ci.getInstructor().getId().equals(userId));
    if (!callerIsInstructor) {
      dto.setInviteCode(null);
    }
    boolean callerIsOwner =
        course.getCourseInstructors().stream()
            .anyMatch(
                ci ->
                    ci.getInstructor().getId().equals(userId)
                        && ci.getInstructorRole() == InstructorRoleEnum.OWNER);
    if (!callerIsOwner) {
      dto.setInviteCode(null);
    }
    return dto;
  }

  /** Public catalog detail - omits participant list; returns only count and enrollment status. */
  private PublicCourseDetailResponseDto toPublicCourseDetailResponseDto(
      Course course, UUID userId) {
    PublicCourseDetailResponseDto dto = courseMapper.toPublicCourseDetailDto(course);
    UUID courseId = course.getId();
    dto.setParticipantCount(courseEnrollmentRepository.countByCourseId(courseId));
    dto.setEnrolled(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId));
    dto.setParticipants(null);
    filterOutDraftChallenges(dto);
    setInstructorIdsToNull(dto.getCourseInstructors());
    setChallengeCreatorIdsToNull(dto.getCourseLabs());
    populateStudentProgress(dto.getCourseLabs(), userId);
    dto.setInviteCode(null);
    return dto;
  }

  private void filterOutDraftChallenges(PublicCourseDetailResponseDto dto) {
    List<LabStudentDto> labs = new ArrayList<>();
    for (LabStudentDto lab : dto.getCourseLabs()) {
      if (lab.getStatus() != LabStatusEnum.DRAFT) {
        labs.add(lab);
      }
    }
    dto.setCourseLabs(List.copyOf(labs));
  }

  /**
   * Fills in per-student progress on each lab and its challenges. Reads ChallengeCompletion rows in
   * a single query for all challenges of all visible labs, then marks matching challenges as
   * solved.
   */
  private void populateStudentProgress(List<LabStudentDto> labs, UUID userId) {
    if (labs == null || labs.isEmpty()) {
      return;
    }
    List<UUID> challengeIds = collectChallengeIds(labs);
    Set<UUID> solvedIds =
        challengeIds.isEmpty()
            ? Set.of()
            : new HashSet<>(
                challengeCompletionRepository.findSolvedChallengeIds(userId, challengeIds));

    Map<UUID, String> flagsBySolvedId = loadFlagsForSolved(solvedIds);

    OptionProgress optionProgress = loadOptionProgress(userId, challengeIds);

    for (LabStudentDto lab : labs) {
      applyStudentProgress(
          lab,
          solvedIds,
          flagsBySolvedId,
          optionProgress.selectedOptionByChallenge(),
          optionProgress.correctOptionByChallenge());
    }
  }

  private List<UUID> collectChallengeIds(List<LabStudentDto> labs) {
    List<UUID> challengeIds = new ArrayList<>();
    for (LabStudentDto lab : labs) {
      for (ChallengeStudentDto challenge : safeChallenges(lab)) {
        challengeIds.add(challenge.getId());
      }
    }
    return challengeIds;
  }

  private void applyStudentProgress(
      LabStudentDto lab,
      Set<UUID> solvedIds,
      Map<UUID, String> flagsBySolvedId,
      Map<UUID, UUID> selectedOptionByChallenge,
      Map<UUID, UUID> correctOptionByChallenge) {
    List<ChallengeStudentDto> challenges = safeChallenges(lab);
    int solvedCount = 0;
    for (ChallengeStudentDto challenge : challenges) {
      UUID challengeId = challenge.getId();
      boolean solved = solvedIds.contains(challengeId);
      UUID selectedOptionId = selectedOptionByChallenge.get(challengeId);
      boolean attemptedMc =
          challenge.getType() == ChallengeType.MULTIPLE_CHOICE && selectedOptionId != null;

      boolean completed = solved || attemptedMc;
      challenge.setSolved(completed);
      challenge.setSelectedOptionId(selectedOptionId);
      challenge.setCorrectOptionId(correctOptionByChallenge.get(challengeId));

      if (solved && challenge.getType() == ChallengeType.FLAG) {
        challenge.setSolvedFlag(flagsBySolvedId.get(challengeId));
      }
      if (completed) {
        solvedCount++;
      }
    }
    lab.setTotalChallengeCount(challenges.size());
    lab.setSolvedChallengeCount(solvedCount);
    lab.setSolved(!challenges.isEmpty() && solvedCount == challenges.size());
  }

  private OptionProgress loadOptionProgress(UUID userId, List<UUID> challengeIds) {
    if (challengeIds.isEmpty()) {
      return new OptionProgress(Map.of(), Map.of());
    }

    Map<UUID, UUID> selectedOptionByChallenge = new HashMap<>();
    List<UUID> wrongChallengeIds = new ArrayList<>();

    List<StudentOptionSubmission> submissions =
        studentOptionSubmissionRepository.findByUserIdAndChallengeIdIn(userId, challengeIds);
    if (submissions == null) {
      submissions = List.of();
    }

    for (StudentOptionSubmission submission : submissions) {
      if (submission.getChallenge() == null
          || submission.getChallenge().getId() == null
          || submission.getSelectedOption() == null
          || submission.getSelectedOption().getId() == null) {
        continue;
      }
      UUID challengeId = submission.getChallenge().getId();
      selectedOptionByChallenge.put(challengeId, submission.getSelectedOption().getId());
      if (!submission.isCorrect()) {
        wrongChallengeIds.add(challengeId);
      }
    }

    Map<UUID, UUID> correctOptionByChallenge = new HashMap<>();
    if (!wrongChallengeIds.isEmpty()) {
      for (Object[] row :
          challengeRepository.findCorrectOptionIdsByChallengeIds(wrongChallengeIds)) {
        if (row == null || row.length < 2) {
          continue;
        }
        UUID challengeId = (UUID) row[0];
        UUID correctOptionId = (UUID) row[1];
        if (challengeId != null && correctOptionId != null) {
          correctOptionByChallenge.put(challengeId, correctOptionId);
        }
      }
    }

    return new OptionProgress(selectedOptionByChallenge, correctOptionByChallenge);
  }

  private record OptionProgress(
      Map<UUID, UUID> selectedOptionByChallenge, Map<UUID, UUID> correctOptionByChallenge) {}

  private List<ChallengeStudentDto> safeChallenges(LabStudentDto lab) {
    return lab.getChallenges() == null ? List.of() : lab.getChallenges();
  }

  /**
   * Returns a {@code challengeId → flag} map for the given solved challenge ids. Empty map for an
   * empty input — exposed only for the user's own solved challenges, never to anyone else.
   */
  private Map<UUID, String> loadFlagsForSolved(Set<UUID> solvedIds) {
    if (solvedIds.isEmpty()) {
      return Map.of();
    }
    Map<UUID, String> flagsBySolvedId = new HashMap<>();
    for (Object[] row : challengeRepository.findFlagsByIds(solvedIds)) {
      flagsBySolvedId.put((UUID) row[0], (String) row[1]);
    }
    return flagsBySolvedId;
  }

  private void setInstructorIdsToNull(List<CourseDetailInstructorResponseDto> courseInstructors) {
    for (CourseDetailInstructorResponseDto courseInstructor : courseInstructors) {
      courseInstructor.setId(null);
      courseInstructor.getInstructor().setId(null);
    }
  }

  private void setChallengeCreatorIdsToNull(List<LabStudentDto> courseLabs) {
    for (LabStudentDto lab : courseLabs) {
      if (lab.getCreator() != null) {
        lab.getCreator().setId(null);
      }
    }
  }
}
