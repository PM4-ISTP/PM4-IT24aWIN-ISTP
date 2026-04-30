package com.pm4.istp.admin.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminCreateUserResponseDto {
  private UUID userId;
  private String temporaryPassword;
}

