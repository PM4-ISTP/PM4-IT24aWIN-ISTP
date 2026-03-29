package com.pm4.istp.service.impl;

import com.pm4.istp.domain.CreateCourseInstructorRequest;
import com.pm4.istp.domain.CreateCourseRequest;
import com.pm4.istp.domain.UpdateCourseInstructorRequest;
import com.pm4.istp.domain.UpdateCourseRequest;
import com.pm4.istp.domain.entites.Course;
import com.pm4.istp.domain.entites.CourseInstructor;
import com.pm4.istp.domain.entites.InstructorRoleEnum;
import com.pm4.istp.domain.entites.User;
import com.pm4.istp.exception.CourseAccessDeniedException;
import com.pm4.istp.exception.CourseNotFoundException;
import com.pm4.istp.exception.UserNotFoundException;
import com.pm4.istp.repositories.CourseInstructorRepository;
import com.pm4.istp.repositories.CourseRepository;
import com.pm4.istp.repositories.UserRepository;
import com.pm4.istp.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;

    @Override
    @Transactional
    public Course createCourse(UUID userId, CreateCourseRequest course) {
        User instructorUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                        String.format("User with ID '%s' not found", userId))
                );

        Course courseToCreate = new Course();
        courseToCreate.setTitle(course.getTitle());
        courseToCreate.setDescription(course.getDescription());
        courseToCreate.setPublished(course.isPublished());

        List<CourseInstructor> courseInstructors = new ArrayList<>();

        // Owner = the user making the request
        CourseInstructor owner = new CourseInstructor();
        owner.setInstructorRole(InstructorRoleEnum.OWNER);
        owner.setAccepted(true);
        owner.setInstructor(instructorUser);
        owner.setAcceptedAt(LocalDateTime.now());
        owner.setCourse(courseToCreate);
        courseInstructors.add(owner);

        // Collaborators from the request payload
        if (!course.getInstructors().isEmpty()) {
            for (CreateCourseInstructorRequest req : course.getInstructors()) {
                User collaboratorUser = userRepository.findById(req.getInstructorId())
                        .orElseThrow(() -> new UserNotFoundException(
                                String.format("User with ID '%s' not found", req.getInstructorId()))
                        );

                CourseInstructor collaborator = new CourseInstructor();
                collaborator.setInstructorRole(req.getInstructorRole());
                collaborator.setAccepted(false);
                collaborator.setInstructor(collaboratorUser);
                collaborator.setCourse(courseToCreate);
                courseInstructors.add(collaborator);
            }
        }

        courseToCreate.setCourseInstructors(courseInstructors);

        return courseRepository.save(courseToCreate);
    }

    @Override
    @Transactional(readOnly = true)
    public Course getCourse(UUID userId, UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(
                        String.format("Course with ID '%s' not found", courseId))
                );
        verifyInstructor(course, userId);
        return course;
    }

    @Override
    @Transactional
    public Course updateCourse(UUID userId, UUID courseId, UpdateCourseRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(
                        String.format("Course with ID '%s' not found", courseId))
                );
        verifyInstructor(course, userId);

        // Update scalar fields
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setPublished(request.isPublished());

        // Diff instructor list: preserve OWNER, update COLLABORATORs
        Set<UUID> requestedInstructorIds = request.getInstructors().stream()
                .map(UpdateCourseInstructorRequest::getInstructorId)
                .collect(Collectors.toSet());

        // Remove collaborators not in the new list
        List<CourseInstructor> toRemove = course.getCourseInstructors().stream()
                .filter(ci -> ci.getInstructorRole() == InstructorRoleEnum.COLLABORATOR)
                .filter(ci -> !requestedInstructorIds.contains(ci.getInstructor().getId()))
                .toList();

        course.getCourseInstructors().removeAll(toRemove);
        courseInstructorRepository.deleteAll(toRemove);

        // Find existing instructor IDs (remaining after removal)
        Set<UUID> existingInstructorIds = course.getCourseInstructors().stream()
                .map(ci -> ci.getInstructor().getId())
                .collect(Collectors.toSet());

        // Add new collaborators
        for (UpdateCourseInstructorRequest req : request.getInstructors()) {
            if (!existingInstructorIds.contains(req.getInstructorId())) {
                User collaboratorUser = userRepository.findById(req.getInstructorId())
                        .orElseThrow(() -> new UserNotFoundException(
                                String.format("User with ID '%s' not found", req.getInstructorId()))
                        );

                CourseInstructor collaborator = new CourseInstructor();
                collaborator.setInstructorRole(req.getInstructorRole());
                collaborator.setAccepted(false);
                collaborator.setInstructor(collaboratorUser);
                collaborator.setCourse(course);
                course.getCourseInstructors().add(collaborator);
            }
        }

        return courseRepository.save(course);
    }

    @Override
    public Page<Course> listCoursesForInstructors(UUID instructorId, Pageable pageable) {
        return courseRepository.findByCourseInstructors_Instructor_Id(instructorId, pageable);
    }

    private void verifyInstructor(Course course, UUID userId) {
        boolean isInstructor = course.getCourseInstructors().stream()
                .anyMatch(ci -> ci.getInstructor().getId().equals(userId));
        if (!isInstructor) {
            throw new CourseAccessDeniedException(
                    String.format("User with ID '%s' is not an instructor of course '%s'", userId, course.getId())
            );
        }
    }
}
