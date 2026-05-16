package com.pm4.istp.admin.controllers;

import com.pm4.istp.admin.dto.DeleteCheckResponseDto;
import com.pm4.istp.admin.services.AdminDeleteCheckService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/delete-check")
@RequiredArgsConstructor
public class AdminDeleteCheckController {
  private final AdminDeleteCheckService adminDeleteCheckService;

  @GetMapping("/course/{id}")
  public ResponseEntity<DeleteCheckResponseDto> checkCourse(@PathVariable UUID id) {
    return ResponseEntity.ok(adminDeleteCheckService.checkCourse(id));
  }

  @GetMapping("/lab/{id}")
  public ResponseEntity<DeleteCheckResponseDto> checkLab(@PathVariable UUID id) {
    return ResponseEntity.ok(adminDeleteCheckService.checkLab(id));
  }
}
