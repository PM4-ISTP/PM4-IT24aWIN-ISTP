package com.pm4.istp.admin.services;

import com.pm4.istp.admin.dto.AdminCourseListItemDto;
import com.pm4.istp.admin.dto.AdminUpdateCourseRequestDto;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminCourseService {
  Page<AdminCourseListItemDto> listCourses(String query, Pageable pageable);

  void updateCourse(UUID courseId, AdminUpdateCourseRequestDto request);

  void deleteCourse(UUID courseId);
}

