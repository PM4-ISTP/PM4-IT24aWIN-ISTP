package com.pm4.istp.course.services;

import com.pm4.istp.course.db.CreateChallengeRequest;
import com.pm4.istp.course.db.UpdateChallengeRequest;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import com.pm4.istp.course.dto.ListChallengeResponseDto;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChallengeService {
  Challenge createChallenge(UUID userId, CreateChallengeRequest request);

  Challenge getChallenge(UUID userId, UUID challengeId);

  Challenge updateChallenge(UUID userId, UUID challengeId, UpdateChallengeRequest request);

  void deleteChallenge(UUID userId, UUID challengeId);

  Page<ListChallengeResponseDto> listChallengesForCreator(UUID creatorId, Pageable pageable);

  Page<ListChallengeResponseDto> searchAvailableChallenges(
      UUID userId, String search, Pageable pageable);

  int previewVisibilityImpact(UUID userId, UUID challengeId, ChallengeStatusEnum newStatus);
}
