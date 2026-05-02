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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Table(name = "sub_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubTask {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false, unique = true)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "challenge_id", nullable = false)
  private Challenge challenge;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "description", nullable = false, length = 5000)
  private String description;

  @Column(name = "flag", nullable = true)
  private String flag;

  @Column(name = "order_index", nullable = false)
  private int orderIndex;

  @Column(name = "type", nullable = false, columnDefinition = "VARCHAR(50) NOT NULL DEFAULT 'FLAG'")
  @Enumerated(EnumType.STRING)
  private SubTaskType type = SubTaskType.FLAG;

  /** Points awarded automatically when this sub-task is solved correctly. */
  @Column(name = "points", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 1")
  private int points = 1;

  /** Optional hint text shown to the student on demand. */
  @Column(name = "hint", nullable = true, length = 1000)
  private String hint;

  @OneToMany(mappedBy = "subTask", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @OrderBy("orderIndex ASC")
  private List<SubTaskOption> options = new ArrayList<>();

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
