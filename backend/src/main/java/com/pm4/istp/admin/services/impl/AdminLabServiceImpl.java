package com.pm4.istp.admin.services.impl;

import com.pm4.istp.admin.dto.AdminLabListItemDto;
import com.pm4.istp.admin.dto.AdminUpdateLabRequestDto;
import com.pm4.istp.admin.services.AdminLabService;
import com.pm4.istp.course.db.entities.Lab;
import com.pm4.istp.course.db.entities.LabStatusEnum;
import com.pm4.istp.course.exceptions.LabNotFoundException;
import com.pm4.istp.course.repositories.ChallengeCompletionRepository;
import com.pm4.istp.course.repositories.CourseChallengeScoreOverrideRepository;
import com.pm4.istp.course.repositories.CourseLabRepository;
import com.pm4.istp.course.repositories.LabRepository;
import com.pm4.istp.course.repositories.StudentFlagSubmissionRepository;
import com.pm4.istp.course.repositories.StudentOptionSubmissionRepository;
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
  private static final String LAB_NOT_FOUND_MSG = "Lab with ID '%s' not found";

  private final LabRepository labRepository;
  private final CourseLabRepository courseLabRepository;
  private final ChallengeCompletionRepository challengeCompletionRepository;
  private final StudentFlagSubmissionRepository studentFlagSubmissionRepository;
  private final StudentOptionSubmissionRepository studentOptionSubmissionRepository;
  private final CourseChallengeScoreOverrideRepository courseChallengeScoreOverrideRepository;

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
                () -> new LabNotFoundException(String.format(LAB_NOT_FOUND_MSG, labId)));

    lab.setTitle(request.getTitle());
    lab.setDescription(request.getDescription());
    lab.setStatus(request.getStatus());
    lab.setDifficulty(request.getDifficulty());

    labRepository.save(lab);
  }

  @Override
  public void deleteChallenge(UUID labId) {
    Lab lab =
        labRepository
            .findById(labId)
            .orElseThrow(
                () -> new LabNotFoundException(String.format(LAB_NOT_FOUND_MSG, labId)));
    deleteOrArchive(lab);
  }

  private void deleteOrArchive(Lab lab) {
    if (hasRetainedHistory(lab.getId())) {
      archive(lab);
      return;
    }

    labRepository.delete(lab);
    labRepository.flush();
  }

  private boolean hasRetainedHistory(UUID labId) {
    return courseLabRepository.countByChallengeId(labId) > 0
        || challengeCompletionRepository.existsByLabId(labId)
        || studentFlagSubmissionRepository.existsByLabId(labId)
        || studentOptionSubmissionRepository.existsByLabId(labId)
        || courseChallengeScoreOverrideRepository.existsByLabId(labId);
  }

  private void archive(Lab lab) {
    lab.setStatus(LabStatusEnum.ARCHIVED);
    labRepository.save(lab);
  }

  private String normalizeBlankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
