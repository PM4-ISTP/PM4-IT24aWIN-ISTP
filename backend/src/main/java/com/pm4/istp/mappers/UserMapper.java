package com.pm4.istp.mappers;

import com.pm4.istp.domain.entites.User;
import com.pm4.istp.dto.CurrentUserResponseDto;
import com.pm4.istp.dto.ListInstructorUserResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
  CurrentUserResponseDto toCurrentUserResponseDto(User user);

  ListInstructorUserResponseDto toListInstructorUserResponseDto(User user);
}
