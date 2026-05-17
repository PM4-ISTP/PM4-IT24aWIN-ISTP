package com.pm4.istp.course.db.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "challenge_id", nullable = false)
  private Lab lab;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "description", nullable = false, length = 5000)
  private String description;

  @Column(name = "flag", nullable = true)
  private String flag;

  @Column(name = "order_index", nullable = false)
  private int orderIndex;

  @Column(name = "type", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  @ColumnDefault("'FLAG'")
  private ChallengeType type = ChallengeType.FLAG;

  /** Points awarded automatically when this challenge is solved correctly. */
  @Column(name = "points", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 1")
  private int points = 1;

  /** Optional hint text shown to the student on demand. */
  @Column(name = "hint", nullable = true, length = 1000)
  private String hint;

  @OneToMany(
      mappedBy = "challenge",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @OrderBy("orderIndex ASC")
  private List<ChallengeOption> options = new ArrayList<>();

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /** Soft-delete timestamp. Null means the challenge is active. */
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
