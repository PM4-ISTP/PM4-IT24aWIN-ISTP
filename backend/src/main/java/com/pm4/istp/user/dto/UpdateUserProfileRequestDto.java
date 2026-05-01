package com.pm4.istp.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserProfileRequestDto {
  @NotBlank
  @Size(max = 255)
  private String firstName;

  @NotBlank
  @Size(max = 255)
  private String lastName;

  @Size(max = 255)
  private String title;

  @Size(max = 2048)
  @Pattern(
      regexp = "^(https?://).+$",
      message = "must start with http:// or https://",
      flags = Pattern.Flag.CASE_INSENSITIVE)
  private String pictureUrl;
}
