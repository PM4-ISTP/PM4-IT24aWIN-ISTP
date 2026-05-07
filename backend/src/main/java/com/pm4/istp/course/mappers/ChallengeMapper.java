package com.pm4.istp.course.mappers;

import com.pm4.istp.course.db.CreateChallengeRequest;
import com.pm4.istp.course.db.SubTaskOptionRequest;
import com.pm4.istp.course.db.SubTaskRequest;
import com.pm4.istp.course.db.UpdateChallengeRequest;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.CourseChallenge;
import com.pm4.istp.course.db.entities.SubTask;
import com.pm4.istp.course.db.entities.SubTaskOption;
import com.pm4.istp.course.dto.ChallengeCreatorResponseDto;
import com.pm4.istp.course.dto.ChallengeDetailResponseDto;
import com.pm4.istp.course.dto.ChallengeStudentDto;
import com.pm4.istp.course.dto.CourseChallengeResponseDto;
import com.pm4.istp.course.dto.CreateChallengeRequestDto;
import com.pm4.istp.course.dto.CreateChallengeResponseDto;
import com.pm4.istp.course.dto.SubTaskOptionRequestDto;
import com.pm4.istp.course.dto.SubTaskOptionResponseDto;
import com.pm4.istp.course.dto.SubTaskOptionStudentDto;
import com.pm4.istp.course.dto.SubTaskRequestDto;
import com.pm4.istp.course.dto.SubTaskResponseDto;
import com.pm4.istp.course.dto.SubTaskStudentDto;
import com.pm4.istp.course.dto.UpdateChallengeRequestDto;
import com.pm4.istp.user.db.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChallengeMapper {

  CreateChallengeRequest fromDto(CreateChallengeRequestDto dto);

  UpdateChallengeRequest fromDto(UpdateChallengeRequestDto dto);

  SubTaskRequest fromDto(SubTaskRequestDto dto);

  SubTaskOptionRequest fromDto(SubTaskOptionRequestDto dto);

  @Mapping(target = "creatorId", source = "creator.id")
  CreateChallengeResponseDto toCreateResponseDto(Challenge challenge);

  ChallengeDetailResponseDto toDetailResponseDto(Challenge challenge);

  ChallengeCreatorResponseDto toCreatorDto(User user);

  SubTaskResponseDto toSubTaskResponseDto(SubTask subTask);

  SubTaskOptionStudentDto toOptionStudentDto(SubTaskOption option);

  SubTaskOptionResponseDto toOptionResponseDto(SubTaskOption option);

  @Mapping(target = "solved", ignore = true)
  @Mapping(target = "solvedFlag", ignore = true)
  @Mapping(target = "selectedOptionId", ignore = true)
  @Mapping(target = "options", source = "options")
  @Mapping(
      target = "theory",
      expression = "java(subTask.getFlag() == null || subTask.getFlag().isBlank())")
  SubTaskStudentDto toSubTaskStudentDto(SubTask subTask);

  @Mapping(target = "subTasks", source = "subTasks")
  @Mapping(target = "solved", ignore = true)
  @Mapping(target = "solvedSubTaskCount", ignore = true)
  @Mapping(target = "totalSubTaskCount", ignore = true)
  @Mapping(target = "awardedPoints", ignore = true)
  ChallengeStudentDto toStudentDto(Challenge challenge);

  @Mapping(target = "challengeId", source = "challenge.id")
  @Mapping(target = "challengeTitle", source = "challenge.title")
  @Mapping(target = "difficulty", source = "challenge.difficulty")
  CourseChallengeResponseDto toCourseChallengeResponseDto(CourseChallenge courseChallenge);
}
