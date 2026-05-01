package com.pm4.istp.user.dto;

import com.pm4.istp.user.db.entities.UserRoleEnum;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListInstructorUserResponseDto {
  private UUID id;
  private String name;
  private String email;
  private String username;
  private String picture;
  private Set<UserRoleEnum> roles;
}
