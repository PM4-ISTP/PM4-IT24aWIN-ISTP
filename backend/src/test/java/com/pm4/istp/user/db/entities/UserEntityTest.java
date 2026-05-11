package com.pm4.istp.user.db.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserEntityTest {

  @Test
  void isDeletedReflectsDeletedAt() {
    User user = new User();

    assertThat(user.isDeleted()).isFalse();

    user.setDeletedAt(LocalDateTime.now());

    assertThat(user.isDeleted()).isTrue();
  }

  @Test
  void equalsAndHashCodeUseProfileFields() {
    User user = fullyPopulatedUser();
    User same = fullyPopulatedUser();

    assertThat(user)
        .isEqualTo(same)
        .hasSameHashCodeAs(same)
        .isNotEqualTo(null)
        .isNotEqualTo("not a user");
  }

  @Test
  void equalsDetectsChangedProfileFields() {
    User user = fullyPopulatedUser();

    assertThat(user)
        .isNotEqualTo(mutatedUser(copy -> copy.setId(UUID.randomUUID())))
        .isNotEqualTo(mutatedUser(copy -> copy.setName("Other")))
        .isNotEqualTo(mutatedUser(copy -> copy.setEmail("other@example.com")))
        .isNotEqualTo(mutatedUser(copy -> copy.setUsername("other")))
        .isNotEqualTo(mutatedUser(copy -> copy.setFirstName("Other")))
        .isNotEqualTo(mutatedUser(copy -> copy.setLastName("Other")))
        .isNotEqualTo(mutatedUser(copy -> copy.setPicture("https://example.com/b.png")))
        .isNotEqualTo(mutatedUser(copy -> copy.setTitle("Other")))
        .isNotEqualTo(mutatedUser(copy -> copy.setCreatedAt(LocalDateTime.now().minusDays(5))))
        .isNotEqualTo(mutatedUser(copy -> copy.setUpdatedAt(LocalDateTime.now().minusDays(4))))
        .isNotEqualTo(mutatedUser(copy -> copy.setDeletedAt(LocalDateTime.now())))
        .isNotEqualTo(mutatedUser(copy -> copy.setAnonymizedAt(LocalDateTime.now())))
        .isNotEqualTo(mutatedUser(copy -> copy.setTotalSecondsOnline(99L)));
  }

  private User mutatedUser(java.util.function.Consumer<User> mutation) {
    User copy = fullyPopulatedUser();
    mutation.accept(copy);
    return copy;
  }

  private User fullyPopulatedUser() {
    User user = new User();
    user.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    user.setName("Test User");
    user.setEmail("test@example.com");
    user.setUsername("testuser");
    user.setFirstName("Test");
    user.setLastName("User");
    user.setPicture("https://example.com/a.png");
    user.setTitle("Student");
    user.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
    user.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 10, 0));
    user.setDeletedAt(null);
    user.setAnonymizedAt(null);
    user.setTotalSecondsOnline(42L);
    return user;
  }
}
