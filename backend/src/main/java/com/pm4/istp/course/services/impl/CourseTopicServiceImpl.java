package com.pm4.istp.course.services.impl;

import com.pm4.istp.course.repositories.CourseTopicRepository;
import com.pm4.istp.course.services.CourseTopicService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseTopicServiceImpl implements CourseTopicService {
  private static final int MAX_TOPIC_LENGTH = 24;
  private final CourseTopicRepository courseTopicRepository;

  @Override
  @Transactional(readOnly = true)
  public List<String> listActiveTopics() {
    return courseTopicRepository.findAllByActiveTrueOrderByTopicAsc().stream()
        .map(CourseTopic::getTopic)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public String normalizeAndValidate(String topic) {
    if (topic == null) {
      return null;
    }
    String normalized = topic.trim();
    if (normalized.isBlank()) {
      return null;
    }
    if (normalized.length() > MAX_TOPIC_LENGTH) {
      throw new IllegalArgumentException("Invalid topic. Please select a topic from the list.");
    }
    if (!courseTopicRepository.existsByTopicAndActiveTrue(normalized)) {
      throw new IllegalArgumentException("Invalid topic. Please select a topic from the list.");
    }
    return normalized;
  }
}
