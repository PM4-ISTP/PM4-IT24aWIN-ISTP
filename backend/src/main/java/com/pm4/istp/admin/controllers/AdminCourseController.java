package com.pm4.istp.admin.controllers;

import com.pm4.istp.admin.dto.AdminCourseListItemDto;
import com.pm4.istp.admin.dto.AdminUpdateCourseRequestDto;
import com.pm4.istp.admin.services.AdminCourseService;
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
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class AdminCourseController {
  private final AdminCourseService adminCourseService;

  @GetMapping
  public ResponseEntity<Page<AdminCourseListItemDto>> listCourses(
      @RequestParam(name = "q", required = false) String query, Pageable pageable) {
    return ResponseEntity.ok(adminCourseService.listCourses(query, pageable));
  }

  @PutMapping("/{id}")
  public ResponseEntity<Void> updateCourse(
      @PathVariable UUID id, @Valid @RequestBody AdminUpdateCourseRequestDto request) {
    adminCourseService.updateCourse(id, request);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCourse(@PathVariable UUID id) {
    adminCourseService.deleteCourse(id);
    return ResponseEntity.noContent().build();
  }
}
