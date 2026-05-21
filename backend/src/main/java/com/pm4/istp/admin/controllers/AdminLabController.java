package com.pm4.istp.admin.controllers;

import com.pm4.istp.admin.dto.AdminLabListItemDto;
import com.pm4.istp.admin.dto.AdminUpdateLabRequestDto;
import com.pm4.istp.admin.services.AdminLabService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Labs", description = "Administrative endpoints for managing labs")
@RestController
@RequestMapping("/api/admin/labs")
@RequiredArgsConstructor
public class AdminLabController {
  private final AdminLabService adminChallengeService;

  @Operation(
      summary = "List labs",
      description =
          "Returns a paginated list of all labs on the platform, optionally filtered by a search"
              + " query.")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Labs retrieved successfully")})
  @GetMapping
  public ResponseEntity<Page<AdminLabListItemDto>> listChallenges(
      @RequestParam(name = "q", required = false) String query, Pageable pageable) {
    return ResponseEntity.ok(adminChallengeService.listChallenges(query, pageable));
  }

  @Operation(
      summary = "Update a lab",
      description =
          "Updates a lab's title, description, status and difficulty as an administrator.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Lab updated successfully"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Lab not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PutMapping("/{id}")
  public ResponseEntity<Void> updateChallenge(
      @PathVariable UUID id, @Valid @RequestBody AdminUpdateLabRequestDto request) {
    adminChallengeService.updateChallenge(id, request);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Delete a lab",
      description = "Soft-deletes a lab so it is no longer visible to students or instructors.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Lab deleted successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "Lab not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteChallenge(@PathVariable UUID id) {
    adminChallengeService.deleteChallenge(id);
    return ResponseEntity.noContent().build();
  }
}
