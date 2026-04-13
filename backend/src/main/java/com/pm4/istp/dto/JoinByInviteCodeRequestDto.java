package com.pm4.istp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JoinByInviteCodeRequestDto {
  @NotBlank(message = "Invite code is required")
  @Size(min = 6, max = 6, message = "Invite code must be exactly 6 characters")
  @Pattern(regexp = "[A-Z0-9]{6}", message = "Invite code must be 6 uppercase alphanumeric characters")
  private String code;
}
