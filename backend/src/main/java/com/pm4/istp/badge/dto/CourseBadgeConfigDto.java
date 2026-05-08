package com.pm4.istp.badge.dto;

import java.util.UUID;

public record CourseBadgeConfigDto(
    UUID courseId,
    String courseTitle,
    String primaryColor,
    String textColor,
    int template,
    String badgeIcon,
    boolean badgeEnabled) {}
