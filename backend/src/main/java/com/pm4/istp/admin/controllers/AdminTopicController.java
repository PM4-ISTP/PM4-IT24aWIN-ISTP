package com.pm4.istp.admin.controllers;

import com.pm4.istp.admin.dto.AdminTopicRequest;
import com.pm4.istp.admin.services.AdminTopicService;
import com.pm4.istp.shared.dto.ErrorDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Topics", description = "Administrative endpoints for managing course topics")
@RestController
@RequestMapping("/api/admin/topics")
@RequiredArgsConstructor
@Validated
public class AdminTopicController {
  private final AdminTopicService adminTopicService;

  @Operation(
      summary = "List course topics",
      description = "Returns all course topics currently configured on the platform.")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Topics retrieved successfully")})
  @GetMapping
  public ResponseEntity<List<String>> listTopics() {
    return ResponseEntity.ok(adminTopicService.listTopics());
  }

  @Operation(
      summary = "Add a course topic",
      description = "Creates a new course topic that instructors can assign to their courses.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Topic added successfully"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid topic value",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PostMapping
  public ResponseEntity<Map<String, String>> addTopic(
      @Valid @RequestBody AdminTopicRequest request) {
    adminTopicService.addTopic(request.getValue());
    return ResponseEntity.ok(Map.of("message", "Topic added"));
  }

  @Operation(
      summary = "Delete a course topic",
      description = "Removes a course topic and clears it from any courses that referenced it.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Topic deleted successfully"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid topic value",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Topic not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @DeleteMapping("/{value}")
  public ResponseEntity<Map<String, String>> deleteTopic(
      @PathVariable
          @Size(min = 3, max = 24, message = "Topic must be between 3 and 24 characters")
          @Pattern(
              regexp = "^[A-Za-z][A-Za-z0-9-]*$",
              message = "Topic must be a single word (letters, numbers, '-')")
          String value) {
    adminTopicService.deleteTopic(value);
    return ResponseEntity.ok(Map.of("message", "Topic deleted"));
  }
}
