package com.pm4.istp.admin.dto;

import java.util.List;

public record DeleteCheckResponseDto(boolean hardDeleteAllowed, List<DeleteCheckBlockerDto> blockers) {}

