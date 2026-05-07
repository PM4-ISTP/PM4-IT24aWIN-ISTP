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
    name = "student_option_submissions",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_student_option_submission_user_subtask",
            columnNames = {"user_id", "sub_task_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentOptionSubmission {
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

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "selected_option_id",
      nullable = false,
      foreignKey =
          @ForeignKey(
              name = "fk_student_option_submission_option",
              foreignKeyDefinition =
                  "FOREIGN KEY (selected_option_id) REFERENCES challenge_options(id) ON DELETE CASCADE"))
  private ChallengeOption selectedOption;

  @Column(name = "is_correct", nullable = false)
  private boolean correct;

  @Column(name = "submitted_at", nullable = false, updatable = false)
  private LocalDateTime submittedAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
