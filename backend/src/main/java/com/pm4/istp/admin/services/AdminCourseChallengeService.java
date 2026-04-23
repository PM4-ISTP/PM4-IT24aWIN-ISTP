package com.pm4.istp.admin.services;

import com.pm4.istp.admin.dto.AdminChallengeListItemDto;
import com.pm4.istp.admin.dto.AdminCourseListItemDto;
import com.pm4.istp.admin.dto.AdminUpdateChallengeRequestDto;
import com.pm4.istp.admin.dto.AdminUpdateCourseRequestDto;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.ChallengeDifficultyEnum;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.exceptions.ChallengeNotFoundException;
import com.pm4.istp.course.exceptions.CourseNotFoundException;
import com.pm4.istp.course.exceptions.InviteCodeGenerationException;
import com.pm4.istp.course.repositories.ChallengeRepository;
import com.pm4.istp.course.repositories.CourseRepository;
import java.security.SecureRandom;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCourseChallengeService {
  private static final String COURSE_NOT_FOUND_MSG = "Course with ID '%s' not found";
  private static final String CHALLENGE_NOT_FOUND_MSG = "Challenge with ID '%s' not found";

  private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final int INVITE_CODE_LENGTH = 6;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final CourseRepository courseRepository;
  private final ChallengeRepository challengeRepository;

  @Transactional(readOnly = true)
  public Page<AdminCourseListItemDto> listCourses(String query, String owner, Pageable pageable) {
    return courseRepository.findAllCoursesForAdmin(normalizeQuery(query), normalizeQuery(owner), pageable);
  }

  public Course updateCourse(UUID courseId, AdminUpdateCourseRequestDto request) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));

    validateVisibilityState(request.isPublished(), request.isPrivate());

    course.setTitle(request.getTitle());
    course.setDescription(request.getDescription());
    course.setShortDescription(normalizeBlankToNull(request.getShortDescription()));
    course.setPublished(request.isPublished());
    course.setPrivate(request.isPrivate());
    course.setTopic(normalizeBlankToNull(request.getTopic()));
    course.setImageUrl(normalizeBlankToNull(request.getImageUrl()));

    // Ensure private courses have an invite code.
    if (request.isPrivate() && (course.getInviteCode() == null || course.getInviteCode().isBlank())) {
      try {
        course.setInviteCode(generateUniqueInviteCode());
      } catch (IllegalStateException ex) {
        throw new InviteCodeGenerationException("Failed to generate invite code", ex);
      }
    } else if (!request.isPrivate()) {
      course.setInviteCode(null);
    }

    return courseRepository.save(course);
  }

  public void deleteCourse(UUID courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));
    courseRepository.delete(course);
  }

  @Transactional(readOnly = true)
  public Page<AdminChallengeListItemDto> listChallenges(
      String query,
      String owner,
      ChallengeStatusEnum status,
      ChallengeDifficultyEnum difficulty,
      Pageable pageable) {
    return challengeRepository.findAllChallengesForAdmin(
        normalizeQuery(query), normalizeQuery(owner), status, difficulty, pageable);
  }

  public Challenge updateChallenge(UUID challengeId, AdminUpdateChallengeRequestDto request) {
    Challenge challenge =
        challengeRepository
            .findById(challengeId)
            .orElseThrow(
                () ->
                    new ChallengeNotFoundException(
                        String.format(CHALLENGE_NOT_FOUND_MSG, challengeId)));

    challenge.setTitle(request.getTitle());
    challenge.setShortDescription(normalizeBlankToNull(request.getShortDescription()));
    challenge.setDescription(request.getDescription());
    challenge.setStatus(request.getStatus());
    challenge.setDifficulty(request.getDifficulty());
    if (request.getMaxScore() != null) {
      challenge.setMaxScore(request.getMaxScore());
    }

    return challengeRepository.save(challenge);
  }

  public void deleteChallenge(UUID challengeId) {
    Challenge challenge =
        challengeRepository
            .findById(challengeId)
            .orElseThrow(
                () ->
                    new ChallengeNotFoundException(
                        String.format(CHALLENGE_NOT_FOUND_MSG, challengeId)));
    challengeRepository.delete(challenge);
  }

  private String normalizeQuery(String value) {
    String trimmed = normalizeBlankToNull(value);
    return trimmed == null ? null : trimmed.trim();
  }

  private String normalizeBlankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private void validateVisibilityState(boolean published, boolean privateCourse) {
    if (published && privateCourse) {
      throw new IllegalArgumentException("Course cannot be published and private at the same time");
    }
  }

  private String generateUniqueInviteCode() {
    for (int attempt = 0; attempt < 10; attempt++) {
      String code = generateInviteCode();
      if (!courseRepository.existsByInviteCode(code)) {
        return code;
      }
    }
    throw new IllegalStateException("Could not generate a unique invite code after 10 attempts");
  }

  private String generateInviteCode() {
    StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
    for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
      int idx = SECURE_RANDOM.nextInt(INVITE_CODE_CHARS.length());
      sb.append(INVITE_CODE_CHARS.charAt(idx));
    }
    return sb.toString();
  }
}

