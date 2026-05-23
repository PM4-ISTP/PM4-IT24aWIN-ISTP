package com.pm4.istp.course.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LabCreatorResponseDto {
  private UUID id;
  private String name;
}
