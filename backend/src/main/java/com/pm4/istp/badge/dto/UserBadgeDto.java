package com.pm4.istp.badge.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserBadgeDto(
    UUID badgeId,
    UUID courseId,
    String courseTitle,
    String primaryColor,
    String textColor,
    int template,
    String badgeIcon,
    LocalDateTime earnedAt) {}
