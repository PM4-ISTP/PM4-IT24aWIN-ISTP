package com.pm4.istp.mappers;

import com.pm4.istp.domain.CreateCourseInstructorRequest;
import com.pm4.istp.domain.CreateCourseRequest;
import com.pm4.istp.domain.UpdateCourseInstructorRequest;
import com.pm4.istp.domain.UpdateCourseRequest;
import com.pm4.istp.domain.entites.Course;
import com.pm4.istp.domain.entites.CourseChallenge;
import com.pm4.istp.domain.entites.CourseInstructor;
import com.pm4.istp.dto.*;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {ChallengeMapper.class})
public interface CourseMapper {

  CreateCourseInstructorRequest fromDto(CreateCourseInstructorRequestDto dto);

  CreateCourseRequest fromDto(CreateCourseRequestDto dto);

  CreateCourseResponseDto toDto(Course course);

  UpdateCourseInstructorRequest fromDto(UpdateCourseInstructorRequestDto dto);

  UpdateCourseRequest fromDto(UpdateCourseRequestDto dto);

  CourseDetailResponseDto toCourseDetailDto(Course course);

  PublicCourseDetailResponseDto toPublicCourseDetailDto(Course course);

  @Mapping(target = ".", source = "challenge")
  ChallengeDetailResponseDto toChallengeDetailResponseDto(CourseChallenge courseChallenge);

  @Mapping(
      target = "instructorCount",
      source = "courseInstructors",
      qualifiedByName = "countInstructors")
  ListCourseResponseDto toListCourseResponseDto(Course course);

  @Named("countInstructors")
  default int mapInstructorCount(List<CourseInstructor> instructors) {
    return instructors == null ? 0 : instructors.size();
  }
}
