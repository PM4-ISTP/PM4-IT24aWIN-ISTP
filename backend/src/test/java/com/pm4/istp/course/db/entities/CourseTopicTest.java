package com.pm4.istp.course.db.entities;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CourseTopicTest {

  @Test
  void prePersistSetsTimestampsAndPreUpdateRefreshesUpdatedAt() throws Exception {
    CourseTopic topic = new CourseTopic();
    topic.setTopic("web");
    assertThat(topic.isActive()).isTrue();

    topic.prePersist();

    assertThat(topic.getCreatedAt()).isNotNull();
    assertThat(topic.getUpdatedAt()).isNotNull();
    topic.setUpdatedAt(topic.getUpdatedAt().minusNanos(1));
    var firstUpdatedAt = topic.getUpdatedAt();

    topic.preUpdate();

    assertThat(topic.getCreatedAt()).isNotNull();
    assertThat(topic.getUpdatedAt()).isAfter(firstUpdatedAt);
  }
}
