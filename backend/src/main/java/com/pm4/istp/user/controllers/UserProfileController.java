package com.pm4.istp.user.controllers;

import static com.pm4.istp.shared.util.JwtUtil.parseUserId;

import com.pm4.istp.user.dto.UpdateUserProfileRequestDto;
import com.pm4.istp.user.dto.UserDto;
import com.pm4.istp.user.mappers.UserMapper;
import com.pm4.istp.user.services.UserProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
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

  @GetMapping("/me/profile")
  public ResponseEntity<UserDto> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = parseUserId(jwt);
    return ResponseEntity.ok(userMapper.toUserDto(userProfileService.getProfile(userId)));
  }

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
}
