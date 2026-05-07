package com.pm4.istp.admin.services;

import com.pm4.istp.admin.dto.AdminLabListItemDto;
import com.pm4.istp.admin.dto.AdminUpdateLabRequestDto;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminLabService {
  Page<AdminLabListItemDto> listChallenges(String query, Pageable pageable);

  void updateChallenge(UUID labId, AdminUpdateLabRequestDto request);

  void deleteChallenge(UUID labId);
}
