package com.pm4.istp.controller;

import static com.pm4.istp.util.JwtUtil.parseUserId;

import com.pm4.istp.domain.User;
import com.pm4.istp.repositories.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path = "/api/v1/tests")
@RequiredArgsConstructor
public class TestController {

  private final UserRepository userRepository;

  @PostMapping
  public ResponseEntity<User> test(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = parseUserId(jwt);

    return userRepository
        .findById(userId)
        .map(user -> ResponseEntity.ok(user))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
