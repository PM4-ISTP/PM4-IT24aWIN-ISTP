package com.pm4.istp.course.db.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

  @Column(name = "description", nullable = true, length = 5000)
  private String description;

  @Column(name = "short_description", nullable = true, length = 200)
  private String shortDescription;

  @Column(name = "status", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @ColumnDefault("'DRAFT'")
  private CourseStatusEnum status = CourseStatusEnum.DRAFT;

  @Column(name = "image_url", nullable = true, length = 2048)
  private String imageUrl;

  @Column(name = "topic", nullable = true)
  private String topic;

  @Column(name = "invite_code", nullable = true, unique = true, length = 6)
  private String inviteCode;

  @JsonIgnore
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

  @JsonIgnore
  @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("orderIndex ASC")
  private List<CourseLab> courseLabs = new ArrayList<>();

  public void addCourseChallenge(CourseLab courseLab) {
    courseLabs.add(courseLab);
    courseLab.setCourse(this);
  }

  public void removeCourseChallenge(CourseLab courseLab) {
    courseLabs.remove(courseLab);
    courseLab.setCourse(null);
  }

  /**
   * Controls how many attempts students get for MULTIPLE_CHOICE challenges in this course. Defaults
   * to UNLIMITED (self-learning). Set to ONCE for graded / Praktikum courses.
   */
  @Column(name = "mc_attempts_mode", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @ColumnDefault("'UNLIMITED'")
  private McAttemptsMode mcAttemptsMode = McAttemptsMode.UNLIMITED;

  @Column(name = "badge_enabled", nullable = false, columnDefinition = "boolean default true")
  private boolean badgeEnabled = true;

  @Column(name = "badge_primary_color", nullable = true, length = 7)
  private String badgePrimaryColor;

  @Column(name = "badge_text_color", nullable = true, length = 7)
  private String badgeTextColor;

  @Column(name = "badge_template", nullable = true)
  private Integer badgeTemplate;

  @Column(name = "badge_icon", nullable = true, length = 16)
  private String badgeIcon;

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
