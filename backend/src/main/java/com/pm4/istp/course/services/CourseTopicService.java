package com.pm4.istp.course.services;

import java.util.List;

public interface CourseTopicService {
  List<String> listActiveTopics();

  String normalizeAndValidate(String topic);
}

