package com.pm4.istp.course.db.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pm4.istp.user.db.entities.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "challenges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Challenge {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false, unique = true)
  private UUID id;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "short_description")
  private String shortDescription;

  @Column(name = "description", nullable = true, length = 5000)
  private String description;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private ChallengeStatusEnum status;

  @Column(name = "difficulty", nullable = false)
  @Enumerated(EnumType.STRING)
  private ChallengeDifficultyEnum difficulty;

  // This field wil be filled by the SubTasks which will come later, for now we
  // will set it manually
  // to 0
  @Column(name = "max_score", nullable = false)
  private int maxScore;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "creator_id", nullable = false)
  private User creator;

  @JsonIgnore
  @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CourseChallenge> courseChallenges = new ArrayList<>();

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
