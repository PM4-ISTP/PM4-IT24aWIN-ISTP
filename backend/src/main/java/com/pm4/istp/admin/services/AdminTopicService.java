package com.pm4.istp.admin.services;

import com.pm4.istp.course.db.entities.CourseTopic;
import com.pm4.istp.course.repositories.CourseRepository;
import com.pm4.istp.course.repositories.CourseTopicRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminTopicService {
  private final CourseTopicRepository courseTopicRepository;
  private final CourseRepository courseRepository;

  @Value("${istp.topics.max-count:50}")
  private int maxTopicCount;

  @Transactional(readOnly = true)
  public List<String> listTopics() {
    return courseTopicRepository.findAllByActiveTrueOrderByTopicAsc().stream()
        .map(CourseTopic::getTopic)
        .toList();
  }

  public void addTopic(String value) {
    String normalized = normalize(value);

    CourseTopic existing = courseTopicRepository.findById(normalized).orElse(null);
    if (existing != null) {
      if (existing.isActive()) {
        throw new IllegalArgumentException("Topic already exists");
      }
      if (courseTopicRepository.countByActiveTrue() >= maxTopicCount) {
        throw new IllegalArgumentException("Topic limit reached");
      }
      existing.setActive(true);
      courseTopicRepository.save(existing);
      return;
    }

    if (courseTopicRepository.countByActiveTrue() >= maxTopicCount) {
      throw new IllegalArgumentException("Topic limit reached");
    }

    CourseTopic topic = new CourseTopic();
    topic.setTopic(normalized);
    topic.setActive(true);
    try {
      courseTopicRepository.save(topic);
    } catch (DataIntegrityViolationException ex) {
      // Concurrent insert (or unique constraint violation) - treat as "already exists"
      throw new IllegalArgumentException("Topic already exists");
    }
  }

  public void deleteTopic(String value) {
    String normalized = normalize(value);
    CourseTopic existing = courseTopicRepository.findById(normalized).orElse(null);
    if (existing == null) {
      return;
    }
    courseRepository.clearTopic(normalized);
    existing.setActive(false);
    courseTopicRepository.save(existing);
  }

  private static String normalize(String value) {
    return value.trim();
  }
}
