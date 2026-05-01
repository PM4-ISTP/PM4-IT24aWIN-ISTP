package com.pm4.istp.user.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
  private UUID id;
  private String name;
  private String email;
  private String username;
  private String firstName;
  private String lastName;
  private String picture;
  private String title;
}
