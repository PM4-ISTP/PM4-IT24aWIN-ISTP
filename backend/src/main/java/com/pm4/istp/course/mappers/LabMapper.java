package com.pm4.istp.course.mappers;

import com.pm4.istp.course.db.CreateLabRequest;
import com.pm4.istp.course.db.ChallengeOptionRequest;
import com.pm4.istp.course.db.ChallengeRequest;
import com.pm4.istp.course.db.UpdateLabRequest;
import com.pm4.istp.course.db.entities.Lab;
import com.pm4.istp.course.db.entities.CourseLab;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.ChallengeOption;
import com.pm4.istp.course.dto.ChallengeCreatorResponseDto;
import com.pm4.istp.course.dto.ChallengeDetailResponseDto;
import com.pm4.istp.course.dto.LabStudentDto;
import com.pm4.istp.course.dto.CourseLabResponseDto;
import com.pm4.istp.course.dto.CreateChallengeRequestDto;
import com.pm4.istp.course.dto.CreateChallengeResponseDto;
import com.pm4.istp.course.dto.ChallengeOptionRequestDto;
import com.pm4.istp.course.dto.ChallengeOptionResponseDto;
import com.pm4.istp.course.dto.ChallengeOptionStudentDto;
import com.pm4.istp.course.dto.ChallengeRequestDto;
import com.pm4.istp.course.dto.ChallengeResponseDto;
import com.pm4.istp.course.dto.ChallengeStudentDto;
import com.pm4.istp.course.dto.UpdateChallengeRequestDto;
import com.pm4.istp.user.db.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LabMapper {

  CreateLabRequest fromDto(CreateChallengeRequestDto dto);

  UpdateLabRequest fromDto(UpdateChallengeRequestDto dto);

  ChallengeRequest fromDto(ChallengeRequestDto dto);

  ChallengeOptionRequest fromDto(ChallengeOptionRequestDto dto);

  @Mapping(target = "creatorId", source = "creator.id")
  CreateChallengeResponseDto toCreateResponseDto(Lab lab);

  ChallengeDetailResponseDto toDetailResponseDto(Lab lab);

  ChallengeCreatorResponseDto toCreatorDto(User user);

  ChallengeResponseDto toChallengeResponseDto(Challenge challenge);

  ChallengeOptionStudentDto toOptionStudentDto(ChallengeOption option);

  ChallengeOptionResponseDto toOptionResponseDto(ChallengeOption option);

  @Mapping(target = "solved", ignore = true)
  @Mapping(target = "solvedFlag", ignore = true)
  @Mapping(target = "selectedOptionId", ignore = true)
  @Mapping(target = "options", source = "options")
  @Mapping(
      target = "theory",
      expression = "java(challenge.getFlag() == null || challenge.getFlag().isBlank())")
  ChallengeStudentDto toChallengeStudentDto(Challenge challenge);

  @Mapping(target = "challenges", source = "challenges")
  @Mapping(target = "solved", ignore = true)
  @Mapping(target = "solvedChallengeCount", ignore = true)
  @Mapping(target = "totalChallengeCount", ignore = true)
  LabStudentDto toStudentDto(Lab lab);

  @Mapping(target = "labId", source = "lab.id")
  @Mapping(target = "labTitle", source = "lab.title")
  @Mapping(target = "difficulty", source = "lab.difficulty")
  CourseLabResponseDto toCourseLabResponseDto(CourseLab courseLab);
}
