package com.pm4.istp.user.mappers;

import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.dto.ListInstructorUserResponseDto;
import com.pm4.istp.user.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

  User fromDto(UserDto dto);

  UserDto toUserDto(User user);

  ListInstructorUserResponseDto toListInstructorUserResponseDto(User user);
}
