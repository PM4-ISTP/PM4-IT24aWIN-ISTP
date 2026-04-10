package com.pm4.istp.domain.entites;

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
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Course {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false, unique = true)
  private UUID id;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "description", nullable = true)
  private String description;

  @Column(name = "short_description", nullable = true, length = 200)
  private String shortDescription;

  @Column(name = "isPublished", nullable = false)
  private boolean isPublished;

  @Column(name = "image_url", nullable = true)
  private String imageUrl;

  @Column(name = "topic", nullable = true)
  private String topic;

  @Column(name = "difficulty", nullable = true)
  @Enumerated(EnumType.STRING)
  private CourseDifficultyEnum difficulty;

  @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CourseInstructor> courseInstructors = new ArrayList<>();

  @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CourseEnrollment> courseEnrollments = new ArrayList<>();

  public void addCourseInstructor(CourseInstructor courseInstructor) {
    courseInstructors.add(courseInstructor);
    courseInstructor.setCourse(this);
  }

  public void removeCourseInstructor(CourseInstructor courseInstructor) {
    courseInstructors.remove(courseInstructor);
    courseInstructor.setCourse(null);
  }

  public void addCourseEnrollment(CourseEnrollment courseEnrollment) {
    courseEnrollments.add(courseEnrollment);
    courseEnrollment.setCourse(this);
  }

  public void removeCourseEnrollment(CourseEnrollment courseEnrollment) {
    courseEnrollments.remove(courseEnrollment);
    courseEnrollment.setCourse(null);
  }

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
