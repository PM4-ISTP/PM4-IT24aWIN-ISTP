package com.pm4.istp.mappers;

import com.pm4.istp.domain.entites.User;
import com.pm4.istp.dto.ListInstructorUserResponseDto;
import com.pm4.istp.dto.PublicTeamMemberDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

  ListInstructorUserResponseDto toListInstructorUserResponseDto(User user);

  PublicTeamMemberDto toPublicTeamMemberDto(User user);
}
