package com.pm4.istp.badge.services;

import com.pm4.istp.badge.dto.CourseBadgeConfigDto;
import com.pm4.istp.badge.dto.UpdateCourseBadgeRequestDto;
import com.pm4.istp.badge.dto.UserBadgeDto;
import java.util.List;
import java.util.UUID;

public interface BadgeService {

  CourseBadgeConfigDto getCourseBadgeConfig(UUID courseId);

  CourseBadgeConfigDto updateCourseBadgeConfig(
      UUID userId, UUID courseId, UpdateCourseBadgeRequestDto request);

  List<UserBadgeDto> getUserBadges(UUID userId);

  void tryAwardBadgesForChallenge(UUID userId, UUID labId);

  /**
   * Attempts to award the course badge if badges are enabled and the course is already completed.
   */
  void tryAwardBadgeForCourse(UUID userId, UUID courseId);
}
