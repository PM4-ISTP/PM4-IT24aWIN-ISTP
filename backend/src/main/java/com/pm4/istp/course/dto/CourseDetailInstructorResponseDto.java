package com.pm4.istp.course.dto;

import com.pm4.istp.course.db.InstructorRoleEnum;
import com.pm4.istp.user.dto.UserDto;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseDetailInstructorResponseDto {
  private UUID id;
  private InstructorRoleEnum instructorRole;
  private boolean isAccepted;
  private UserDto instructor;
  private LocalDateTime invitedAt;
  private LocalDateTime acceptedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
