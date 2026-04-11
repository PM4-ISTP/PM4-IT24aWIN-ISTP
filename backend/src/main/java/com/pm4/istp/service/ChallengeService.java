package com.pm4.istp.service;

import com.pm4.istp.domain.CreateChallengeRequest;
import com.pm4.istp.domain.UpdateChallengeRequest;
import com.pm4.istp.domain.entites.Challenge;
import com.pm4.istp.dto.ListChallengeResponseDto;
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
}
