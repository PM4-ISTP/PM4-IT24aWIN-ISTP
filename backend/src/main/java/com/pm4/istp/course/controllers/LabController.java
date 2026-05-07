package com.pm4.istp.course.controllers;

import static com.pm4.istp.shared.util.JwtUtil.parseUserId;

import com.pm4.istp.course.db.CreateLabRequest;
import com.pm4.istp.course.db.UpdateLabRequest;
import com.pm4.istp.course.db.entities.Lab;
import com.pm4.istp.course.db.entities.LabStatusEnum;
import com.pm4.istp.course.dto.ChallengeDetailResponseDto;
import com.pm4.istp.course.dto.ChallengeSubmissionRequestDto;
import com.pm4.istp.course.dto.ChallengeSubmissionResponseDto;
import com.pm4.istp.course.dto.ChoiceSubmissionRequestDto;
import com.pm4.istp.course.dto.ChoiceSubmissionResponseDto;
import com.pm4.istp.course.dto.CreateChallengeRequestDto;
import com.pm4.istp.course.dto.CreateChallengeResponseDto;
import com.pm4.istp.course.dto.DockerImageCheckResponseDto;
import com.pm4.istp.course.dto.LabStudentDto;
import com.pm4.istp.course.dto.ListLabResponseDto;
import com.pm4.istp.course.dto.UpdateChallengeRequestDto;
import com.pm4.istp.course.dto.VisibilityImpactResponseDto;
import com.pm4.istp.course.mappers.LabMapper;
import com.pm4.istp.course.services.DockerImageAvailabilityService;
import com.pm4.istp.course.services.DockerImageAvailabilityService.DockerImageAvailabilityResult;
import com.pm4.istp.course.services.LabService;
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

@Tag(name = "Lab", description = "Lab endpoints for the API")
@RestController
@RequestMapping("/api/v1/labs")
@RequiredArgsConstructor
public class LabController {
  private final LabMapper labMapper;
  private final LabService labService;
  private final DockerImageAvailabilityService dockerImageAvailabilityService;

  @Operation(
      summary = "Create a lab",
      description = "Creates a new lab and returns the persisted lab.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Lab created successfully",
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
    CreateLabRequest request = labMapper.fromDto(createChallengeRequestDto);
    Lab created = labService.createChallenge(userId, request);
    CreateChallengeResponseDto responseDto = labMapper.toCreateResponseDto(created);
    return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
  }

  @Operation(
      summary = "Get a lab by ID",
      description =
          "Returns the full details of a lab. Visibility depends on status and user role.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lab found",
            content =
                @Content(schema = @Schema(implementation = ChallengeDetailResponseDto.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Lab not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @GetMapping("/{id}")
  public ResponseEntity<ChallengeDetailResponseDto> getChallenge(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    UUID userId = parseUserId(jwt);
    Lab lab = labService.getChallenge(userId, id);
    ChallengeDetailResponseDto dto = labMapper.toDetailResponseDto(lab);
    dto.setCourseCount(lab.getCourseLabs().size());
    return ResponseEntity.ok(dto);
  }

  @Operation(
      summary = "Update a lab",
      description = "Updates an existing lab. Only the creator can update.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lab updated successfully",
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
            description = "Lab not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PutMapping("/{id}")
  public ResponseEntity<ChallengeDetailResponseDto> updateChallenge(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateChallengeRequestDto updateChallengeRequestDto) {
    UUID userId = parseUserId(jwt);
    UpdateLabRequest request = labMapper.fromDto(updateChallengeRequestDto);
    Lab updated = labService.updateChallenge(userId, id, request);
    ChallengeDetailResponseDto dto = labMapper.toDetailResponseDto(updated);
    dto.setCourseCount(updated.getCourseLabs().size());
    return ResponseEntity.ok(dto);
  }

  @Operation(
      summary = "Delete a lab",
      description = "Deletes a lab and removes it from all courses. Only the creator can delete.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Lab deleted successfully"),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Lab not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteChallenge(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    UUID userId = parseUserId(jwt);
    labService.deleteChallenge(userId, id);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "List my labs",
      description = "Returns a paginated list of labs created by the authenticated user.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Challenges retrieved successfully")
      })
  @GetMapping
  public ResponseEntity<Page<ListLabResponseDto>> listChallenges(
      @AuthenticationPrincipal Jwt jwt, Pageable pageable) {
    UUID userId = parseUserId(jwt);
    Page<ListLabResponseDto> labs = labService.listChallengesForCreator(userId, pageable);
    return ResponseEntity.ok(labs);
  }

  @Operation(
      summary = "Check Docker image availability",
      description = "Checks whether a public GHCR image reference is reachable.")
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
    DockerImageAvailabilityResult result =
        dockerImageAvailabilityService.checkImageAvailability(image);
    String message =
        result.privateImage()
            ? "Private GHCR image accepted; Kubernetes will use the configured image pull secret"
            : "Public GHCR image found";
    return ResponseEntity.ok(new DockerImageCheckResponseDto(true, message));
  }

  @Operation(
      summary = "Search available labs",
      description = "Searches for labs by title. Returns the user's own labs and public ones.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
      })
  @GetMapping("/search")
  public ResponseEntity<Page<ListLabResponseDto>> searchChallenges(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(name = "q", defaultValue = "") String query,
      Pageable pageable) {
    UUID userId = parseUserId(jwt);
    Page<ListLabResponseDto> results =
        labService.searchAvailableChallenges(userId, query, pageable);
    return ResponseEntity.ok(results);
  }

  @Operation(
      summary = "Preview visibility change impact",
      description =
          "Returns how many course assignments would be removed if the lab's visibility were"
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
            description = "Lab not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @GetMapping("/{id}/visibility-impact")
  public ResponseEntity<VisibilityImpactResponseDto> getVisibilityImpact(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestParam("status") LabStatusEnum status) {
    UUID userId = parseUserId(jwt);
    int affectedCourseCount = labService.previewVisibilityImpact(userId, id, status);
    return ResponseEntity.ok(new VisibilityImpactResponseDto(affectedCourseCount));
  }

  @Operation(
      summary = "Get a lab in student play view",
      description =
          "Returns a lab formatted for students (no challenge flags, with per-user solved"
              + " progress). Caller must be enrolled in the given course, and the lab must"
              + " belong to it.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lab retrieved successfully",
            content = @Content(schema = @Schema(implementation = LabStudentDto.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Lab not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @GetMapping("/{id}/play")
  public ResponseEntity<LabStudentDto> getChallengeForPlay(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestParam("courseId") UUID courseId) {
    UUID userId = parseUserId(jwt);
    LabStudentDto dto = labService.getChallengeForPlay(userId, courseId, id);
    return ResponseEntity.ok(dto);
  }

  @Operation(
      summary = "Submit a flag for a challenge",
      description =
          "Validates the submitted flag against the stored plaintext flag (case-sensitive). On a"
              + " correct submission the challenge is marked as solved for the authenticated user."
              + " Already-solved challenges cannot be re-submitted.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Submission processed",
            content =
                @Content(schema = @Schema(implementation = ChallengeSubmissionResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid flag format",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "403",
            description = "User not enrolled in a course containing this lab",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Lab or challenge not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "409",
            description = "Sub-task already solved",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PostMapping("/{labId}/challenges/{challengeId}/submit")
  public ResponseEntity<ChallengeSubmissionResponseDto> submitChallengeFlag(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID labId,
      @PathVariable UUID challengeId,
      @Valid @RequestBody ChallengeSubmissionRequestDto request) {
    UUID userId = parseUserId(jwt);
    ChallengeSubmissionResponseDto response =
        labService.submitChallengeFlag(
            userId, request.getCourseId(), labId, challengeId, request.getFlag());
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Submit a multiple-choice answer for a challenge",
      description =
          "Records the student's selected option for a MULTIPLE_CHOICE challenge. Points are awarded"
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
            description = "Lab, challenge or option not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PostMapping("/{labId}/challenges/{challengeId}/submit-choice")
  public ResponseEntity<ChoiceSubmissionResponseDto> submitChallengeChoice(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID labId,
      @PathVariable UUID challengeId,
      @Valid @RequestBody ChoiceSubmissionRequestDto request) {
    UUID userId = parseUserId(jwt);
    ChoiceSubmissionResponseDto response =
        labService.submitChallengeChoice(
            userId, request.getCourseId(), labId, challengeId, request.getSelectedOptionId());
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Complete a theory challenge",
      description =
          "Marks a FLAG challenge with no flag as completed (theory/reading task). "
              + "Fails if the challenge has a flag set.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Theory challenge marked as completed"),
        @ApiResponse(
            responseCode = "400",
            description = "Sub-task has a flag and cannot be auto-completed",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "403",
            description = "User not enrolled",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Lab or challenge not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PostMapping("/{labId}/challenges/{challengeId}/complete")
  public ResponseEntity<ChallengeSubmissionResponseDto> completeTheoryChallenge(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID labId,
      @PathVariable UUID challengeId,
      @RequestParam("courseId") UUID courseId) {
    UUID userId = parseUserId(jwt);
    ChallengeSubmissionResponseDto response =
        labService.completeTheoryChallenge(userId, courseId, labId, challengeId);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Count completed labs for current user",
      description =
          "Returns the number of labs where the authenticated user has solved all challenges.")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Count returned successfully")})
  @GetMapping("/my-completed-count")
  public ResponseEntity<java.util.Map<String, Long>> countMyCompletedChallenges(
      @AuthenticationPrincipal Jwt jwt) {
    UUID userId = parseUserId(jwt);
    long count = labService.countCompletedChallenges(userId);
    return ResponseEntity.ok(java.util.Map.of("count", count));
  }
}
