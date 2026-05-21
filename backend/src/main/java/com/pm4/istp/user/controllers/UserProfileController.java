package com.pm4.istp.user.controllers;

import static com.pm4.istp.shared.util.JwtUtil.parseUserId;

import com.pm4.istp.shared.dto.ErrorDto;
import com.pm4.istp.user.dto.AddOnlineTimeRequestDto;
import com.pm4.istp.user.dto.UpdateUserProfileRequestDto;
import com.pm4.istp.user.dto.UserDto;
import com.pm4.istp.user.mappers.UserMapper;
import com.pm4.istp.user.services.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User Profile", description = "User profile endpoints for the API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {
  private final UserProfileService userProfileService;
  private final UserMapper userMapper;

  @Operation(
      summary = "Get my profile",
      description = "Returns the profile of the authenticated user.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserDto.class)))
      })
  @GetMapping("/me/profile")
  public ResponseEntity<UserDto> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = parseUserId(jwt);
    return ResponseEntity.ok(userMapper.toUserDto(userProfileService.getProfile(userId)));
  }

  @Operation(
      summary = "Update my profile",
      description = "Updates the authenticated user's name, title and profile picture.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile updated successfully",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PutMapping("/me/profile")
  public ResponseEntity<UserDto> updateMyProfile(
      @AuthenticationPrincipal Jwt jwt,
      Authentication authentication,
      @Valid @RequestBody UpdateUserProfileRequestDto request) {
    UUID actorId = parseUserId(jwt);
    return ResponseEntity.ok(
        userMapper.toUserDto(
            userProfileService.updateProfile(
                actorId,
                authentication == null ? null : authentication.getAuthorities(),
                actorId,
                request)));
  }

  @Operation(
      summary = "Update a user's profile",
      description =
          "Updates another user's profile. Requires sufficient privileges over the target user.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile updated successfully",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PutMapping("/{userId}/profile")
  public ResponseEntity<UserDto> updateUserProfile(
      @AuthenticationPrincipal Jwt jwt,
      Authentication authentication,
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateUserProfileRequestDto request) {
    UUID actorId = parseUserId(jwt);
    return ResponseEntity.ok(
        userMapper.toUserDto(
            userProfileService.updateProfile(
                actorId,
                authentication == null ? null : authentication.getAuthorities(),
                userId,
                request)));
  }

  @Operation(
      summary = "Add online time",
      description = "Adds elapsed online time (in seconds) to the authenticated user's total.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Online time recorded successfully"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PatchMapping("/me/online-time")
  public ResponseEntity<Void> addOnlineTime(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddOnlineTimeRequestDto request) {
    UUID userId = parseUserId(jwt);
    userProfileService.addOnlineTime(userId, request.getSeconds());
    return ResponseEntity.noContent().build();
  }
}
