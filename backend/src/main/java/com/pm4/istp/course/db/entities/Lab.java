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

@Entity
@Table(name = "labs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lab {
  public static final int DEFAULT_CONTAINER_PORT = 80;

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
  private LabStatusEnum status;

  @Column(name = "difficulty", nullable = false)
  @Enumerated(EnumType.STRING)
  private LabDifficultyEnum difficulty;

  @Column(name = "docker_image", nullable = false)
  private String dockerImage;

  @Column(name = "container_port", nullable = false)
  private Integer containerPort = DEFAULT_CONTAINER_PORT;

  @Column(name = "pod_ttl_seconds")
  private Integer podTtlSeconds;

  // This field will be filled by the Challenges which will come later, for now we
  // will set it manually
  // to 0
  @Column(name = "max_score", nullable = false)
  private int maxScore;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "creator_id", nullable = false)
  private User creator;

  @JsonIgnore
  @OneToMany(mappedBy = "lab", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CourseLab> courseLabs = new ArrayList<>();

  @JsonIgnore
  @OneToMany(
      mappedBy = "lab",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @OrderBy("orderIndex ASC")
  private List<Challenge> challenges = new ArrayList<>();

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
