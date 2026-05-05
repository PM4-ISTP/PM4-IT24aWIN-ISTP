package com.pm4.istp.admin.services.impl;

import com.pm4.istp.admin.dto.AdminLabListItemDto;
import com.pm4.istp.admin.dto.AdminUpdateLabRequestDto;
import com.pm4.istp.admin.services.AdminLabService;
import com.pm4.istp.course.db.entities.Lab;
import com.pm4.istp.course.exceptions.LabNotFoundException;
import com.pm4.istp.course.repositories.LabRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminLabServiceImpl implements AdminLabService {
  private static final String CHALLENGE_NOT_FOUND_MSG = "Lab with ID '%s' not found";

  private final LabRepository labRepository;

  @Override
  @Transactional(readOnly = true)
  public Page<AdminLabListItemDto> listChallenges(String query, Pageable pageable) {
    String normalizedQuery = normalizeBlankToNull(query);

    if (normalizedQuery == null) {
      return labRepository.findAllChallengesForAdmin(pageable);
    }

    return labRepository.findAllChallengesForAdminByQuery(normalizedQuery, pageable);
  }

  @Override
  public void updateChallenge(UUID labId, AdminUpdateLabRequestDto request) {
    Lab lab =
        labRepository
            .findById(labId)
            .orElseThrow(
                () ->
                    new LabNotFoundException(
                        String.format(CHALLENGE_NOT_FOUND_MSG, labId)));

    lab.setTitle(request.getTitle());
    lab.setShortDescription(normalizeBlankToNull(request.getShortDescription()));
    lab.setDescription(request.getDescription());
    lab.setStatus(request.getStatus());
    lab.setDifficulty(request.getDifficulty());
    if (request.getMaxScore() != null) {
      lab.setMaxScore(request.getMaxScore());
    }

    labRepository.save(lab);
  }

  @Override
  public void deleteChallenge(UUID labId) {
    Lab lab =
        labRepository
            .findById(labId)
            .orElseThrow(
                () ->
                    new LabNotFoundException(
                        String.format(CHALLENGE_NOT_FOUND_MSG, labId)));
    labRepository.delete(lab);
  }

  private String normalizeBlankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
