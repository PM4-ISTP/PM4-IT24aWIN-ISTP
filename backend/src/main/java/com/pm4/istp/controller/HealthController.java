package com.pm4.istp.controller;

import com.pm4.istp.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "Health check endpoints for the API")
@RestController
public class HealthController {

  @Operation(
      summary = "Check API status",
      description = "Returns a simple status response to verify that the API is running.")
  @ApiResponse(responseCode = "200", description = "API is running successfully")
  @GetMapping("/health")
  public HealthResponse health() {
    return new HealthResponse("ok");
  }
}
