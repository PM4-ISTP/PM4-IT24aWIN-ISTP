package com.pm4.istp.domain;

import com.pm4.istp.domain.entites.InstructorRoleEnum;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCourseInstructorRequest {
  private UUID instructorId;
  private InstructorRoleEnum instructorRole;
}
