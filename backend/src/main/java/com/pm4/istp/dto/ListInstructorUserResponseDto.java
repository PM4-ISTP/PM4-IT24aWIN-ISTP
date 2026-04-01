package com.pm4.istp.dto;

import com.pm4.istp.domain.entites.UserRoleEnum;
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
  private String picture;
  private Set<UserRoleEnum> roles;
}
