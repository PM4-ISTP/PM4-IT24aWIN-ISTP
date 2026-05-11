package com.pm4.istp.admin.dto;

import com.pm4.istp.course.db.entities.LabDifficultyEnum;
import com.pm4.istp.course.db.entities.LabStatusEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUpdateLabRequestDto {
  @NotBlank(message = "Title is required")
  @Size(max = 255, message = "Title must be at most 255 characters")
  private String title;

  @Size(max = 200, message = "Short description must be at most 200 characters")
  private String shortDescription;

  @Size(max = 5000, message = "Description must be at most 5000 characters")
  private String description;

  @NotNull private LabStatusEnum status;
  @NotNull private LabDifficultyEnum difficulty;
}
