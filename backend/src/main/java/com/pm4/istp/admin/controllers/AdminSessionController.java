package com.pm4.istp.admin.controllers;

import com.pm4.istp.admin.dto.AdminActiveSessionDto;
import com.pm4.istp.admin.services.AdminSessionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/admin/sessions")
@RequiredArgsConstructor
public class AdminSessionController {
  private final AdminSessionService adminSessionService;

  @GetMapping
  public ResponseEntity<List<AdminActiveSessionDto>> listActiveSessions() {
    return ResponseEntity.ok(adminSessionService.listActiveSessions());
  }

  @DeleteMapping("/{sessionId}")
  public ResponseEntity<Void> logoutSession(@PathVariable String sessionId) {
    adminSessionService.logoutSession(sessionId);
    return ResponseEntity.noContent().build();
  }
}

