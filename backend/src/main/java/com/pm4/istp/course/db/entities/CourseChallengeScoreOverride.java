package com.pm4.istp.course.db.entities;

import com.pm4.istp.user.db.entities.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "course_challenge_score_overrides",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_course_challenge_score_override",
            columnNames = {"course_id", "participant_id", "challenge_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseChallengeScoreOverride {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false, unique = true)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "participant_id", nullable = false)
  private User participant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "challenge_id", nullable = false)
  private Challenge challenge;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "updated_by_instructor_id", nullable = false)
  private User updatedByInstructor;

  @Column(name = "points", nullable = false)
  private int points;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
