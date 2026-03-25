package com.pm4.istp.mappers;

import com.pm4.istp.domain.CreateCourseInstructorRequest;
import com.pm4.istp.domain.CreateCourseRequest;
import com.pm4.istp.domain.entites.Course;
import com.pm4.istp.dto.CreateCourseInstructorRequestDto;
import com.pm4.istp.dto.CreateCourseRequestDto;
import com.pm4.istp.dto.CreateCourseResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CourseMapper {

    CreateCourseInstructorRequest fromDto(CreateCourseInstructorRequestDto dto);

    CreateCourseRequest fromDto(CreateCourseRequestDto dto);

    CreateCourseResponseDto toDto(Course course);
}
