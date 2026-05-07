package com.pm4.istp.course.db.entities;

import com.pm4.istp.user.db.entities.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
    name = "student_flag_submissions",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_student_flag_submission_user_challenge",
            columnNames = {"user_id", "sub_task_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentFlagSubmission {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false, unique = true)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sub_task_id", nullable = false)
  private Challenge challenge;

  @Column(name = "submitted_flag", nullable = false)
  private String submittedFlag;

  @Column(name = "is_correct", nullable = false)
  private boolean correct;

  @Column(name = "submitted_at", nullable = false)
  private LocalDateTime submittedAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
