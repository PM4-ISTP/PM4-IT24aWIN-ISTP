package com.pm4.istp.domain;

import com.pm4.istp.domain.entites.InstructorRoleEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCourseInstructorRequest {
    private UUID instructorId;
    private InstructorRoleEnum instructorRole;
}
