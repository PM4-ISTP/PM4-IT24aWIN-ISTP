package com.pm4.istp.dto;

import com.pm4.istp.domain.entites.InstructorRoleEnum;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCourseInstructorRequestDto {
    @NotNull(message = "Instructor ID is required")
    private UUID instructorId;

  private InstructorRoleEnum instructorRole = InstructorRoleEnum.COLLABORATOR;
}
