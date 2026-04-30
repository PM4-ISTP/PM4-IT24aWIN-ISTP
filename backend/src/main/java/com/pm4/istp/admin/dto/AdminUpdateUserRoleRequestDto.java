package com.pm4.istp.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Data;

@Data
public class AdminUpdateUserRoleRequestDto {
  @NotEmpty
  @Size(min = 1, max = 1)
  private Set<String> roles;
}
