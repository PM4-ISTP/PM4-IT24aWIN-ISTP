package com.pm4.istp.course.mappers;

import com.pm4.istp.course.db.CreateCourseInstructorRequest;
import com.pm4.istp.course.db.CreateCourseRequest;
import com.pm4.istp.course.db.UpdateCourseInstructorRequest;
import com.pm4.istp.course.db.UpdateCourseRequest;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.db.entities.CourseInstructor;
import com.pm4.istp.course.db.entities.CourseLab;
import com.pm4.istp.course.dto.ChallengeDetailResponseDto;
import com.pm4.istp.course.dto.CourseDetailResponseDto;
import com.pm4.istp.course.dto.CreateCourseInstructorRequestDto;
import com.pm4.istp.course.dto.CreateCourseRequestDto;
import com.pm4.istp.course.dto.CreateCourseResponseDto;
import com.pm4.istp.course.dto.LabStudentDto;
import com.pm4.istp.course.dto.ListCourseResponseDto;
import com.pm4.istp.course.dto.PublicCourseDetailResponseDto;
import com.pm4.istp.course.dto.UpdateCourseInstructorRequestDto;
import com.pm4.istp.course.dto.UpdateCourseRequestDto;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {LabMapper.class})
public interface CourseMapper {

  CreateCourseInstructorRequest fromDto(CreateCourseInstructorRequestDto dto);

  CreateCourseRequest fromDto(CreateCourseRequestDto dto);

  CreateCourseResponseDto toDto(Course course);

  UpdateCourseInstructorRequest fromDto(UpdateCourseInstructorRequestDto dto);

  UpdateCourseRequest fromDto(UpdateCourseRequestDto dto);

  CourseDetailResponseDto toCourseDetailDto(Course course);

  PublicCourseDetailResponseDto toPublicCourseDetailDto(Course course);

  @Mapping(target = ".", source = "lab")
  ChallengeDetailResponseDto toChallengeDetailResponseDto(CourseLab courseLab);

  @Mapping(target = ".", source = "lab")
  @Mapping(target = "solved", ignore = true)
  @Mapping(target = "solvedChallengeCount", ignore = true)
  @Mapping(target = "totalChallengeCount", ignore = true)
  @Mapping(target = "dueAt", source = "dueAt")
  LabStudentDto toChallengeStudentDto(CourseLab courseLab);

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
