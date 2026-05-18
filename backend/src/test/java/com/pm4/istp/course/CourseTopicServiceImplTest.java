package com.pm4.istp.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.course.db.entities.CourseTopic;
import com.pm4.istp.course.repositories.CourseTopicRepository;
import com.pm4.istp.course.services.CourseTopicService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseTopicServiceImplTest {

  @Mock private CourseTopicRepository courseTopicRepository;

  @InjectMocks private CourseTopicService courseTopicService;

  private CourseTopic topic(String name) {
    CourseTopic t = new CourseTopic();
    t.setTopic(name);
    t.setActive(true);
    return t;
  }

  // ── listActiveTopics ─────────────────────────────────────────────────────────

  @Test
  void listActiveTopics_returnsNamesOfActiveTopicsInOrder() {
    when(courseTopicRepository.findAllByActiveTrueOrderByTopicAsc())
        .thenReturn(List.of(topic("Docker"), topic("Security")));

    List<String> result = courseTopicService.listActiveTopics();

    assertThat(result).containsExactly("Docker", "Security");
    verify(courseTopicRepository).findAllByActiveTrueOrderByTopicAsc();
  }

  @Test
  void listActiveTopics_withNoActiveTopics_returnsEmptyList() {
    when(courseTopicRepository.findAllByActiveTrueOrderByTopicAsc()).thenReturn(List.of());

    List<String> result = courseTopicService.listActiveTopics();

    assertThat(result).isEmpty();
  }

  // ── normalizeAndValidate ────────────────────────────────────────────────────

  @Test
  void normalizeAndValidate_withNullInput_returnsNull() {
    String result = courseTopicService.normalizeAndValidate(null);

    assertThat(result).isNull();
  }

  @Test
  void normalizeAndValidate_withBlankInput_returnsNull() {
    String result = courseTopicService.normalizeAndValidate("   ");

    assertThat(result).isNull();
  }

  @Test
  void normalizeAndValidate_withValueExceedingMaxLength_throwsIllegalArgumentException() {
    String longTopic = "A".repeat(25);

    assertThatThrownBy(() -> courseTopicService.normalizeAndValidate(longTopic))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid topic. Please select a topic from the list.");
  }

  @Test
  void normalizeAndValidate_withValidTopicNotInDb_throwsIllegalArgumentException() {
    String topic = "Docker";
    when(courseTopicRepository.existsByTopicAndActiveTrue(topic)).thenReturn(false);

    assertThatThrownBy(() -> courseTopicService.normalizeAndValidate(topic))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid topic. Please select a topic from the list.");
  }

  @Test
  void normalizeAndValidate_withValidTopicInDb_returnsNormalizedValue() {
    String topic = "  Docker  ";
    when(courseTopicRepository.existsByTopicAndActiveTrue("Docker")).thenReturn(true);

    String result = courseTopicService.normalizeAndValidate(topic);

    assertThat(result).isEqualTo("Docker");
  }

  @Test
  void normalizeAndValidate_withExactly24CharsAndInDb_returnsValue() {
    String topic = "A".repeat(24);
    when(courseTopicRepository.existsByTopicAndActiveTrue(topic)).thenReturn(true);

    String result = courseTopicService.normalizeAndValidate(topic);

    assertThat(result).isEqualTo(topic);
  }
}
