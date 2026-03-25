package com.pm4.istp.service.impl;

import com.pm4.istp.domain.CreateCourseInstructorRequest;
import com.pm4.istp.domain.CreateCourseRequest;
import com.pm4.istp.domain.entites.Course;
import com.pm4.istp.domain.entites.CourseInstructor;
import com.pm4.istp.domain.entites.User;
import com.pm4.istp.exception.UserNotFoundException;
import com.pm4.istp.repositories.CourseInstructorRepository;
import com.pm4.istp.repositories.CourseRepository;
import com.pm4.istp.repositories.UserRepository;
import com.pm4.istp.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseInstructorRepository courseInstructorRepository;

    @Override
    @Transactional
    public Course createCourse(CreateCourseRequest course) {
        CreateCourseInstructorRequest instructor = course.getInstructor();
        UUID instructorId = instructor.getInstructorId();
        User instructorUser = userRepository.findById(instructorId)
                .orElseThrow(() -> new UserNotFoundException(
                        String.format("User with ID '%s' not found", instructorId))
                );

        CourseInstructor courseInstructor = new CourseInstructor();
        courseInstructor.setInstructorRole(instructor.getInstructorRole());
        courseInstructor.setAccepted(true);
        courseInstructor.setInstructor(instructorUser);
        courseInstructor.setAcceptedAt(LocalDateTime.now());

        Course courseToCreate = new Course();
        courseToCreate.setTitle(course.getTitle());
        courseToCreate.setDescription(course.getDescription());
        courseToCreate.setPublished(course.isPublished());

        // Wire both sides of the relationship
        courseInstructor.setCourse(courseToCreate);
        courseToCreate.setCourseInstructors(List.of(courseInstructor));

        // Single save — cascade handles CourseInstructor
        return courseRepository.save(courseToCreate);
    }
}
