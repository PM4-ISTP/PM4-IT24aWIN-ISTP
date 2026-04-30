package com.pm4.istp.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForgotPasswordRequestDto {
  @NotBlank
  @Size(min = 3, max = 255)
  private String identifier;
}
