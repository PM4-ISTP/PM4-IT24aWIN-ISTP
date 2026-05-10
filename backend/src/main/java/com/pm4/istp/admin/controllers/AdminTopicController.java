package com.pm4.istp.admin.controllers;

import com.pm4.istp.admin.dto.AdminTopicRequest;
import com.pm4.istp.admin.services.AdminTopicService;
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

@RestController
@RequestMapping("/api/admin/topics")
@RequiredArgsConstructor
@Validated
public class AdminTopicController {
  private final AdminTopicService adminTopicService;

  @GetMapping
  public ResponseEntity<List<String>> listTopics() {
    return ResponseEntity.ok(adminTopicService.listTopics());
  }

  @PostMapping
  public ResponseEntity<Map<String, String>> addTopic(
      @Valid @RequestBody AdminTopicRequest request) {
    adminTopicService.addTopic(request.getValue());
    return ResponseEntity.ok(Map.of("message", "Topic added"));
  }

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
