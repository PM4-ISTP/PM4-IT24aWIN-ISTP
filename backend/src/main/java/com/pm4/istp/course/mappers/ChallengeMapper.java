package com.pm4.istp.course.mappers;

import com.pm4.istp.course.db.CreateChallengeRequest;
import com.pm4.istp.course.db.UpdateChallengeRequest;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.CourseChallenge;
import com.pm4.istp.course.dto.ChallengeCreatorResponseDto;
import com.pm4.istp.course.dto.ChallengeDetailResponseDto;
import com.pm4.istp.course.dto.CourseChallengeResponseDto;
import com.pm4.istp.course.dto.CreateChallengeRequestDto;
import com.pm4.istp.course.dto.CreateChallengeResponseDto;
import com.pm4.istp.course.dto.UpdateChallengeRequestDto;
import com.pm4.istp.user.db.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChallengeMapper {

  CreateChallengeRequest fromDto(CreateChallengeRequestDto dto);

  UpdateChallengeRequest fromDto(UpdateChallengeRequestDto dto);

  @Mapping(target = "creatorId", source = "creator.id")
  CreateChallengeResponseDto toCreateResponseDto(Challenge challenge);

  ChallengeDetailResponseDto toDetailResponseDto(Challenge challenge);

  ChallengeCreatorResponseDto toCreatorDto(User user);

  @Mapping(target = "challengeId", source = "challenge.id")
  @Mapping(target = "challengeTitle", source = "challenge.title")
  @Mapping(target = "difficulty", source = "challenge.difficulty")
  CourseChallengeResponseDto toCourseChallengeResponseDto(CourseChallenge courseChallenge);
}
