package com.pm4.istp.badge.services.impl;

import com.pm4.istp.badge.db.entities.UserCourseBadge;
import com.pm4.istp.badge.dto.CourseBadgeConfigDto;
import com.pm4.istp.badge.dto.UpdateCourseBadgeRequestDto;
import com.pm4.istp.badge.dto.UserBadgeDto;
import com.pm4.istp.badge.repositories.UserCourseBadgeRepository;
import com.pm4.istp.badge.services.BadgeService;
import com.pm4.istp.course.db.InstructorRoleEnum;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.db.entities.CourseLab;
import com.pm4.istp.course.exceptions.CourseAccessDeniedException;
import com.pm4.istp.course.repositories.ChallengeCompletionRepository;
import com.pm4.istp.course.repositories.CourseRepository;
import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.repositories.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BadgeServiceImpl implements BadgeService {

  private static final String DEFAULT_COLOR = "#4f46e5";
  private static final String DEFAULT_TEXT_COLOR = "#ffffff";
  private static final int DEFAULT_TEMPLATE = 1;
  private static final String DEFAULT_ICON = "🏆";

  private final CourseRepository courseRepository;
  private final UserRepository userRepository;
  private final UserCourseBadgeRepository userCourseBadgeRepository;
  private final ChallengeCompletionRepository challengeCompletionRepository;

  @Override
  @Transactional(readOnly = true)
  public CourseBadgeConfigDto getCourseBadgeConfig(UUID courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));
    return toConfigDto(course);
  }

  @Override
  @Transactional
  public CourseBadgeConfigDto updateCourseBadgeConfig(
      UUID userId, UUID courseId, UpdateCourseBadgeRequestDto request) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

    verifyOwner(course, userId);

    course.setBadgePrimaryColor(request.primaryColor());
    course.setBadgeTextColor(request.textColor());
    course.setBadgeTemplate(request.template());
    course.setBadgeIcon(
        request.badgeIcon() == null || request.badgeIcon().isBlank()
            ? DEFAULT_ICON
            : request.badgeIcon());
    course.setBadgeEnabled(request.badgeEnabled());

    Course saved = courseRepository.save(course);
    return toConfigDto(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserBadgeDto> getUserBadges(UUID userId) {
    List<UserCourseBadge> badges = userCourseBadgeRepository.findByUserIdFetchCourse(userId);
    List<UserBadgeDto> result = new ArrayList<>();
    for (UserCourseBadge b : badges) {
      result.add(toBadgeDto(b));
    }
    return result;
  }

  @Override
  @Transactional
  public void tryAwardBadgesForChallenge(UUID userId, UUID labId) {
    List<Course> courses =
        courseRepository.findCoursesByChallengeIdAndEnrolledUserId(labId, userId);

    for (Course course : courses) {
      if (canAwardBadge(userId, course) && isCourseCompleted(userId, course)) {
        awardBadge(userId, course);
      }
    }
  }

  private boolean canAwardBadge(UUID userId, Course course) {
    return course.isBadgeEnabled()
        && !userCourseBadgeRepository.existsByUserIdAndCourseId(userId, course.getId());
  }

  private boolean isCourseCompleted(UUID userId, Course course) {
    List<UUID> allChallengeIds = new ArrayList<>();
    for (CourseLab cc : course.getCourseLabs()) {
      for (Challenge st : cc.getLab().getChallenges()) {
        allChallengeIds.add(st.getId());
      }
    }
    if (allChallengeIds.isEmpty()) {
      return false;
    }
    List<UUID> solvedIds =
        challengeCompletionRepository.findSolvedChallengeIds(userId, allChallengeIds);
    Set<UUID> solvedSet = new HashSet<>(solvedIds);
    return solvedSet.containsAll(allChallengeIds);
  }

  private void awardBadge(UUID userId, Course course) {
    try {
      User user =
          userRepository
              .findByIdAndDeletedAtIsNull(userId)
              .orElseThrow(() -> new RuntimeException("User not found: " + userId));

      UserCourseBadge badge = new UserCourseBadge();
      badge.setUser(user);
      badge.setCourse(course);
      badge.setEarnedAt(LocalDateTime.now());
      userCourseBadgeRepository.saveAndFlush(badge);
      log.info("Awarded badge for course '{}' to user '{}'", course.getId(), userId);
    } catch (DataIntegrityViolationException ex) {
      log.debug(
          "Badge already exists (concurrent award) for user '{}' course '{}'",
          userId,
          course.getId());
    }
  }

  private void verifyOwner(Course course, UUID userId) {
    boolean isOwner =
        course.getCourseInstructors().stream()
            .anyMatch(
                ci ->
                    ci.getInstructor().getId().equals(userId)
                        && ci.getInstructorRole() == InstructorRoleEnum.OWNER);
    if (!isOwner) {
      throw new CourseAccessDeniedException(
          String.format(
              "User with ID '%s' is not the owner of course '%s'", userId, course.getId()));
    }
  }

  private CourseBadgeConfigDto toConfigDto(Course course) {
    return new CourseBadgeConfigDto(
        course.getId(),
        course.getTitle(),
        course.getBadgePrimaryColor() != null ? course.getBadgePrimaryColor() : DEFAULT_COLOR,
        course.getBadgeTextColor() != null ? course.getBadgeTextColor() : DEFAULT_TEXT_COLOR,
        course.getBadgeTemplate() != null ? course.getBadgeTemplate() : DEFAULT_TEMPLATE,
        course.getBadgeIcon() != null ? course.getBadgeIcon() : DEFAULT_ICON,
        course.isBadgeEnabled());
  }

  private UserBadgeDto toBadgeDto(UserCourseBadge b) {
    Course course = b.getCourse();
    return new UserBadgeDto(
        b.getId(),
        course.getId(),
        course.getTitle(),
        course.getBadgePrimaryColor() != null ? course.getBadgePrimaryColor() : DEFAULT_COLOR,
        course.getBadgeTextColor() != null ? course.getBadgeTextColor() : DEFAULT_TEXT_COLOR,
        course.getBadgeTemplate() != null ? course.getBadgeTemplate() : DEFAULT_TEMPLATE,
        course.getBadgeIcon() != null ? course.getBadgeIcon() : DEFAULT_ICON,
        b.getEarnedAt());
  }
}
