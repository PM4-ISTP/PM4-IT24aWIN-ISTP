package com.pm4.istp.course.dto;

import com.pm4.istp.course.db.InstructorRoleEnum;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCourseInstructorRequestDto {
  @NotNull(message = "Instructor ID is required")
  private UUID instructorId;

  private InstructorRoleEnum instructorRole = InstructorRoleEnum.COLLABORATOR;
}
