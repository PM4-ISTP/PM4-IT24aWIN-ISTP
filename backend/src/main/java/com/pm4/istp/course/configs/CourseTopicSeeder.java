package com.pm4.istp.course.configs;

import com.pm4.istp.course.db.entities.CourseTopic;
import com.pm4.istp.course.repositories.CourseTopicRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CourseTopicSeeder implements ApplicationRunner {
  private final CourseTopicRepository courseTopicRepository;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (courseTopicRepository.count() > 0) {
      return;
    }

    List<String> defaults =
        List.of(
            "Cybersecurity",
            "Programming",
            "Design",
            "Data Science",
            "Networking",
            "Cloud",
            "DevOps",
            "Other");

    defaults.forEach(
        value -> {
          CourseTopic topic = new CourseTopic();
          topic.setTopic(value);
          topic.setActive(true);
          courseTopicRepository.save(topic);
        });
  }
}
