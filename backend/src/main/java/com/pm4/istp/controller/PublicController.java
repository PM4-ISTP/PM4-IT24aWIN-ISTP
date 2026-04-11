package com.pm4.istp.controller;

import com.pm4.istp.dto.PublicTeamMemberDto;
import com.pm4.istp.mappers.UserMapper;
import com.pm4.istp.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public", description = "Unauthenticated public endpoints")
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicController {

  private final UserService userService;
  private final UserMapper userMapper;

  @Value("${team.emails:}")
  private String teamEmailsRaw;

  @GetMapping("/team")
  public ResponseEntity<List<PublicTeamMemberDto>> getTeamMembers() {
    List<String> emails =
        Arrays.stream(teamEmailsRaw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    List<PublicTeamMemberDto> team =
        userService.getTeamMembers(emails).stream().map(userMapper::toPublicTeamMemberDto).toList();
    return ResponseEntity.ok(team);
  }
}
