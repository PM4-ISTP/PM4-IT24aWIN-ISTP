package com.pm4.istp.admin.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminUserDirectoryItemDto {
  private UUID id;
  private String email;
  private String username;
  private String firstName;
  private String lastName;
  private boolean enabled;

  // From app DB (if provisioned)
  private boolean provisioned;
  private LocalDateTime deletedAt;
  private LocalDateTime anonymizedAt;
  private Set<String> roles;
}
