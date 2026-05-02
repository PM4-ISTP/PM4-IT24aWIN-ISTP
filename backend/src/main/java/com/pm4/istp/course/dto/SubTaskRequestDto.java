package com.pm4.istp.course.dto;

import com.pm4.istp.course.db.entities.SubTaskType;
import com.pm4.istp.shared.validation.ValidFlag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubTaskRequestDto {
  private UUID id;

  @NotBlank(message = "SubTask title is required")
  @Size(max = 200, message = "SubTask title must not exceed 200 characters")
  private String title;

  @NotBlank(message = "SubTask description is required")
  @Size(max = 5000, message = "SubTask description must not exceed 5000 characters")
  private String description;

  @ValidFlag private String flag;

  @Min(value = 0, message = "orderIndex must be >= 0")
  private int orderIndex;

  private SubTaskType type;

  @Min(value = 1, message = "Points must be at least 1")
  private int points = 1;

  @Size(max = 1000, message = "Hint must not exceed 1000 characters")
  private String hint;

  @Valid private List<SubTaskOptionRequestDto> options;
}
