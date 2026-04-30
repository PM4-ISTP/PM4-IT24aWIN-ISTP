package com.pm4.istp.user.db.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.CourseEnrollment;
import com.pm4.istp.course.db.entities.CourseInstructor;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
  @Id
  @Column(name = "id", updatable = false, nullable = false, unique = true)
  private UUID id;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "email", nullable = false, length = 255)
  private String email;

  @Column(name = "username", length = 255)
  private String username;

  @Column(name = "first_name", length = 255)
  private String firstName;

  @Column(name = "last_name", length = 255)
  private String lastName;

  @Column(name = "picture", columnDefinition = "TEXT")
  private String picture;

  @Column(name = "title", length = 255)
  private String title;

  @JsonIgnore
  @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL)
  private List<Challenge> creatorChallenges = new ArrayList<>();

  @JsonIgnore
  @OneToMany(mappedBy = "instructor", cascade = CascadeType.ALL)
  private List<CourseInstructor> coursesInstructors = new ArrayList<>();

  @JsonIgnore
  @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL)
  private List<CourseEnrollment> courseEnrollments = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private Set<UserRoleEnum> roles = new HashSet<>();

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  // Marks an irreversible soft-delete where identifiers were anonymized to free email/username for reuse.
  @Column(name = "anonymized_at")
  private LocalDateTime anonymizedAt;

  public boolean isDeleted() {
    return deletedAt != null;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    User user = (User) o;
    return Objects.equals(id, user.id)
        && Objects.equals(name, user.name)
        && Objects.equals(email, user.email)
        && Objects.equals(username, user.username)
        && Objects.equals(firstName, user.firstName)
        && Objects.equals(lastName, user.lastName)
        && Objects.equals(picture, user.picture)
        && Objects.equals(title, user.title)
        && Objects.equals(createdAt, user.createdAt)
        && Objects.equals(updatedAt, user.updatedAt)
        && Objects.equals(deletedAt, user.deletedAt)
        && Objects.equals(anonymizedAt, user.anonymizedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id, name, email, username, firstName, lastName, picture, title, createdAt, updatedAt,
        deletedAt, anonymizedAt);
  }
}
