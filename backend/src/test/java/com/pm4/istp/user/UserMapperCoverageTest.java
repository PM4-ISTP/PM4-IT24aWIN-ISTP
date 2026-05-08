package com.pm4.istp.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.db.entities.UserRoleEnum;
import com.pm4.istp.user.dto.UserDto;
import com.pm4.istp.user.mappers.UserMapperImpl;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserMapperCoverageTest {

  private final UserMapperImpl mapper = new UserMapperImpl();

  @Test
  void mapsUserDtoEntityAndInstructorListDto() {
    UUID id = UUID.randomUUID();
    UserDto dto = new UserDto();
    dto.setId(id);
    dto.setName("Alice");
    dto.setEmail("alice@example.com");
    dto.setUsername("alice");
    dto.setFirstName("Alice");
    dto.setLastName("Example");
    dto.setPicture("picture");
    dto.setTitle("Student");
    dto.setTotalSecondsOnline(42);

    User user = mapper.fromDto(dto);
    user.setRoles(Set.of(UserRoleEnum.ROLE_INSTRUCTOR));

    assertThat(mapper.fromDto(null)).isNull();
    assertThat(mapper.toUserDto(null)).isNull();
    assertThat(mapper.toListInstructorUserResponseDto(null)).isNull();
    assertThat(user.getEmail()).isEqualTo("alice@example.com");
    assertThat(mapper.toUserDto(user).getTotalSecondsOnline()).isEqualTo(42);
    assertThat(mapper.toListInstructorUserResponseDto(user).getRoles())
        .containsExactly(UserRoleEnum.ROLE_INSTRUCTOR);

    user.setRoles(null);
    assertThat(mapper.toListInstructorUserResponseDto(user).getRoles()).isNull();
  }
}
