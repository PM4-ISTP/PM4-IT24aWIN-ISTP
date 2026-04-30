package com.pm4.istp.admin.services;

import com.pm4.istp.admin.dto.AdminActiveSessionDto;
import java.util.List;

public interface AdminSessionService {
  List<AdminActiveSessionDto> listActiveSessions();

  void logoutSession(String sessionId);
}

