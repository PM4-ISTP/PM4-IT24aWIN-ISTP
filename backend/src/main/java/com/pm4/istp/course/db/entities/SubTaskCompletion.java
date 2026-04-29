package com.pm4.istp.course.db.entities;

import com.pm4.istp.user.db.entities.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "sub_task_completions",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_sub_task_completion_user_sub_task",
            columnNames = {"user_id", "sub_task_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubTaskCompletion {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false, unique = true)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sub_task_id", nullable = false)
  private SubTask subTask;

  @Column(name = "solved_at", nullable = false, updatable = false)
  private LocalDateTime solvedAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
