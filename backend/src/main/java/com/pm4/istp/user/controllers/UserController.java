package com.pm4.istp.user.controllers;

import static com.pm4.istp.shared.util.JwtUtil.parseUserId;

import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.dto.ListInstructorUserResponseDto;
import com.pm4.istp.user.mappers.UserMapper;
import com.pm4.istp.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

  @Operation(
      summary = "List collaborator/instructor users",
      description =
          "Returns a paginated list of users eligible to be added as course collaborators,"
              + " optionally filtered by a search query.")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Users retrieved successfully")})
  @GetMapping(path = {"/collaborators", "/instructors"})
  public ResponseEntity<Page<ListInstructorUserResponseDto>> listCollaboratorUsers(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(required = false) String query,
      Pageable pageable) {
    UUID userId = parseUserId(jwt);
    Page<User> users;
    if (null != query && !query.trim().isEmpty()) {
      users = userService.searchCollaboratorUsersByQuery(userId, query, pageable);
    } else {
      users = userService.listCollaboratorUsers(userId, pageable);
    }

    return ResponseEntity.ok(users.map(userMapper::toListInstructorUserResponseDto));
  }
}
