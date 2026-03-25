package com.pm4.istp.service;

import com.pm4.istp.domain.CreateCourseRequest;
import com.pm4.istp.domain.entites.Course;

public interface CourseService {
    Course createCourse(CreateCourseRequest course);
}
