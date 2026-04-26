package com.pm4.istp.admin.services;

import java.util.List;

public interface AdminTopicService {
  List<String> listTopics();

  void addTopic(String value);

  void deleteTopic(String value);
}

