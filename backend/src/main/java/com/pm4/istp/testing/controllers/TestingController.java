package com.pm4.istp.testing.controllers;

import java.time.Instant;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/testing")
@ConditionalOnProperty(
    name = "istp.features.staging-endpoint-enabled",
    havingValue = "true",
    matchIfMissing = false)
public class TestingController {

  @GetMapping("/ping")
  public Map<String, String> ping() {
    return Map.of("status", "ok", "timestamp", Instant.now().toString());
  }
}
