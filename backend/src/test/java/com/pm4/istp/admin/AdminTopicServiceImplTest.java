package com.pm4.istp.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.admin.services.impl.AdminTopicServiceImpl;
import com.pm4.istp.course.db.entities.CourseTopic;
import com.pm4.istp.course.repositories.CourseRepository;
import com.pm4.istp.course.repositories.CourseTopicRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminTopicServiceImplTest {

  @Mock private CourseTopicRepository courseTopicRepository;
  @Mock private CourseRepository courseRepository;

  @InjectMocks private AdminTopicServiceImpl adminTopicService;

  private void setMaxTopicCount(int max) {
    ReflectionTestUtils.setField(adminTopicService, "maxTopicCount", max);
  }

  private CourseTopic activeTopic(String name) {
    CourseTopic t = new CourseTopic();
    t.setTopic(name);
    t.setActive(true);
    return t;
  }

  private CourseTopic inactiveTopic(String name) {
    CourseTopic t = new CourseTopic();
    t.setTopic(name);
    t.setActive(false);
    return t;
  }

  // ── listTopics ──────────────────────────────────────────────────────────────

  @Test
  void listTopics_returnsNamesOfActiveTopicsInOrder() {
    CourseTopic a = activeTopic("Docker");
    CourseTopic b = activeTopic("Web");
    when(courseTopicRepository.findAllByActiveTrueOrderByTopicAsc()).thenReturn(List.of(a, b));

    List<String> result = adminTopicService.listTopics();

    assertThat(result).containsExactly("Docker", "Web");
  }

  @Test
  void listTopics_withNoActiveTopics_returnsEmptyList() {
    when(courseTopicRepository.findAllByActiveTrueOrderByTopicAsc()).thenReturn(List.of());

    List<String> result = adminTopicService.listTopics();

    assertThat(result).isEmpty();
  }

  // ── addTopic ────────────────────────────────────────────────────────────────

  @Test
  void addTopic_withNewValidValue_savesNewActiveTopic() {
    setMaxTopicCount(50);
    String value = "Docker";
    when(courseTopicRepository.findById(value)).thenReturn(Optional.empty());
    when(courseTopicRepository.countByActiveTrue()).thenReturn(5L);

    adminTopicService.addTopic(value);

    verify(courseTopicRepository).save(any(CourseTopic.class));
  }

  @Test
  void addTopic_withExistingInactiveTopic_reactivatesIt() {
    setMaxTopicCount(50);
    CourseTopic existing = inactiveTopic("Docker");
    when(courseTopicRepository.findById("Docker")).thenReturn(Optional.of(existing));
    when(courseTopicRepository.countByActiveTrue()).thenReturn(5L);

    adminTopicService.addTopic("Docker");

    assertThat(existing.isActive()).isTrue();
    verify(courseTopicRepository).save(existing);
  }

  @Test
  void addTopic_withExistingActiveTopic_throwsIllegalArgumentException() {
    CourseTopic existing = activeTopic("Docker");
    when(courseTopicRepository.findById("Docker")).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> adminTopicService.addTopic("Docker"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Topic already exists");
  }

  @Test
  void addTopic_whenTopicLimitReachedForNewTopic_throwsIllegalArgumentException() {
    setMaxTopicCount(2);
    when(courseTopicRepository.findById("Docker")).thenReturn(Optional.empty());
    when(courseTopicRepository.countByActiveTrue()).thenReturn(2L);

    assertThatThrownBy(() -> adminTopicService.addTopic("Docker"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Topic limit reached");
  }

  @Test
  void addTopic_whenTopicLimitReachedForReactivation_throwsIllegalArgumentException() {
    setMaxTopicCount(2);
    CourseTopic existing = inactiveTopic("Docker");
    when(courseTopicRepository.findById("Docker")).thenReturn(Optional.of(existing));
    when(courseTopicRepository.countByActiveTrue()).thenReturn(2L);

    assertThatThrownBy(() -> adminTopicService.addTopic("Docker"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Topic limit reached");
  }

  @Test
  void addTopic_whenConcurrentInsertConflict_throwsIllegalArgumentException() {
    setMaxTopicCount(50);
    when(courseTopicRepository.findById("Docker")).thenReturn(Optional.empty());
    when(courseTopicRepository.countByActiveTrue()).thenReturn(5L);
    when(courseTopicRepository.save(any(CourseTopic.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    assertThatThrownBy(() -> adminTopicService.addTopic("Docker"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Topic already exists");
  }

  // ── addTopic input validation ────────────────────────────────────────────────

  @Test
  void addTopic_withNullValue_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> adminTopicService.addTopic(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Topic value is required");
  }

  @Test
  void addTopic_withBlankValue_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> adminTopicService.addTopic("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Topic value is required");
  }

  @Test
  void addTopic_withTooShortValue_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> adminTopicService.addTopic("ab"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Topic must be at least 3 characters");
  }

  @Test
  void addTopic_withTooLongValue_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> adminTopicService.addTopic("A".repeat(25)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Topic must be at most 24 characters");
  }

  @Test
  void addTopic_withInvalidCharacters_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> adminTopicService.addTopic("invalid topic"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Topic must be a single word (letters, numbers, '-')");
  }

  @Test
  void addTopic_withValueStartingWithDigit_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> adminTopicService.addTopic("1Docker"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Topic must be a single word (letters, numbers, '-')");
  }

  @Test
  void addTopic_withValidHyphenatedValue_succeeds() {
    setMaxTopicCount(50);
    String value = "Web-Security";
    when(courseTopicRepository.findById(value)).thenReturn(Optional.empty());
    when(courseTopicRepository.countByActiveTrue()).thenReturn(0L);

    adminTopicService.addTopic(value);

    verify(courseTopicRepository).save(any(CourseTopic.class));
  }

  // ── deleteTopic ─────────────────────────────────────────────────────────────

  @Test
  void deleteTopic_whenTopicExists_softDeletesItAndClearsCourses() {
    CourseTopic existing = activeTopic("Docker");
    when(courseTopicRepository.findById("Docker")).thenReturn(Optional.of(existing));

    adminTopicService.deleteTopic("Docker");

    verify(courseRepository).clearTopic("Docker");
    assertThat(existing.isActive()).isFalse();
    verify(courseTopicRepository).save(existing);
  }

  @Test
  void deleteTopic_whenTopicDoesNotExist_isIdempotent() {
    when(courseTopicRepository.findById("Docker")).thenReturn(Optional.empty());

    adminTopicService.deleteTopic("Docker");

    verify(courseRepository, never()).clearTopic(any());
    verify(courseTopicRepository, never()).save(any());
  }
}
