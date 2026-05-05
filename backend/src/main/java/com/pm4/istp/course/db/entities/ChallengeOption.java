package com.pm4.istp.course.db.entities;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sub_task_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeOption {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false, unique = true)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sub_task_id", nullable = false)
  private Challenge challenge;

  @Column(name = "text", nullable = false, length = 500)
  private String text;

  @Column(name = "is_correct", nullable = false)
  private boolean correct;

  @Column(name = "order_index", nullable = false)
  private int orderIndex;
}
