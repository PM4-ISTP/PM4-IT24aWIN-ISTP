package com.pm4.istp.course.services.impl;

import com.pm4.istp.course.db.CreateCourseInstructorRequest;
import com.pm4.istp.course.db.CreateCourseRequest;
import com.pm4.istp.course.db.InstructorRoleEnum;
import com.pm4.istp.course.db.UpdateCourseInstructorRequest;
import com.pm4.istp.course.db.UpdateCourseRequest;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.db.entities.CourseChallenge;
import com.pm4.istp.course.db.entities.CourseEnrollment;
import com.pm4.istp.course.db.entities.CourseInstructor;
import com.pm4.istp.course.db.entities.McAttemptsMode;
import com.pm4.istp.course.dto.CourseChallengeDeadlineDto;
import com.pm4.istp.course.dto.CourseChallengeItemDto;
import com.pm4.istp.course.dto.CourseChallengeResponseDto;
import com.pm4.istp.course.dto.CourseChallengeSubmissionEntryDto;
import com.pm4.istp.course.dto.CourseChallengeSubmissionStatusEnum;
import com.pm4.istp.course.dto.CourseChallengeSubmissionsResponseDto;
import com.pm4.istp.course.dto.CourseParticipantResponseDto;
import com.pm4.istp.course.dto.ListCourseResponseDto;
import com.pm4.istp.course.exceptions.ChallengeNotFoundException;
import com.pm4.istp.course.exceptions.CourseAccessDeniedException;
import com.pm4.istp.course.exceptions.CourseNotFoundException;
import com.pm4.istp.course.exceptions.CourseParticipantNotFoundException;
import com.pm4.istp.course.exceptions.InvalidCourseChallengeException;
import com.pm4.istp.course.exceptions.InvalidCourseShortDescriptionException;
import com.pm4.istp.course.exceptions.InvalidInviteCodeException;
import com.pm4.istp.course.repositories.ChallengeRepository;
import com.pm4.istp.course.repositories.CourseChallengeRepository;
import com.pm4.istp.course.repositories.CourseEnrollmentRepository;
import com.pm4.istp.course.repositories.CourseRepository;
import com.pm4.istp.course.repositories.SubTaskCompletionRepository;
import com.pm4.istp.course.repositories.SubTaskRepository;
import com.pm4.istp.course.services.CourseInviteCodeHelper;
import com.pm4.istp.course.services.CourseService;
import com.pm4.istp.course.services.CourseTopicService;
import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.exceptions.UserNotFoundException;
import com.pm4.istp.user.repositories.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  private static final int SHORT_DESCRIPTION_MAX_CHARS = 200;

  private static final String USER_NOT_FOUND_MSG = "User with ID '%s' not found";
  private static final String COURSE_NOT_FOUND_MSG = "Course with ID '%s' not found";

  private final UserRepository userRepository;
  private final CourseRepository courseRepository;
  private final CourseEnrollmentRepository courseEnrollmentRepository;
  private final CourseChallengeRepository courseChallengeRepository;
  private final ChallengeRepository challengeRepository;
  private final SubTaskRepository subTaskRepository;
  private final SubTaskCompletionRepository subTaskCompletionRepository;
  private final CourseInviteCodeHelper courseInviteCodeHelper;
  private final CourseTopicService courseTopicService;

  @Override
  @Transactional
  public Course createCourse(UUID userId, CreateCourseRequest course) {
    User instructorUser =
        userRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(
                () -> new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId)));

    Course courseToCreate = new Course();
    courseToCreate.setTitle(course.getTitle());
    courseToCreate.setDescription(course.getDescription());
    courseToCreate.setShortDescription(normalizeShortDescription(course.getShortDescription()));
    courseToCreate.setPublished(course.isPublished());
    courseToCreate.setPrivate(course.isPrivate());
    validateVisibilityState(course.isPublished(), course.isPrivate());
    courseToCreate.setImageUrl(course.getImageUrl());
    courseToCreate.setTopic(courseTopicService.normalizeAndValidate(course.getTopic()));
    courseToCreate.setMcAttemptsMode(
        course.getMcAttemptsMode() != null ? course.getMcAttemptsMode() : McAttemptsMode.UNLIMITED);

    // Owner = the user making the request
    CourseInstructor owner = new CourseInstructor();
    owner.setInstructorRole(InstructorRoleEnum.OWNER);
    owner.setAccepted(true);
    owner.setInstructor(instructorUser);
    owner.setAcceptedAt(LocalDateTime.now());
    courseToCreate.addCourseInstructor(owner);

    addEnrollmentIfMissing(courseToCreate, instructorUser);

    // Collaborators from the request payload
    if (!course.getInstructors().isEmpty()) {
      for (CreateCourseInstructorRequest req : course.getInstructors()) {
        User collaboratorUser =
            userRepository
                .findByIdAndDeletedAtIsNull(req.getInstructorId())
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

    if (course.isPrivate()) {
      return courseInviteCodeHelper.saveNewCourseWithInviteCode(courseToCreate);
    }
    return courseRepository.save(courseToCreate);
  }

  private void addEnrollmentIfMissing(Course course, User participant) {
    boolean alreadyEnrolled =
        course.getCourseEnrollments().stream()
            .anyMatch(
                enrollment ->
                    enrollment.getParticipant() != null
                        && participant.getId().equals(enrollment.getParticipant().getId()));

    if (alreadyEnrolled) {
      return;
    }

    CourseEnrollment courseEnrollment = new CourseEnrollment();
    courseEnrollment.setParticipant(participant);
    course.addCourseEnrollment(courseEnrollment);
  }

  @Override
  @Transactional(readOnly = true)
  public Course getCourse(UUID userId, UUID courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));

    if (course.isPrivate()) {
      boolean hasPrivateAccess =
          isInstructor(course, userId)
              || courseEnrollmentRepository.existsByCourseIdAndParticipantId(
                  course.getId(), userId);
      if (!hasPrivateAccess) {
        throw new CourseAccessDeniedException(
            String.format("Course '%s' is private and can only be accessed via invite", courseId));
      }
      return course;
    }

    if (course.isPublished()) {
      return course;
    }

    verifyInstructor(course, userId);
    return course;
  }

  @Override
  @Transactional(noRollbackFor = DataIntegrityViolationException.class)
  public Course enrollInCourse(UUID userId, UUID courseId) {
    User participant =
        userRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(
                () -> new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, userId)));

    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));

    // Private courses are accessible without invite code once the user has the course link.
    // The catalog/discovery protection is enforced by getCourse (403 for non-enrolled,
    // non-instructors). Draft courses (not published, not private) remain closed.
    if (!course.isPublished() && !course.isPrivate()) {
      throw new CourseAccessDeniedException(
          String.format("Course '%s' is not open for enrollment", courseId));
    }

    if (courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId)) {
      return course;
    }

    CourseEnrollment courseEnrollment = new CourseEnrollment();
    courseEnrollment.setParticipant(participant);
    course.addCourseEnrollment(courseEnrollment);

    try {
      return courseRepository.save(course);
    } catch (DataIntegrityViolationException ex) {
      // Concurrent enrollment: another request already enrolled this user; treat as
      // already
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
    course.setTopic(courseTopicService.normalizeAndValidate(request.getTopic()));

    boolean wasPrivate = course.isPrivate();
    boolean willBePublished = request.isPublished();
    boolean willBePrivate = request.isPrivate();
    validateVisibilityState(willBePublished, willBePrivate);
    boolean needsNewCode = willBePrivate && (!wasPrivate || course.getInviteCode() == null);
    if (!willBePrivate) {
      course.setInviteCode(null);
    }
    course.setPublished(willBePublished);
    course.setPrivate(willBePrivate);
    course.setMcAttemptsMode(
        request.getMcAttemptsMode() != null
            ? request.getMcAttemptsMode()
            : McAttemptsMode.UNLIMITED);

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
                .findByIdAndDeletedAtIsNull(req.getInstructorId())
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

    Course saved = courseRepository.save(course);
    if (needsNewCode) {
      saved.setInviteCode(courseInviteCodeHelper.generateAndAssign(saved.getId()));
    }
    return saved;
  }

  @Override
  @Transactional
  public Course updateCourseChallenges(
      UUID userId, UUID courseId, List<CourseChallengeItemDto> challenges) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));
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
      courseChallenge.setDueAt(item.getDueAt());
      course.addCourseChallenge(courseChallenge);
    }

    return courseRepository.save(course);
  }

  @Override
  @Transactional(readOnly = true)
  public CourseChallengeSubmissionsResponseDto getCourseChallengeSubmissions(
      UUID userId, UUID courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));
    verifyInstructor(course, userId);

    List<CourseParticipantResponseDto> participants = loadParticipants(courseId);
    List<CourseChallenge> assigned =
        course.getCourseChallenges() == null ? List.of() : course.getCourseChallenges();
    List<CourseChallengeResponseDto> challengesDto = toChallengeSubmissionDtos(assigned);

    List<UUID> userIds = participants.stream().map(CourseParticipantResponseDto::getId).toList();
    List<UUID> challengeIds = assigned.stream().map(cc -> cc.getChallenge().getId()).toList();

    Map<UUID, Integer> totalByChallenge = loadSubTaskTotals(challengeIds);

    record Key(UUID userId, UUID challengeId) {}
    Map<Key, Integer> solvedCountByKey = new HashMap<>();
    Map<Key, LocalDateTime> completedAtByKey = new HashMap<>();
    if (!userIds.isEmpty() && !challengeIds.isEmpty()) {
      for (Object[] row :
          subTaskCompletionRepository.aggregateSolvedCountsForUsersAndChallenges(
              userIds, challengeIds)) {
        UUID u = (UUID) row[0];
        UUID c = (UUID) row[1];
        Long solved = (Long) row[2];
        LocalDateTime completedAt = (LocalDateTime) row[3];
        Key key = new Key(u, c);
        solvedCountByKey.put(key, solved == null ? 0 : solved.intValue());
        completedAtByKey.put(key, completedAt);
      }
    }

    Map<UUID, LocalDateTime> dueAtByChallenge = new HashMap<>();
    for (CourseChallenge cc : assigned) {
      dueAtByChallenge.put(cc.getChallenge().getId(), cc.getDueAt());
    }

    List<CourseChallengeSubmissionEntryDto> entries = new ArrayList<>();
    for (UUID participantId : userIds) {
      for (UUID challengeId : challengeIds) {
        Key key = new Key(participantId, challengeId);
        int solvedCount = solvedCountByKey.getOrDefault(key, 0);
        int totalCount = totalByChallenge.getOrDefault(challengeId, 0);
        LocalDateTime completedAt =
            totalCount > 0 && solvedCount == totalCount ? completedAtByKey.get(key) : null;

        CourseChallengeSubmissionStatusEnum status =
            resolveSubmissionStatus(
                solvedCount, totalCount, completedAt, dueAtByChallenge.get(challengeId));

        entries.add(
            new CourseChallengeSubmissionEntryDto(
                participantId, challengeId, solvedCount, totalCount, completedAt, status));
      }
    }

    return new CourseChallengeSubmissionsResponseDto(
        courseId, participants, challengesDto, entries);
  }

  private List<CourseParticipantResponseDto> loadParticipants(UUID courseId) {
    return courseEnrollmentRepository.findByCourseIdFetchParticipant(courseId).stream()
        .map(
            enrollment -> {
              User participant = enrollment.getParticipant();
              return new CourseParticipantResponseDto(
                  participant.getId(), participant.getName(), participant.getPicture());
            })
        .toList();
  }

  private List<CourseChallengeResponseDto> toChallengeSubmissionDtos(
      List<CourseChallenge> assigned) {
    return assigned.stream()
        .map(
            courseChallenge -> {
              Challenge challenge = courseChallenge.getChallenge();
              return new CourseChallengeResponseDto(
                  challenge.getId(),
                  challenge.getTitle(),
                  challenge.getDifficulty(),
                  courseChallenge.getOrderIndex(),
                  courseChallenge.getDueAt());
            })
        .toList();
  }

  private Map<UUID, Integer> loadSubTaskTotals(List<UUID> challengeIds) {
    Map<UUID, Integer> totalByChallenge = new HashMap<>();
    if (challengeIds.isEmpty()) {
      return totalByChallenge;
    }
    for (Object[] row : subTaskRepository.countByChallengeIds(challengeIds)) {
      UUID challengeId = (UUID) row[0];
      Long count = (Long) row[1];
      totalByChallenge.put(challengeId, count == null ? 0 : count.intValue());
    }
    return totalByChallenge;
  }

  private CourseChallengeSubmissionStatusEnum resolveSubmissionStatus(
      int solvedCount, int totalCount, LocalDateTime completedAt, LocalDateTime dueAt) {
    if (totalCount <= 0 || solvedCount <= 0) {
      return CourseChallengeSubmissionStatusEnum.NOT_SUBMITTED;
    }
    if (solvedCount < totalCount) {
      return CourseChallengeSubmissionStatusEnum.IN_PROGRESS;
    }
    if (dueAt == null || completedAt == null || !completedAt.isAfter(dueAt)) {
      return CourseChallengeSubmissionStatusEnum.ON_TIME;
    }
    return CourseChallengeSubmissionStatusEnum.LATE;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseChallengeDeadlineDto> listUpcomingDeadlines(UUID userId) {
    List<Object[]> rows = courseChallengeRepository.findDeadlinesForUser(userId);
    List<CourseChallengeDeadlineDto> result = new ArrayList<>(rows.size());
    for (Object[] row : rows) {
      UUID courseId = (UUID) row[0];
      String courseTitle = (String) row[1];
      UUID challengeId = (UUID) row[2];
      String challengeTitle = (String) row[3];
      LocalDateTime dueAt = (LocalDateTime) row[4];
      if (courseId == null || challengeId == null || dueAt == null) {
        continue;
      }
      result.add(
          new CourseChallengeDeadlineDto(
              courseId, courseTitle, challengeId, challengeTitle, dueAt));
    }
    return result;
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
  public Page<ListCourseResponseDto> listUserEnrollments(UUID userId, Pageable pageable) {
    return courseRepository.findListEnrollmentsForUser(userId, pageable);
  }

  @Override
  public Page<ListCourseResponseDto> listPublishedCourses(
      String query, String topic, Pageable pageable) {
    String normalizedQuery = query == null || query.trim().isEmpty() ? null : query.trim();
    // topic is trimmed but intentionally not validated against the DB here.
    // CourseController always calls courseTopicService.normalizeAndValidate() before delegating to
    // this method, so invalid topics are rejected at the API layer. When this method is called
    // directly (e.g. in tests or future services) an unrecognised topic simply returns an empty
    // page rather than throwing – an acceptable silent-ignore trade-off documented here.
    String normalizedTopic = topic == null || topic.trim().isEmpty() ? null : topic.trim();

    if (normalizedTopic == null) {
      if (normalizedQuery == null) {
        return courseRepository.findPublishedCourses(pageable);
      }
      return courseRepository.findPublishedCoursesByQuery(normalizedQuery, pageable);
    }

    if (normalizedQuery == null) {
      return courseRepository.findPublishedCoursesByTopic(normalizedTopic, pageable);
    }
    return courseRepository.findPublishedCoursesByQueryAndTopic(
        normalizedQuery, normalizedTopic, pageable);
  }

  @Override
  @Transactional(noRollbackFor = DataIntegrityViolationException.class)
  public Course joinByInviteCode(String code, UUID studentId) {
    User participant =
        userRepository
            .findByIdAndDeletedAtIsNull(studentId)
            .orElseThrow(
                () -> new UserNotFoundException(String.format(USER_NOT_FOUND_MSG, studentId)));

    Course course =
        courseRepository
            .findByInviteCode(code)
            .orElseThrow(() -> new InvalidInviteCodeException("Invalid invite code"));

    if (!course.isPrivate()) {
      throw new InvalidInviteCodeException("Invalid invite code");
    }

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
      // Concurrent enrollment: another request already enrolled this user; treat as
      // already
      // enrolled
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

    if (!course.isPrivate()) {
      throw new CourseAccessDeniedException(
          String.format(
              "Course '%s' is not private; invite code regeneration is disabled", courseId));
    }

    course.setInviteCode(courseInviteCodeHelper.generateAndAssign(courseId));
    return course;
  }

  @Override
  @Transactional
  public void removeParticipant(UUID ownerId, UUID courseId, UUID participantId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));

    verifyOwner(course, ownerId);

    CourseEnrollment enrollment =
        courseEnrollmentRepository
            .findByCourseIdAndParticipantId(courseId, participantId)
            .orElseThrow(
                () ->
                    new CourseParticipantNotFoundException(
                        String.format(
                            "Participant with ID '%s' is not enrolled in course '%s'",
                            participantId, courseId)));

    course.removeCourseEnrollment(enrollment);
    courseRepository.save(course);
  }

  @Override
  @Transactional
  public void leaveCourse(UUID userId, UUID courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));

    CourseEnrollment enrollment =
        courseEnrollmentRepository
            .findByCourseIdAndParticipantId(courseId, userId)
            .orElseThrow(
                () ->
                    new CourseParticipantNotFoundException(
                        String.format(
                            "User '%s' is not enrolled in course '%s'", userId, courseId)));

    course.removeCourseEnrollment(enrollment);
    courseRepository.save(course);
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

  private void validateVisibilityState(boolean published, boolean privateCourse) {
    if (published && privateCourse) {
      throw new IllegalArgumentException("Course cannot be published and private at the same time");
    }
  }
}
