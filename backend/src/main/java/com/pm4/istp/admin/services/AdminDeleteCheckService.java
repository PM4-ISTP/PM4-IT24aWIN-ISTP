package com.pm4.istp.admin.services;

import com.pm4.istp.admin.dto.DeleteCheckResponseDto;
import java.util.UUID;

public interface AdminDeleteCheckService {
  DeleteCheckResponseDto checkCourse(UUID courseId);

  DeleteCheckResponseDto checkLab(UUID labId);
}

