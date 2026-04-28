package com.pm4.istp.admin.services;

import com.pm4.istp.admin.dto.AdminChallengeListItemDto;
import com.pm4.istp.admin.dto.AdminUpdateChallengeRequestDto;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminChallengeService {
  Page<AdminChallengeListItemDto> listChallenges(String query, Pageable pageable);

  void updateChallenge(UUID challengeId, AdminUpdateChallengeRequestDto request);

  void deleteChallenge(UUID challengeId);
}
