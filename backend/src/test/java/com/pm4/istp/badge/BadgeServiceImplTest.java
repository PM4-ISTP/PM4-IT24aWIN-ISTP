package com.pm4.istp.badge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.badge.db.entities.UserCourseBadge;
import com.pm4.istp.badge.dto.UpdateCourseBadgeRequestDto;
import com.pm4.istp.badge.repositories.UserCourseBadgeRepository;
import com.pm4.istp.badge.services.impl.BadgeServiceImpl;
import com.pm4.istp.course.db.InstructorRoleEnum;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.db.entities.CourseInstructor;
import com.pm4.istp.course.db.entities.CourseLab;
import com.pm4.istp.course.db.entities.Lab;
import com.pm4.istp.course.exceptions.CourseAccessDeniedException;
import com.pm4.istp.course.repositories.ChallengeCompletionRepository;
import com.pm4.istp.course.repositories.CourseRepository;
import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.repositories.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class BadgeServiceImplTest {

  @Mock private CourseRepository courseRepository;
  @Mock private UserRepository userRepository;
  @Mock private UserCourseBadgeRepository userCourseBadgeRepository;
  @Mock private ChallengeCompletionRepository challengeCompletionRepository;

  private BadgeServiceImpl badgeService;

  @BeforeEach
  void setUp() {
    badgeService =
        new BadgeServiceImpl(
            courseRepository,
            userRepository,
            userCourseBadgeRepository,
            challengeCompletionRepository);
  }

  @Test
  void getCourseBadgeConfig_missingBadgeFields_returnsDefaults() {
    UUID courseId = UUID.randomUUID();
    Course course = course(courseId, "Intro Security");
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    var dto = badgeService.getCourseBadgeConfig(courseId);

    assertThat(dto.courseId()).isEqualTo(courseId);
    assertThat(dto.courseTitle()).isEqualTo("Intro Security");
    assertThat(dto.primaryColor()).isEqualTo("#4f46e5");
    assertThat(dto.textColor()).isEqualTo("#ffffff");
    assertThat(dto.template()).isEqualTo(1);
    assertThat(dto.badgeIcon()).isNotBlank();
    assertThat(dto.badgeEnabled()).isTrue();
  }

  @Test
  void updateCourseBadgeConfig_owner_updatesAndDefaultsBlankIcon() {
    UUID courseId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    Course course = course(courseId, "Course");
    course.addCourseInstructor(instructor(ownerId, InstructorRoleEnum.OWNER));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(courseRepository.save(course)).thenReturn(course);

    var request = new UpdateCourseBadgeRequestDto("#123456", "#abcdef", 3, " ", false);

    var dto = badgeService.updateCourseBadgeConfig(ownerId, courseId, request);

    assertThat(course.getBadgePrimaryColor()).isEqualTo("#123456");
    assertThat(course.getBadgeTextColor()).isEqualTo("#abcdef");
    assertThat(course.getBadgeTemplate()).isEqualTo(3);
    assertThat(course.getBadgeIcon()).isNotBlank();
    assertThat(course.isBadgeEnabled()).isFalse();
    assertThat(dto.template()).isEqualTo(3);
  }

  @Test
  void updateCourseBadgeConfig_nonOwner_throwsAccessDenied() {
    UUID courseId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Course course = course(courseId, "Course");
    course.addCourseInstructor(instructor(UUID.randomUUID(), InstructorRoleEnum.OWNER));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    var request = new UpdateCourseBadgeRequestDto("#123456", "#ffffff", 1, "star", true);

    assertThatThrownBy(() -> badgeService.updateCourseBadgeConfig(userId, courseId, request))
        .isInstanceOf(CourseAccessDeniedException.class);

    verify(courseRepository, never()).save(any());
  }

  @Test
  void getUserBadges_mapsBadgeWithCourseDefaults() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    Course course = course(courseId, "Badged Course");
    UserCourseBadge badge = new UserCourseBadge();
    badge.setId(UUID.randomUUID());
    badge.setCourse(course);
    badge.setEarnedAt(LocalDateTime.now());
    when(userCourseBadgeRepository.findByUserIdFetchCourse(userId)).thenReturn(List.of(badge));

    var result = badgeService.getUserBadges(userId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).courseId()).isEqualTo(courseId);
    assertThat(result.get(0).primaryColor()).isEqualTo("#4f46e5");
  }

  @Test
  void tryAwardBadgesForChallenge_completedEnabledCourse_awardsBadge() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Course course = completedCourse(courseId, challengeId);
    User user = new User();
    user.setId(userId);

    when(courseRepository.findCoursesByChallengeIdAndEnrolledUserId(labId, userId))
        .thenReturn(List.of(course));
    when(userCourseBadgeRepository.existsByUserIdAndCourseId(userId, courseId)).thenReturn(false);
    when(challengeCompletionRepository.findSolvedChallengeIds(userId, List.of(challengeId)))
        .thenReturn(List.of(challengeId));
    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

    badgeService.tryAwardBadgesForChallenge(userId, labId);

    ArgumentCaptor<UserCourseBadge> captor = ArgumentCaptor.forClass(UserCourseBadge.class);
    verify(userCourseBadgeRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getUser()).isEqualTo(user);
    assertThat(captor.getValue().getCourse()).isEqualTo(course);
    assertThat(captor.getValue().getEarnedAt()).isNotNull();
  }

  @Test
  void tryAwardBadgesForChallenge_courseDisabledOrExistingBadge_skipsAward() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Course disabled = completedCourse(UUID.randomUUID(), UUID.randomUUID());
    disabled.setBadgeEnabled(false);
    Course existing = completedCourse(UUID.randomUUID(), UUID.randomUUID());

    when(courseRepository.findCoursesByChallengeIdAndEnrolledUserId(labId, userId))
        .thenReturn(List.of(disabled, existing));
    when(userCourseBadgeRepository.existsByUserIdAndCourseId(userId, existing.getId()))
        .thenReturn(true);

    badgeService.tryAwardBadgesForChallenge(userId, labId);

    verify(userCourseBadgeRepository, never()).saveAndFlush(any());
    verify(challengeCompletionRepository, never()).findSolvedChallengeIds(any(), any());
  }

  @Test
  void tryAwardBadgesForChallenge_notAllChallengesSolved_skipsAward() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Course course = completedCourse(UUID.randomUUID(), challengeId);

    when(courseRepository.findCoursesByChallengeIdAndEnrolledUserId(labId, userId))
        .thenReturn(List.of(course));
    when(userCourseBadgeRepository.existsByUserIdAndCourseId(userId, course.getId())).thenReturn(false);
    when(challengeCompletionRepository.findSolvedChallengeIds(userId, List.of(challengeId)))
        .thenReturn(List.of());

    badgeService.tryAwardBadgesForChallenge(userId, labId);

    verify(userCourseBadgeRepository, never()).saveAndFlush(any());
  }

  @Test
  void tryAwardBadgesForChallenge_concurrentBadgeInsert_isIgnored() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Course course = completedCourse(UUID.randomUUID(), challengeId);
    User user = new User();
    user.setId(userId);

    when(courseRepository.findCoursesByChallengeIdAndEnrolledUserId(labId, userId))
        .thenReturn(List.of(course));
    when(userCourseBadgeRepository.existsByUserIdAndCourseId(userId, course.getId())).thenReturn(false);
    when(challengeCompletionRepository.findSolvedChallengeIds(userId, List.of(challengeId)))
        .thenReturn(List.of(challengeId));
    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
    when(userCourseBadgeRepository.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("duplicate"));

    badgeService.tryAwardBadgesForChallenge(userId, labId);
  }

  private static Course course(UUID id, String title) {
    Course course = new Course();
    course.setId(id);
    course.setTitle(title);
    course.setBadgeEnabled(true);
    return course;
  }

  private static CourseInstructor instructor(UUID userId, InstructorRoleEnum role) {
    User user = new User();
    user.setId(userId);
    CourseInstructor instructor = new CourseInstructor();
    instructor.setInstructor(user);
    instructor.setInstructorRole(role);
    return instructor;
  }

  private static Course completedCourse(UUID courseId, UUID challengeId) {
    Course course = course(courseId, "Course");
    Lab lab = new Lab();
    Challenge challenge = new Challenge();
    challenge.setId(challengeId);
    challenge.setLab(lab);
    lab.getChallenges().add(challenge);
    CourseLab courseLab = new CourseLab();
    courseLab.setCourse(course);
    courseLab.setLab(lab);
    course.getCourseLabs().add(courseLab);
    return course;
  }
}
