package com.pm4.istp.challengepod.dto;

import java.util.UUID;

public record RunningPodResponse(
    UUID labId, String labTitle, UUID courseId, String courseTitle, PodStatusResponse pod) {}
