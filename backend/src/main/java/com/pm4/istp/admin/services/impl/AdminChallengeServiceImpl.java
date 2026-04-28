package com.pm4.istp.admin.services.impl;

import com.pm4.istp.admin.dto.AdminChallengeListItemDto;
import com.pm4.istp.admin.dto.AdminUpdateChallengeRequestDto;
import com.pm4.istp.admin.services.AdminChallengeService;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.exceptions.ChallengeNotFoundException;
import com.pm4.istp.course.repositories.ChallengeRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminChallengeServiceImpl implements AdminChallengeService {
  private static final String CHALLENGE_NOT_FOUND_MSG = "Challenge with ID '%s' not found";

  private final ChallengeRepository challengeRepository;

  @Override
  @Transactional(readOnly = true)
  public Page<AdminChallengeListItemDto> listChallenges(String query, Pageable pageable) {
    String normalizedQuery = normalizeQuery(query);

    if (normalizedQuery == null) {
      return challengeRepository.findAllChallengesForAdmin(pageable);
    }

    return challengeRepository.findAllChallengesForAdminByQuery(normalizedQuery, pageable);
  }

  @Override
  public void updateChallenge(UUID challengeId, AdminUpdateChallengeRequestDto request) {
    Challenge challenge =
        challengeRepository
            .findById(challengeId)
            .orElseThrow(
                () ->
                    new ChallengeNotFoundException(
                        String.format(CHALLENGE_NOT_FOUND_MSG, challengeId)));

    challenge.setTitle(request.getTitle());
    challenge.setShortDescription(normalizeBlankToNull(request.getShortDescription()));
    challenge.setDescription(request.getDescription());
    challenge.setStatus(request.getStatus());
    challenge.setDifficulty(request.getDifficulty());
    if (request.getMaxScore() != null) {
      challenge.setMaxScore(request.getMaxScore());
    }

    challengeRepository.save(challenge);
  }

  @Override
  public void deleteChallenge(UUID challengeId) {
    Challenge challenge =
        challengeRepository
            .findById(challengeId)
            .orElseThrow(
                () ->
                    new ChallengeNotFoundException(
                        String.format(CHALLENGE_NOT_FOUND_MSG, challengeId)));
    challengeRepository.delete(challenge);
  }

  private String normalizeQuery(String value) {
    return normalizeBlankToNull(value);
  }

  private String normalizeBlankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}

