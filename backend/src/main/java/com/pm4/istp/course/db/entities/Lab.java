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
import org.hibernate.annotations.SQLRestriction;
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

  /**
   * Optional: name of a Kubernetes Secret (in the lab namespace) whose key-value pairs are injected
   * as environment variables into the lab pod at startup.
   *
   * <p><b>Current state:</b> This field was introduced to demonstrate the concept for the "LLM01 -
   * Prompt Injection" lab, which requires a {@code GROQ_API_KEY} to call the Groq API. The secret
   * {@code groq-api-secret} is pre-created in the cluster by an admin and referenced here directly
   * in the database — there is no instructor-facing UI for this field yet.
   *
   * <p><b>Not yet fully available to instructors:</b> Future work should add a frontend field in
   * the lab create/edit form so instructors can configure this themselves without DB access.
   *
   * <p>Leave {@code null} for labs that need no external secrets.
   */
  @Column(name = "env_secret_name", nullable = true)
  private String envSecretName;

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
      orphanRemoval = false,
      fetch = FetchType.LAZY)
  @OrderBy("orderIndex ASC")
  @SQLRestriction("deleted_at IS NULL")
  private List<Challenge> challenges = new ArrayList<>();

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "deleted_by_username", length = 128)
  private String deletedByUsername;
}
