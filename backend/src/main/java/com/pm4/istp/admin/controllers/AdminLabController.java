package com.pm4.istp.admin.controllers;

import com.pm4.istp.admin.dto.AdminLabListItemDto;
import com.pm4.istp.admin.dto.AdminUpdateLabRequestDto;
import com.pm4.istp.admin.services.AdminLabService;
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

@RestController
@RequestMapping("/api/admin/labs")
@RequiredArgsConstructor
public class AdminLabController {
  private final AdminLabService adminChallengeService;

  @GetMapping
  public ResponseEntity<Page<AdminLabListItemDto>> listChallenges(
      @RequestParam(name = "q", required = false) String query, Pageable pageable) {
    return ResponseEntity.ok(adminChallengeService.listChallenges(query, pageable));
  }

  @GetMapping("/removed")
  public ResponseEntity<Page<AdminLabListItemDto>> listRemovedChallenges(
      @RequestParam(name = "q", required = false) String query, Pageable pageable) {
    return ResponseEntity.ok(adminChallengeService.listRemovedChallenges(query, pageable));
  }

  @PutMapping("/{id}")
  public ResponseEntity<Void> updateChallenge(
      @PathVariable UUID id, @Valid @RequestBody AdminUpdateLabRequestDto request) {
    adminChallengeService.updateChallenge(id, request);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteChallenge(@PathVariable UUID id) {
    adminChallengeService.deleteChallenge(id);
    return ResponseEntity.noContent().build();
  }
}
