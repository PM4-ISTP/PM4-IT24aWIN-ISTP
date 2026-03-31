package com.pm4.istp.controller;

import static com.pm4.istp.util.JwtUtil.parseUserId;

import com.pm4.istp.domain.entites.User;
import com.pm4.istp.dto.CurrentUserResponseDto;
import com.pm4.istp.dto.ListInstructorUserResponseDto;
import com.pm4.istp.mappers.UserMapper;
import com.pm4.istp.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "User endpoints for the API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserMapper userMapper;
  private final UserService userService;

  @GetMapping(path = "/me")
  public ResponseEntity<CurrentUserResponseDto> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = parseUserId(jwt);
    User user = userService.getUserById(userId);
    return ResponseEntity.ok(userMapper.toCurrentUserResponseDto(user));
  }

  @GetMapping(path = "/instructors")
  public ResponseEntity<Page<ListInstructorUserResponseDto>> listInstructorUsers(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(required = false) String name,
      Pageable pageable) {
    UUID userId = parseUserId(jwt);
    Page<User> users;
    if (null != name && !name.trim().isEmpty()) {
      users = userService.searchInstructorUsersByName(userId, name, pageable);
    } else {
      users = userService.listInstructorUsers(userId, pageable);
    }

    return ResponseEntity.ok(users.map(userMapper::toListInstructorUserResponseDto));
  }
}
