package com.pm4.istp.admin.services.impl;

import com.pm4.istp.admin.services.AdminTopicService;
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
public class AdminTopicServiceImpl implements AdminTopicService {
  private static final int MIN_TOPIC_LENGTH = 3;
  private static final int MAX_TOPIC_LENGTH = 24;
  private final CourseTopicRepository courseTopicRepository;
  private final CourseRepository courseRepository;

  @Value("${istp.topics.max-count:200}")
  private long maxTopicCount;

  @Override
  @Transactional(readOnly = true)
  public List<String> listTopics() {
    return courseTopicRepository.findAllByActiveTrueOrderByTopicAsc().stream()
        .map(CourseTopic::getTopic)
        .toList();
  }

  @Override
  public void addTopic(String value) {
    String normalized = normalize(value);

    CourseTopic existing = courseTopicRepository.findById(normalized).orElse(null);
    if (existing != null) {
      if (existing.isActive()) {
        throw new IllegalArgumentException("Topic already exists");
      }
      existing.setActive(true);
      courseTopicRepository.save(existing);
      return;
    }

    if (courseTopicRepository.count() >= maxTopicCount) {
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

  @Override
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
    if (value == null) {
      throw new IllegalArgumentException("Topic value is required");
    }
    String trimmed = value.trim();
    if (trimmed.isBlank()) {
      throw new IllegalArgumentException("Topic value is required");
    }
    if (trimmed.length() < MIN_TOPIC_LENGTH) {
      throw new IllegalArgumentException("Topic must be at least 3 characters");
    }
    if (trimmed.length() > MAX_TOPIC_LENGTH) {
      throw new IllegalArgumentException("Topic must be at most 24 characters");
    }
    if (!trimmed.matches("^[A-Za-z][A-Za-z0-9-]*$")) {
      throw new IllegalArgumentException("Topic must be a single word (letters, numbers, '-')");
    }
    return trimmed;
  }
}
