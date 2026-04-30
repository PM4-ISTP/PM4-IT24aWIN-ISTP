package com.pm4.istp.admin.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminUserListItemDto {
  private UUID id;
  private String name;
  private String email;
  private String username;
  private String firstName;
  private String lastName;
  private String title;
  private String picture;
  private Set<String> roles;
  private LocalDateTime deletedAt;
  private LocalDateTime anonymizedAt;
}
