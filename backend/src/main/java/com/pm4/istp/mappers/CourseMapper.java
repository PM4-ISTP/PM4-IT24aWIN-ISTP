package com.pm4.istp.mappers;

import com.pm4.istp.domain.CreateCourseInstructorRequest;
import com.pm4.istp.domain.CreateCourseRequest;
import com.pm4.istp.domain.entites.Course;
import com.pm4.istp.domain.entites.CourseInstructor;
import com.pm4.istp.dto.CreateCourseInstructorRequestDto;
import com.pm4.istp.dto.CreateCourseRequestDto;
import com.pm4.istp.dto.CreateCourseResponseDto;
import com.pm4.istp.dto.ListCourseResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CourseMapper {

    CreateCourseInstructorRequest fromDto(CreateCourseInstructorRequestDto dto);

    CreateCourseRequest fromDto(CreateCourseRequestDto dto);

    CreateCourseResponseDto toDto(Course course);

    @Mapping(target = "instructorCount", source = "courseInstructors", qualifiedByName = "countInstructors")
    ListCourseResponseDto toListCourseResponseDto(Course course);

    @Named("countInstructors")
    default int mapInstructorCount(List<CourseInstructor> instructors) {
        return instructors == null ? 0 : instructors.size();
    }
}
