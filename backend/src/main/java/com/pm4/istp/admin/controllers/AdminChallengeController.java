package com.pm4.istp.admin.controllers;

import com.pm4.istp.admin.dto.AdminChallengeListItemDto;
import com.pm4.istp.admin.dto.AdminUpdateChallengeRequestDto;
import com.pm4.istp.admin.services.AdminCourseChallengeService;
import com.pm4.istp.course.db.entities.ChallengeDifficultyEnum;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
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
@RequestMapping("/api/admin/challenges")
@RequiredArgsConstructor
public class AdminChallengeController {
  private final AdminCourseChallengeService adminCourseChallengeService;

  @GetMapping
  public ResponseEntity<Page<AdminChallengeListItemDto>> listChallenges(
      @RequestParam(name = "q", required = false) String query,
      @RequestParam(name = "owner", required = false) String owner,
      @RequestParam(name = "status", required = false) ChallengeStatusEnum status,
      @RequestParam(name = "difficulty", required = false) ChallengeDifficultyEnum difficulty,
      Pageable pageable) {
    return ResponseEntity.ok(
        adminCourseChallengeService.listChallenges(query, owner, status, difficulty, pageable));
  }

  @PutMapping("/{id}")
  public ResponseEntity<Void> updateChallenge(
      @PathVariable UUID id, @Valid @RequestBody AdminUpdateChallengeRequestDto request) {
    adminCourseChallengeService.updateChallenge(id, request);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteChallenge(@PathVariable UUID id) {
    adminCourseChallengeService.deleteChallenge(id);
    return ResponseEntity.noContent().build();
  }
}
