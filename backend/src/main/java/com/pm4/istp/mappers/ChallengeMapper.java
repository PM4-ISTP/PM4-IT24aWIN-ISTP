package com.pm4.istp.mappers;

import com.pm4.istp.domain.CreateChallengeRequest;
import com.pm4.istp.domain.UpdateChallengeRequest;
import com.pm4.istp.domain.entites.Challenge;
import com.pm4.istp.domain.entites.CourseChallenge;
import com.pm4.istp.domain.entites.User;
import com.pm4.istp.dto.*;
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
