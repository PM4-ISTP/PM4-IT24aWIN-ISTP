package com.pm4.istp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PublicTeamMemberDto {
  private String name;
  private String picture;
  private String title;
}
