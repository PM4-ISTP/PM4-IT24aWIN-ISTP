package com.pm4.istp.shared.controllers;

import java.time.Instant;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staging")
@ConditionalOnProperty(
    name = "istp.features.staging-endpoint-enabled",
    havingValue = "true",
    matchIfMissing = false)
public class StagingOnlyController {

  @GetMapping("/ping")
  public Map<String, String> ping() {
    return Map.of("status", "ok", "timestamp", Instant.now().toString());
  }
}

