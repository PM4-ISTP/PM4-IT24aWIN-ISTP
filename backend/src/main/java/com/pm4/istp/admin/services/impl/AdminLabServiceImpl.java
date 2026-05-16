package com.pm4.istp.admin.services.impl;

import com.pm4.istp.admin.dto.AdminLabListItemDto;
import com.pm4.istp.admin.dto.AdminUpdateLabRequestDto;
import com.pm4.istp.admin.services.AdminLabService;
import com.pm4.istp.course.db.entities.Lab;
import com.pm4.istp.course.exceptions.LabNotFoundException;
import com.pm4.istp.course.repositories.CourseLabRepository;
import com.pm4.istp.course.repositories.LabRepository;
import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.repositories.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminLabServiceImpl implements AdminLabService {
  private static final String LAB_NOT_FOUND_MSG = "Lab with ID '%s' not found";

  private final LabRepository labRepository;
  private final CourseLabRepository courseLabRepository;
  private final UserRepository userRepository;

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
            .orElseThrow(() -> new LabNotFoundException(String.format(LAB_NOT_FOUND_MSG, labId)));

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
            .orElseThrow(() -> new LabNotFoundException(String.format(LAB_NOT_FOUND_MSG, labId)));

    if (lab.getDeletedAt() == null) {
      courseLabRepository.deleteByChallengeId(labId);
      UUID actorId = resolveActorIdFromSecurityContext();
      String deletedByUsername =
          actorId == null
              ? "unknown"
              : userRepository
                  .findByIdAndDeletedAtIsNull(actorId)
                  .map(User::getUsername)
                  .orElse("unknown");
      lab.setDeletedByUsername(deletedByUsername);
      lab.setDeletedAt(LocalDateTime.now());
      labRepository.save(lab);
    }
  }

  private String normalizeBlankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private UUID resolveActorIdFromSecurityContext() {
    try {
      var auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth == null) {
        return null;
      }
      Object principal = auth.getPrincipal();
      if (principal instanceof Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
      }
      return null;
    } catch (Exception ignored) {
      return null;
    }
  }
}
