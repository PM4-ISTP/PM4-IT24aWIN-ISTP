package com.pm4.istp.service.impl;

import com.pm4.istp.domain.CreateCourseInstructorRequest;
import com.pm4.istp.domain.CreateCourseRequest;
import com.pm4.istp.domain.UpdateCourseInstructorRequest;
import com.pm4.istp.domain.UpdateCourseRequest;
import com.pm4.istp.domain.entites.*;
import com.pm4.istp.domain.entites.Course;
import com.pm4.istp.domain.entites.CourseEnrollment;
import com.pm4.istp.domain.entites.CourseInstructor;
import com.pm4.istp.domain.entites.InstructorRoleEnum;
import com.pm4.istp.domain.entites.User;
import com.pm4.istp.domain.entites.UserRoleEnum;
import com.pm4.istp.dto.CourseChallengeItemDto;
import com.pm4.istp.dto.ListCourseResponseDto;
import com.pm4.istp.exception.ChallengeNotFoundException;
import com.pm4.istp.exception.CourseAccessDeniedException;
import com.pm4.istp.exception.CourseNotFoundException;
import com.pm4.istp.exception.InvalidCourseChallengeException;
import com.pm4.istp.exception.InvalidCourseShortDescriptionException;
import com.pm4.istp.exception.InvalidInviteCodeException;
import com.pm4.istp.exception.UserNotFoundException;
import com.pm4.istp.repositories.ChallengeRepository;
import com.pm4.istp.repositories.CourseEnrollmentRepository;
import com.pm4.istp.repositories.CourseRepository;
import com.pm4.istp.repositories.UserRepository;
import com.pm4.istp.service.CourseService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {
  private static final Set<UserRoleEnum> COURSE_COLLABORATOR_ROLES =
      Set.of(UserRoleEnum.ROLE_ADMINISTRATOR, UserRoleEnum.ROLE_INSTRUCTOR);
  private static final int SHORT_DESCRIPTION_MAX_CHARS = 200;

  private static final String USER_NOT_FOUND_MSG = "User with ID '%s' not found";
  private static final String COURSE_NOT_FOUND_MSG = "Course with ID '%s' not found";

  private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final int INVITE_CODE_LENGTH = 6;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final UserRepository userRepository;
  private final CourseRepository courseRepository;
  private final CourseEnrollmentRepository courseEnrollmentRepository;
  private final ChallengeRepository challengeRepository;

  @Override
  @Transactional
  public Course createCourse(UUID userId, CreateCourseRequest course) {
    User instructorUser =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId)));

    Course courseToCreate = new Course();
    courseToCreate.setTitle(course.getTitle());
    courseToCreate.setDescription(course.getDescription());
    courseToCreate.setShortDescription(normalizeShortDescription(course.getShortDescription()));
    courseToCreate.setPublished(course.isPublished());
    courseToCreate.setPrivate(course.isPrivate());
    courseToCreate.setImageUrl(course.getImageUrl());
    courseToCreate.setTopic(course.getTopic());
    if (course.isPublished() && course.isPrivate()) {
      courseToCreate.setInviteCode(generateUniqueInviteCode());
    }

    // Owner = the user making the request
    CourseInstructor owner = new CourseInstructor();
    owner.setInstructorRole(InstructorRoleEnum.OWNER);
    owner.setAccepted(true);
    owner.setInstructor(instructorUser);
    owner.setAcceptedAt(LocalDateTime.now());
    courseToCreate.addCourseInstructor(owner);

    // Collaborators from the request payload
    if (!course.getInstructors().isEmpty()) {
      for (CreateCourseInstructorRequest req : course.getInstructors()) {
        User collaboratorUser =
            userRepository
                .findById(req.getInstructorId())
                .orElseThrow(
                    () ->
                        new UserNotFoundException(
                            String.format(USER_NOT_FOUND_MSG, req.getInstructorId())));

        CourseInstructor collaborator = new CourseInstructor();
        collaborator.setInstructorRole(InstructorRoleEnum.COLLABORATOR);
        collaborator.setAccepted(false);
        collaborator.setInstructor(collaboratorUser);
        courseToCreate.addCourseInstructor(collaborator);
      }
    }

    return courseRepository.save(courseToCreate);
  }

  @Override
  @Transactional(readOnly = true)
  public Course getCourse(UUID userId, UUID courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));

    if (!course.isPublished()) {
      verifyInstructor(course, userId);
      return course;
    }

    if (course.isPrivate()) {
      boolean hasPrivateAccess =
          isInstructor(course, userId)
              || courseEnrollmentRepository.existsByCourseIdAndParticipantId(course.getId(), userId);
      if (!hasPrivateAccess) {
        throw new CourseAccessDeniedException(
            String.format("Course '%s' is private and can only be accessed via invite", courseId));
      }
    }

    return course;
  }

  @Override
  @Transactional(noRollbackFor = DataIntegrityViolationException.class)
  public Course enrollInCourse(UUID userId, UUID courseId) {
    User participant =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId)));

    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));

    if (!course.isPublished()) {
      throw new CourseAccessDeniedException(
          String.format("Course '%s' is not open for enrollment", courseId));
    }

    if (course.isPrivate()) {
      throw new CourseAccessDeniedException(
          String.format("Course '%s' is private and can only be joined via invite code", courseId));
    }

    if (isInstructor(course, userId)
        || courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId)) {
      return course;
    }

    CourseEnrollment courseEnrollment = new CourseEnrollment();
    courseEnrollment.setParticipant(participant);
    course.addCourseEnrollment(courseEnrollment);

    try {
      return courseRepository.save(course);
    } catch (DataIntegrityViolationException ex) {
      // Concurrent enrollment: another request already enrolled this user; treat as already
      // enrolled
      return course;
    }
  }

  @Override
  @Transactional
  public Course updateCourse(UUID userId, UUID courseId, UpdateCourseRequest request) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));
    verifyInstructor(course, userId);

    // Update scalar fields
    course.setTitle(request.getTitle());
    course.setDescription(request.getDescription());
    course.setShortDescription(normalizeShortDescription(request.getShortDescription()));
    course.setImageUrl(request.getImageUrl());
    course.setTopic(request.getTopic());

    boolean wasPublished = course.isPublished();
    boolean wasPrivate = course.isPrivate();
    boolean willBePublished = request.isPublished();
    boolean willBePrivate = request.isPrivate();
    if (willBePublished && willBePrivate && (!wasPublished || !wasPrivate)) {
      course.setInviteCode(generateUniqueInviteCode());
    } else if (!willBePublished || !willBePrivate) {
      course.setInviteCode(null);
    }
    course.setPublished(willBePublished);
    course.setPrivate(willBePrivate);

    // Diff instructor list: preserve OWNER, update COLLABORATORs
    Set<UUID> requestedInstructorIds =
        request.getInstructors().stream()
            .map(UpdateCourseInstructorRequest::getInstructorId)
            .collect(Collectors.toSet());

    // Remove collaborators not in the new list
    List<CourseInstructor> toRemove =
        course.getCourseInstructors().stream()
            .filter(ci -> ci.getInstructorRole() == InstructorRoleEnum.COLLABORATOR)
            .filter(ci -> !requestedInstructorIds.contains(ci.getInstructor().getId()))
            .toList();

    toRemove.forEach(course::removeCourseInstructor);

    // Find existing instructor IDs (remaining after removal)
    Set<UUID> existingInstructorIds =
        course.getCourseInstructors().stream()
            .map(ci -> ci.getInstructor().getId())
            .collect(Collectors.toSet());

    // Add new collaborators
    for (UpdateCourseInstructorRequest req : request.getInstructors()) {
      if (!existingInstructorIds.contains(req.getInstructorId())) {
        User collaboratorUser =
            userRepository
                .findById(req.getInstructorId())
                .orElseThrow(
                    () ->
                        new UserNotFoundException(
                            String.format(USER_NOT_FOUND_MSG, req.getInstructorId())));

        CourseInstructor collaborator = new CourseInstructor();
        collaborator.setInstructorRole(InstructorRoleEnum.COLLABORATOR);
        collaborator.setAccepted(false);
        collaborator.setInstructor(collaboratorUser);
        course.addCourseInstructor(collaborator);
      }
    }

    return courseRepository.save(course);
  }

  @Override
  @Transactional
  public Course updateCourseChallenges(
      UUID userId, UUID courseId, List<CourseChallengeItemDto> challenges) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () ->
                    new CourseNotFoundException(
                        String.format("Course with ID '%s' not found", courseId)));
    verifyInstructor(course, userId);

    // Clear existing challenge assignments
    course.getCourseChallenges().clear();

    // Add new challenge assignments
    for (CourseChallengeItemDto item : challenges) {
      Challenge challenge =
          challengeRepository
              .findById(item.getChallengeId())
              .orElseThrow(
                  () ->
                      new ChallengeNotFoundException(
                          String.format(
                              "Challenge with ID '%s' not found", item.getChallengeId())));

      // DRAFT challenges cannot be added to any course, even by their creator
      if (challenge.getStatus() == ChallengeStatusEnum.DRAFT) {
        throw new InvalidCourseChallengeException(
            String.format(
                "Challenge '%s' is a draft and cannot be added to a course", challenge.getTitle()));
      }

      // Only allow adding own PRIVATE challenges or PUBLIC challenges
      boolean isCreator = challenge.getCreator().getId().equals(userId);
      boolean isPublic = challenge.getStatus() == ChallengeStatusEnum.PUBLIC;
      if (!isCreator && !isPublic) {
        throw new ChallengeNotFoundException(
            String.format("Challenge with ID '%s' not found", item.getChallengeId()));
      }

      CourseChallenge courseChallenge = new CourseChallenge();
      courseChallenge.setChallenge(challenge);
      courseChallenge.setOrderIndex(item.getOrderIndex());
      course.addCourseChallenge(courseChallenge);
    }

    return courseRepository.save(course);
  }

  @Override
  @Transactional
  public void deleteCourse(UUID userId, UUID courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));
    verifyOwner(course, userId);
    courseRepository.delete(course);
  }

  @Override
  public Page<ListCourseResponseDto> listCoursesForInstructors(
      UUID instructorId, Pageable pageable) {
    return courseRepository.findListCoursesForInstructor(instructorId, pageable);
  }

  @Override
  public Page<ListCourseResponseDto> listPublishedCourses(String query, Pageable pageable) {
    String normalizedQuery = query == null || query.trim().isEmpty() ? null : query.trim();
    if (normalizedQuery == null) {
      return courseRepository.findPublishedCourses(pageable);
    }
    return courseRepository.findPublishedCoursesByQuery(normalizedQuery, pageable);
  }

  @Override
  @Transactional(noRollbackFor = DataIntegrityViolationException.class)
  public Course joinByInviteCode(String code, UUID studentId) {
    User participant =
        userRepository
            .findById(studentId)
            .orElseThrow(
                () -> new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, studentId)));

    Course course =
        courseRepository
            .findByInviteCode(code)
            .filter(Course::isPublished)
            .orElseThrow(() -> new InvalidInviteCodeException("Invalid invite code"));

    if (isInstructor(course, studentId)
        || courseEnrollmentRepository.existsByCourseIdAndParticipantId(course.getId(), studentId)) {
      return course;
    }

    CourseEnrollment courseEnrollment = new CourseEnrollment();
    courseEnrollment.setParticipant(participant);
    course.addCourseEnrollment(courseEnrollment);

    try {
      return courseRepository.save(course);
    } catch (DataIntegrityViolationException ex) {
      return course;
    }
  }

  @Override
  @Transactional
  public Course regenerateInviteCode(UUID courseId, UUID userId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));
    verifyOwner(course, userId);

    if (!course.isPublished()) {
      throw new CourseAccessDeniedException(
          String.format("Course '%s' is not published; cannot regenerate invite code", courseId));
    }

    if (!course.isPrivate()) {
      throw new CourseAccessDeniedException(
          String.format("Course '%s' is public; invite code regeneration is disabled", courseId));
    }

    course.setInviteCode(generateUniqueInviteCode());
    return courseRepository.save(course);
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

  private boolean isInstructor(Course course, UUID userId) {
    return course.getCourseInstructors().stream()
        .anyMatch(ci -> ci.getInstructor().getId().equals(userId));
  }

  private void verifyInstructor(Course course, UUID userId) {
    if (!isInstructor(course, userId)) {
      throw new CourseAccessDeniedException(
          String.format(
              "User with ID '%s' is not an instructor of course '%s'", userId, course.getId()));
    }
  }

  private String generateInviteCode() {
    StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
    for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
      sb.append(INVITE_CODE_CHARS.charAt(SECURE_RANDOM.nextInt(INVITE_CODE_CHARS.length())));
    }
    return sb.toString();
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

  private String normalizeShortDescription(String shortDescription) {
    if (shortDescription == null || shortDescription.isBlank()) {
      return null;
    }
    String normalized = shortDescription.trim().replaceAll("\\s+", " ");
    if (normalized.length() > SHORT_DESCRIPTION_MAX_CHARS) {
      throw new InvalidCourseShortDescriptionException(
          String.format(
              "Short description must be at most %d characters", SHORT_DESCRIPTION_MAX_CHARS));
    }
    return normalized;
  }
}
