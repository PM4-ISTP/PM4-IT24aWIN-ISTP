package com.pm4.istp.course.db.entities;

import com.pm4.istp.course.db.InstructorRoleEnum;
import com.pm4.istp.user.db.entities.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Table(name = "course_instructors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseInstructor {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false, unique = true)
  private UUID id;

  @Column(name = "instructor_role", nullable = false)
  @Enumerated(EnumType.STRING)
  private InstructorRoleEnum instructorRole;

  @Column(name = "is_accepted", nullable = false)
  private boolean isAccepted = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "instructor_id", nullable = false)
  private User instructor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @CreatedDate
  @Column(name = "invited_at", nullable = false)
  private LocalDateTime invitedAt;

  @Column(name = "accepted_at", nullable = true)
  private LocalDateTime acceptedAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
