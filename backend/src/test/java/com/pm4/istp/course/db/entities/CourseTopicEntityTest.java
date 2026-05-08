package com.pm4.istp.course.db.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CourseTopicEntityTest {

  @Test
  void prePersistSetsTimestampsAndKeepsExistingCreatedAt() {
    CourseTopic newTopic = new CourseTopic();

    newTopic.prePersist();

    assertThat(newTopic.getCreatedAt()).isNotNull();
    assertThat(newTopic.getUpdatedAt()).isNotNull();

    LocalDateTime existingCreatedAt = LocalDateTime.now().minusDays(1);
    CourseTopic existingTopic = new CourseTopic();
    existingTopic.setCreatedAt(existingCreatedAt);

    existingTopic.prePersist();

    assertThat(existingTopic.getCreatedAt()).isEqualTo(existingCreatedAt);
    assertThat(existingTopic.getUpdatedAt()).isNotNull();
  }

  @Test
  void preUpdateRefreshesUpdatedAtOnly() {
    LocalDateTime createdAt = LocalDateTime.now().minusDays(2);
    LocalDateTime oldUpdatedAt = LocalDateTime.now().minusDays(1);
    CourseTopic topic = new CourseTopic();
    topic.setCreatedAt(createdAt);
    topic.setUpdatedAt(oldUpdatedAt);

    topic.preUpdate();

    assertThat(topic.getCreatedAt()).isEqualTo(createdAt);
    assertThat(topic.getUpdatedAt()).isAfter(oldUpdatedAt);
  }
}
