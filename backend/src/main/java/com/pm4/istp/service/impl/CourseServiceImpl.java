package com.pm4.istp.service.impl;

import com.pm4.istp.domain.CreateCourseInstructorRequest;
import com.pm4.istp.domain.CreateCourseRequest;
import com.pm4.istp.domain.entites.Course;
import com.pm4.istp.domain.entites.CourseInstructor;
import com.pm4.istp.domain.entites.InstructorRoleEnum;
import com.pm4.istp.domain.entites.User;
import com.pm4.istp.exception.UserNotFoundException;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

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
    public Page<Course> listCoursesForInstructors(UUID instructorId, Pageable pageable) {
        return courseRepository.findByCourseInstructors_Instructor_Id(instructorId, pageable);
    }
}
