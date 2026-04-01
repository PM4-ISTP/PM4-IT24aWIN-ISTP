package com.pm4.istp.service.impl;

import com.pm4.istp.domain.CreateCourseInstructorRequest;
import com.pm4.istp.domain.CreateCourseRequest;
import com.pm4.istp.domain.UpdateCourseInstructorRequest;
import com.pm4.istp.domain.UpdateCourseRequest;
import com.pm4.istp.domain.entites.Course;
import com.pm4.istp.domain.entites.CourseInstructor;
import com.pm4.istp.domain.entites.InstructorRoleEnum;
import com.pm4.istp.domain.entites.User;
import com.pm4.istp.domain.entites.UserRoleEnum;
import com.pm4.istp.dto.ListCourseResponseDto;
import com.pm4.istp.exception.CourseAccessDeniedException;
import com.pm4.istp.exception.CourseNotFoundException;
import com.pm4.istp.exception.InvalidCourseCollaboratorException;
import com.pm4.istp.exception.UserNotFoundException;
import com.pm4.istp.repositories.CourseRepository;
import com.pm4.istp.repositories.UserRepository;
import com.pm4.istp.service.CourseService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {
  private static final Set<UserRoleEnum> COURSE_COLLABORATOR_ROLES =
      Set.of(UserRoleEnum.ROLE_ADMINISTRATOR, UserRoleEnum.ROLE_INSTRUCTOR);

  private final UserRepository userRepository;
  private final CourseRepository courseRepository;

  @Override
  @Transactional
  public Course createCourse(UUID userId, CreateCourseRequest course) {
    User instructorUser =
        userRepository
            .findById(userId)
            .orElseThrow(
                () ->
                    new UserNotFoundException(
                        String.format("User with ID '%s' not found", userId)));

    Course courseToCreate = new Course();
    courseToCreate.setTitle(course.getTitle());
    courseToCreate.setDescription(course.getDescription());
    courseToCreate.setPublished(course.isPublished());

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
                            String.format("User with ID '%s' not found", req.getInstructorId())));
        validateCollaboratorUser(collaboratorUser);

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
                () ->
                    new CourseNotFoundException(
                        String.format("Course with ID '%s' not found", courseId)));
    verifyInstructor(course, userId);
    return course;
  }

  @Override
  @Transactional
  public Course updateCourse(UUID userId, UUID courseId, UpdateCourseRequest request) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () ->
                    new CourseNotFoundException(
                        String.format("Course with ID '%s' not found", courseId)));
    verifyInstructor(course, userId);

    // Update scalar fields
    course.setTitle(request.getTitle());
    course.setDescription(request.getDescription());
    course.setPublished(request.isPublished());

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
                            String.format("User with ID '%s' not found", req.getInstructorId())));
        validateCollaboratorUser(collaboratorUser);

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
  public Page<ListCourseResponseDto> listCoursesForInstructors(
      UUID instructorId, Pageable pageable) {
    return courseRepository.findListCoursesForInstructor(instructorId, pageable);
  }

  private void verifyInstructor(Course course, UUID userId) {
    boolean isInstructor =
        course.getCourseInstructors().stream()
            .anyMatch(ci -> ci.getInstructor().getId().equals(userId));
    if (!isInstructor) {
      throw new CourseAccessDeniedException(
          String.format(
              "User with ID '%s' is not an instructor of course '%s'", userId, course.getId()));
    }
  }

  private void validateCollaboratorUser(User collaboratorUser) {
    boolean isEligibleCollaborator =
        collaboratorUser.getRoles().stream().anyMatch(COURSE_COLLABORATOR_ROLES::contains);
    if (!isEligibleCollaborator) {
      throw new InvalidCourseCollaboratorException(
          "Only admins or instructors can be added as collaborators");
    }
  }
}
