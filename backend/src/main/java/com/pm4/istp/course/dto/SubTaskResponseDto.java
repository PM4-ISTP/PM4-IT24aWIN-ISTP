package com.pm4.istp.course.dto;

import com.pm4.istp.course.db.entities.SubTaskType;
import java.util.List;
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
  private SubTaskType type;
  private int points;
  private String hint;
  private List<SubTaskOptionResponseDto> options;
}
