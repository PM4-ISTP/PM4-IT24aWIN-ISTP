package com.pm4.istp.testing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DatabaseCredentialsDto {
  @NotNull(message = "Username is required")
  @Valid
  private String username;

  @NotNull(message = "Password is required")
  @Valid
  private String password;
}
