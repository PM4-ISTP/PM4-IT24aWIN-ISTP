package com.pm4.istp.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminSetUserPasswordRequestDto {
  @NotBlank
  @Size(min = 8, max = 255)
  private String password;

  private boolean temporary = true;
}
