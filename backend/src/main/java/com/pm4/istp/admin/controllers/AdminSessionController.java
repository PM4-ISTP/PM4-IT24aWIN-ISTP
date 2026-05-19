package com.pm4.istp.admin.controllers;

import com.pm4.istp.admin.dto.AdminActiveSessionDto;
import com.pm4.istp.admin.services.AdminSessionService;
import com.pm4.istp.shared.dto.ErrorDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
    name = "Admin Sessions",
    description = "Administrative endpoints for inspecting and terminating active sessions")
@RestController
@RequestMapping(path = "/api/admin/sessions")
@RequiredArgsConstructor
public class AdminSessionController {
  private final AdminSessionService adminSessionService;

  @Operation(
      summary = "List active sessions",
      description = "Returns all active Keycloak sessions for the configured application client.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Active sessions retrieved successfully")
      })
  @GetMapping
  public ResponseEntity<List<AdminActiveSessionDto>> listActiveSessions() {
    return ResponseEntity.ok(adminSessionService.listActiveSessions());
  }

  @Operation(
      summary = "Terminate a session",
      description = "Logs out a single active session by its session ID.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Session terminated successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "Session not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @DeleteMapping("/{sessionId}")
  public ResponseEntity<Void> logoutSession(@PathVariable String sessionId) {
    adminSessionService.logoutSession(sessionId);
    return ResponseEntity.noContent().build();
  }
}
