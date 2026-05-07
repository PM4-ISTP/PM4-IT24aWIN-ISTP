package com.pm4.istp.badge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCourseBadgeRequestDto(
    @NotNull @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String primaryColor,
    @NotNull @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String textColor,
    @NotNull @Min(1) @Max(3) Integer template,
    @Size(max = 16) String badgeIcon,
    boolean badgeEnabled) {}
