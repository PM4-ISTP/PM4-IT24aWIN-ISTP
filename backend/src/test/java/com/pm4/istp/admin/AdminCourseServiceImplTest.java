package com.pm4.istp.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.admin.dto.AdminCourseListItemDto;
import com.pm4.istp.admin.dto.AdminUpdateCourseRequestDto;
import com.pm4.istp.admin.services.impl.AdminCourseServiceImpl;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.db.entities.CourseStatusEnum;
import com.pm4.istp.course.exceptions.CourseNotFoundException;
import com.pm4.istp.course.repositories.CourseRepository;
import com.pm4.istp.course.services.CourseInviteCodeHelper;
import com.pm4.istp.course.services.CourseTopicService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminCourseServiceImplTest {

  @Mock private CourseRepository courseRepository;
  @Mock private CourseTopicService courseTopicService;
  @Mock private CourseInviteCodeHelper courseInviteCodeHelper;

  @InjectMocks private AdminCourseServiceImpl adminCourseService;

  // ── listCourses ─────────────────────────────────────────────────────────────

  @Test
  void listCourses_withNullQuery_callsFindAllWithoutFilter() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<AdminCourseListItemDto> expected = new PageImpl<>(List.of(), pageable, 0);
    when(courseRepository.findAllCoursesForAdmin(pageable)).thenReturn(expected);

    Page<AdminCourseListItemDto> result = adminCourseService.listCourses(null, pageable);

    assertThat(result).isSameAs(expected);
    verify(courseRepository).findAllCoursesForAdmin(pageable);
    verify(courseRepository, never()).findAllCoursesForAdminByQuery(any(), any());
  }

  @Test
  void listCourses_withBlankQuery_treatsAsNullAndCallsFindAll() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<AdminCourseListItemDto> expected = new PageImpl<>(List.of(), pageable, 0);
    when(courseRepository.findAllCoursesForAdmin(pageable)).thenReturn(expected);

    Page<AdminCourseListItemDto> result = adminCourseService.listCourses("   ", pageable);

    assertThat(result).isSameAs(expected);
    verify(courseRepository).findAllCoursesForAdmin(pageable);
    verify(courseRepository, never()).findAllCoursesForAdminByQuery(any(), any());
  }

  @Test
  void listCourses_withNonBlankQuery_callsFindByQuery() {
    Pageable pageable = PageRequest.of(0, 10);
    String query = "spring";
    Page<AdminCourseListItemDto> expected = new PageImpl<>(List.of(), pageable, 0);
    when(courseRepository.findAllCoursesForAdminByQuery(query, pageable)).thenReturn(expected);

    Page<AdminCourseListItemDto> result = adminCourseService.listCourses(query, pageable);

    assertThat(result).isSameAs(expected);
    verify(courseRepository).findAllCoursesForAdminByQuery(query, pageable);
    verify(courseRepository, never()).findAllCoursesForAdmin(any());
  }

  // ── updateCourse ────────────────────────────────────────────────────────────

  @Test
  void updateCourse_withValidPublicRequest_updatesAllFields() {
    UUID id = UUID.randomUUID();
    Course course = new Course();
    course.setId(id);
    course.setInviteCode("OLDCOD");

    AdminUpdateCourseRequestDto request = new AdminUpdateCourseRequestDto();
    request.setTitle("Updated Title");
    request.setDescription("Updated description");
    request.setShortDescription("  short desc  ");
    request.setStatus(CourseStatusEnum.PUBLIC);
    request.setTopic("Security");
    request.setImageUrl("  https://example.com/img.png  ");

    when(courseRepository.findById(id)).thenReturn(Optional.of(course));
    when(courseTopicService.normalizeAndValidate("Security")).thenReturn("Security");
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    adminCourseService.updateCourse(id, request);

    ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
    verify(courseRepository).save(captor.capture());
    Course saved = captor.getValue();

    assertThat(saved.getTitle()).isEqualTo("Updated Title");
    assertThat(saved.getDescription()).isEqualTo("Updated description");
    assertThat(saved.getShortDescription()).isEqualTo("short desc");
    assertThat(saved.getStatus()).isEqualTo(CourseStatusEnum.PUBLIC);
    assertThat(saved.getTopic()).isEqualTo("Security");
    assertThat(saved.getImageUrl()).isEqualTo("https://example.com/img.png");
    // Invite code should be cleared for non-private course
    assertThat(saved.getInviteCode()).isNull();
  }

  @Test
  void updateCourse_makingCoursePrivateWithoutInviteCode_generatesInviteCode() {
    UUID id = UUID.randomUUID();
    Course course = new Course();
    course.setId(id);
    course.setInviteCode(null);

    AdminUpdateCourseRequestDto request = new AdminUpdateCourseRequestDto();
    request.setTitle("Private Course");
    request.setDescription("desc");
    request.setStatus(CourseStatusEnum.PRIVATE);
    request.setTopic(null);

    when(courseRepository.findById(id)).thenReturn(Optional.of(course));
    when(courseTopicService.normalizeAndValidate(null)).thenReturn(null);
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    adminCourseService.updateCourse(id, request);

    verify(courseInviteCodeHelper).generateAndAssign(id);
  }

  @Test
  void updateCourse_makingCoursePrivateWithExistingInviteCode_doesNotRegenerate() {
    UUID id = UUID.randomUUID();
    Course course = new Course();
    course.setId(id);
    course.setInviteCode("EXIST1");

    AdminUpdateCourseRequestDto request = new AdminUpdateCourseRequestDto();
    request.setTitle("Private Course");
    request.setDescription("desc");
    request.setStatus(CourseStatusEnum.PRIVATE);
    request.setTopic(null);

    when(courseRepository.findById(id)).thenReturn(Optional.of(course));
    when(courseTopicService.normalizeAndValidate(null)).thenReturn(null);
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    adminCourseService.updateCourse(id, request);

    verify(courseInviteCodeHelper, never()).generateAndAssign(any());
  }

  @Test
  void updateCourse_withBlankShortDescription_normalizesToNull() {
    UUID id = UUID.randomUUID();
    Course course = new Course();
    course.setId(id);

    AdminUpdateCourseRequestDto request = new AdminUpdateCourseRequestDto();
    request.setTitle("Course");
    request.setDescription("desc");
    request.setShortDescription("   ");
    request.setStatus(CourseStatusEnum.DRAFT);
    request.setTopic(null);

    when(courseRepository.findById(id)).thenReturn(Optional.of(course));
    when(courseTopicService.normalizeAndValidate(null)).thenReturn(null);
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    adminCourseService.updateCourse(id, request);

    ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
    verify(courseRepository).save(captor.capture());
    assertThat(captor.getValue().getShortDescription()).isNull();
  }

  @Test
  void updateCourse_whenCourseNotFound_throwsCourseNotFoundException() {
    UUID id = UUID.randomUUID();
    when(courseRepository.findById(id)).thenReturn(Optional.empty());

    AdminUpdateCourseRequestDto request = new AdminUpdateCourseRequestDto();
    request.setTitle("Course");
    request.setStatus(CourseStatusEnum.DRAFT);

    assertThatThrownBy(() -> adminCourseService.updateCourse(id, request))
        .isInstanceOf(CourseNotFoundException.class)
        .hasMessageContaining(id.toString());

    verify(courseRepository, never()).save(any());
  }

  // ── deleteCourse ────────────────────────────────────────────────────────────

  @Test
  void deleteCourse_whenExists_softDeletesCourse() {
    UUID id = UUID.randomUUID();
    Course course = new Course();
    course.setId(id);
    when(courseRepository.findById(id)).thenReturn(Optional.of(course));

    adminCourseService.deleteCourse(id);

    verify(courseRepository).save(course);
  }

  @Test
  void deleteCourse_whenNotFound_throwsCourseNotFoundException() {
    UUID id = UUID.randomUUID();
    when(courseRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adminCourseService.deleteCourse(id))
        .isInstanceOf(CourseNotFoundException.class)
        .hasMessageContaining(id.toString());

    verify(courseRepository, never()).save(any());
  }
}
