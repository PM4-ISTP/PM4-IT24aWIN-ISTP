package com.pm4.istp.course.controllers;

import static com.pm4.istp.shared.util.JwtUtil.parseUserId;

import com.pm4.istp.course.db.CreateChallengeRequest;
import com.pm4.istp.course.db.UpdateChallengeRequest;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import com.pm4.istp.course.dto.ChallengeDetailResponseDto;
import com.pm4.istp.course.dto.ChallengeStudentDto;
import com.pm4.istp.course.dto.CreateChallengeRequestDto;
import com.pm4.istp.course.dto.CreateChallengeResponseDto;
import com.pm4.istp.course.dto.DockerImageCheckResponseDto;
import com.pm4.istp.course.dto.ListChallengeResponseDto;
import com.pm4.istp.course.dto.ChoiceSubmissionRequestDto;
import com.pm4.istp.course.dto.ChoiceSubmissionResponseDto;
import com.pm4.istp.course.dto.SubTaskSubmissionRequestDto;
import com.pm4.istp.course.dto.SubTaskSubmissionResponseDto;
import com.pm4.istp.course.dto.UpdateChallengeRequestDto;
import com.pm4.istp.course.dto.VisibilityImpactResponseDto;
import com.pm4.istp.course.mappers.ChallengeMapper;
import com.pm4.istp.course.services.ChallengeService;
import com.pm4.istp.course.services.DockerImageAvailabilityService;
import com.pm4.istp.shared.dto.ErrorDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

@Tag(name = "Challenge", description = "Challenge endpoints for the API")
@RestController
@RequestMapping("/api/v1/challenges")
@RequiredArgsConstructor
public class ChallengeController {
  private final ChallengeMapper challengeMapper;
  private final ChallengeService challengeService;
  private final DockerImageAvailabilityService dockerImageAvailabilityService;

  @Operation(
      summary = "Create a challenge",
      description = "Creates a new challenge and returns the persisted challenge.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Challenge created successfully",
            content =
                @Content(schema = @Schema(implementation = CreateChallengeResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PostMapping
  public ResponseEntity<CreateChallengeResponseDto> createChallenge(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody CreateChallengeRequestDto createChallengeRequestDto) {
    UUID userId = parseUserId(jwt);
    CreateChallengeRequest request = challengeMapper.fromDto(createChallengeRequestDto);
    Challenge created = challengeService.createChallenge(userId, request);
    CreateChallengeResponseDto responseDto = challengeMapper.toCreateResponseDto(created);
    return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
  }

  @Operation(
      summary = "Get a challenge by ID",
      description =
          "Returns the full details of a challenge. Visibility depends on status and user role.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Challenge found",
            content =
                @Content(schema = @Schema(implementation = ChallengeDetailResponseDto.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Challenge not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @GetMapping("/{id}")
  public ResponseEntity<ChallengeDetailResponseDto> getChallenge(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    UUID userId = parseUserId(jwt);
    Challenge challenge = challengeService.getChallenge(userId, id);
    ChallengeDetailResponseDto dto = challengeMapper.toDetailResponseDto(challenge);
    dto.setCourseCount(challenge.getCourseChallenges().size());
    return ResponseEntity.ok(dto);
  }

  @Operation(
      summary = "Update a challenge",
      description = "Updates an existing challenge. Only the creator can update.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Challenge updated successfully",
            content =
                @Content(schema = @Schema(implementation = ChallengeDetailResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Challenge not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PutMapping("/{id}")
  public ResponseEntity<ChallengeDetailResponseDto> updateChallenge(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateChallengeRequestDto updateChallengeRequestDto) {
    UUID userId = parseUserId(jwt);
    UpdateChallengeRequest request = challengeMapper.fromDto(updateChallengeRequestDto);
    Challenge updated = challengeService.updateChallenge(userId, id, request);
    ChallengeDetailResponseDto dto = challengeMapper.toDetailResponseDto(updated);
    dto.setCourseCount(updated.getCourseChallenges().size());
    return ResponseEntity.ok(dto);
  }

  @Operation(
      summary = "Delete a challenge",
      description =
          "Deletes a challenge and removes it from all courses. Only the creator can delete.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Challenge deleted successfully"),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Challenge not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteChallenge(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    UUID userId = parseUserId(jwt);
    challengeService.deleteChallenge(userId, id);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "List my challenges",
      description = "Returns a paginated list of challenges created by the authenticated user.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Challenges retrieved successfully")
      })
  @GetMapping
  public ResponseEntity<Page<ListChallengeResponseDto>> listChallenges(
      @AuthenticationPrincipal Jwt jwt, Pageable pageable) {
    UUID userId = parseUserId(jwt);
    Page<ListChallengeResponseDto> challenges =
        challengeService.listChallengesForCreator(userId, pageable);
    return ResponseEntity.ok(challenges);
  }

  @Operation(
      summary = "Check Docker image availability",
      description = "Checks whether a supported Docker image reference is reachable.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Docker image is reachable"),
        @ApiResponse(
            responseCode = "400",
            description = "Docker image is invalid or unreachable",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @GetMapping("/docker-image")
  public ResponseEntity<DockerImageCheckResponseDto> checkDockerImage(
      @RequestParam("image") String image) {
    dockerImageAvailabilityService.assertImageExists(image);
    return ResponseEntity.ok(new DockerImageCheckResponseDto(true, "Image found"));
  }

  @Operation(
      summary = "Search available challenges",
      description =
          "Searches for challenges by title. Returns the user's own challenges and public ones.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
      })
  @GetMapping("/search")
  public ResponseEntity<Page<ListChallengeResponseDto>> searchChallenges(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(name = "q", defaultValue = "") String query,
      Pageable pageable) {
    UUID userId = parseUserId(jwt);
    Page<ListChallengeResponseDto> results =
        challengeService.searchAvailableChallenges(userId, query, pageable);
    return ResponseEntity.ok(results);
  }

  @Operation(
      summary = "Preview visibility change impact",
      description =
          "Returns how many course assignments would be removed if the challenge's visibility were"
              + " changed to the given status. Only the creator can call this.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Impact retrieved successfully",
            content =
                @Content(schema = @Schema(implementation = VisibilityImpactResponseDto.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Challenge not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @GetMapping("/{id}/visibility-impact")
  public ResponseEntity<VisibilityImpactResponseDto> getVisibilityImpact(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestParam("status") ChallengeStatusEnum status) {
    UUID userId = parseUserId(jwt);
    int affectedCourseCount = challengeService.previewVisibilityImpact(userId, id, status);
    return ResponseEntity.ok(new VisibilityImpactResponseDto(affectedCourseCount));
  }

  @Operation(
      summary = "Get a challenge in student play view",
      description =
          "Returns a challenge formatted for students (no sub-task flags, with per-user solved"
              + " progress). Caller must be enrolled in the given course, and the challenge must"
              + " belong to it.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Challenge retrieved successfully",
            content = @Content(schema = @Schema(implementation = ChallengeStudentDto.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Challenge not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @GetMapping("/{id}/play")
  public ResponseEntity<ChallengeStudentDto> getChallengeForPlay(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestParam("courseId") UUID courseId) {
    UUID userId = parseUserId(jwt);
    ChallengeStudentDto dto = challengeService.getChallengeForPlay(userId, courseId, id);
    return ResponseEntity.ok(dto);
  }

  @Operation(
      summary = "Submit a flag for a sub-task",
      description =
          "Validates the submitted flag against the stored plaintext flag (case-sensitive). On a"
              + " correct submission the sub-task is marked as solved for the authenticated user."
              + " Already-solved sub-tasks cannot be re-submitted.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Submission processed",
            content =
                @Content(schema = @Schema(implementation = SubTaskSubmissionResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid flag format",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "403",
            description = "User not enrolled in a course containing this challenge",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Challenge or sub-task not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "409",
            description = "Sub-task already solved",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PostMapping("/{challengeId}/subtasks/{subTaskId}/submit")
  public ResponseEntity<SubTaskSubmissionResponseDto> submitSubTaskFlag(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID challengeId,
      @PathVariable UUID subTaskId,
      @Valid @RequestBody SubTaskSubmissionRequestDto request) {
    UUID userId = parseUserId(jwt);
    SubTaskSubmissionResponseDto response =
        challengeService.submitSubTaskFlag(userId, challengeId, subTaskId, request.getFlag());
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Submit a multiple-choice answer for a sub-task",
      description =
          "Records the student's selected option for a MULTIPLE_CHOICE sub-task. Points are awarded"
              + " automatically when the correct option is chosen. Re-submission returns the"
              + " existing result without changing it.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Submission processed",
            content =
                @Content(schema = @Schema(implementation = ChoiceSubmissionResponseDto.class))),
        @ApiResponse(
            responseCode = "403",
            description = "User not enrolled",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Challenge, sub-task or option not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PostMapping("/{challengeId}/subtasks/{subTaskId}/submit-choice")
  public ResponseEntity<ChoiceSubmissionResponseDto> submitSubTaskChoice(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID challengeId,
      @PathVariable UUID subTaskId,
      @Valid @RequestBody ChoiceSubmissionRequestDto request) {
    UUID userId = parseUserId(jwt);
    ChoiceSubmissionResponseDto response =
        challengeService.submitSubTaskChoice(
            userId, challengeId, subTaskId, request.getSelectedOptionId());
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Count completed labs for current user",
      description =
          "Returns the number of challenges where the authenticated user has solved all sub-tasks.")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Count returned successfully")})
  @GetMapping("/my-completed-count")
  public ResponseEntity<java.util.Map<String, Long>> countMyCompletedChallenges(
      @AuthenticationPrincipal Jwt jwt) {
    UUID userId = parseUserId(jwt);
    long count = challengeService.countCompletedChallenges(userId);
    return ResponseEntity.ok(java.util.Map.of("count", count));
  }
}
