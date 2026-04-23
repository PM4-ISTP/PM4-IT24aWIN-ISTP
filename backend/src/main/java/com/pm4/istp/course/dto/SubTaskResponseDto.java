package com.pm4.istp.course.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubTaskResponseDto {
  private UUID id;
  private String title;
  private String description;
  private String flag;
  private int orderIndex;
}
