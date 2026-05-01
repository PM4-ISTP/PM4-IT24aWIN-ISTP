package com.pm4.istp.badge.controllers;

import static com.pm4.istp.shared.util.JwtUtil.parseUserId;

import com.pm4.istp.badge.dto.CourseBadgeConfigDto;
import com.pm4.istp.badge.dto.UpdateCourseBadgeRequestDto;
import com.pm4.istp.badge.dto.UserBadgeDto;
import com.pm4.istp.badge.services.BadgeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BadgeController {

  private final BadgeService badgeService;

  @GetMapping("/courses/{courseId}/badge")
  public ResponseEntity<CourseBadgeConfigDto> getCourseBadgeConfig(
      @PathVariable UUID courseId, @AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.ok(badgeService.getCourseBadgeConfig(courseId));
  }

  @PutMapping("/courses/{courseId}/badge")
  public ResponseEntity<CourseBadgeConfigDto> updateCourseBadgeConfig(
      @PathVariable UUID courseId,
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody UpdateCourseBadgeRequestDto request) {
    UUID userId = parseUserId(jwt);
    return ResponseEntity.ok(badgeService.updateCourseBadgeConfig(userId, courseId, request));
  }

  @GetMapping("/users/me/badges")
  public ResponseEntity<List<UserBadgeDto>> getMyBadges(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = parseUserId(jwt);
    return ResponseEntity.ok(badgeService.getUserBadges(userId));
  }

  @GetMapping(value = "/courses/{courseId}/badge/svg", produces = MediaType.IMAGE_SVG_VALUE)
  public ResponseEntity<String> getCourseBadgeSvg(@PathVariable UUID courseId) {
    CourseBadgeConfigDto config = badgeService.getCourseBadgeConfig(courseId);
    String svg = generateSvg(config);
    return ResponseEntity.ok().contentType(MediaType.valueOf("image/svg+xml")).body(svg);
  }

  private String generateSvg(CourseBadgeConfigDto config) {
    String color = config.primaryColor();
    String textColor = config.textColor();
    String icon = config.badgeIcon() != null ? config.badgeIcon() : "🏆";
    String title = config.courseTitle() != null ? config.courseTitle() : "";
    if (title.length() > 20) title = title.substring(0, 20) + "…";
    int template = config.template();
    return switch (template) {
      case 2 -> buildHexSvg(color, textColor, icon, title);
      case 3 -> buildMedalSvg(color, textColor, icon, title);
      default -> buildCircleSvg(color, textColor, icon, title);
    };
  }

  private String darken(String hex, double factor) {
    int r = Integer.parseInt(hex.substring(1, 3), 16);
    int g = Integer.parseInt(hex.substring(3, 5), 16);
    int b = Integer.parseInt(hex.substring(5, 7), 16);
    return String.format(
        "#%02x%02x%02x",
        (int) Math.max(0, r * (1 - factor)),
        (int) Math.max(0, g * (1 - factor)),
        (int) Math.max(0, b * (1 - factor)));
  }

  private String lighten(String hex, double factor) {
    int r = Integer.parseInt(hex.substring(1, 3), 16);
    int g = Integer.parseInt(hex.substring(3, 5), 16);
    int b = Integer.parseInt(hex.substring(5, 7), 16);
    return String.format(
        "#%02x%02x%02x",
        (int) Math.min(255, r + (255 - r) * factor),
        (int) Math.min(255, g + (255 - g) * factor),
        (int) Math.min(255, b + (255 - b) * factor));
  }

  private String buildCircleSvg(String color, String textColor, String icon, String title) {
    String dark = darken(color, 0.4);
    String light = lighten(color, 0.3);
    return ("<svg viewBox=\"0 0 300 300\" xmlns=\"http://www.w3.org/2000/svg\" width=\"300\" height=\"300\">"
        + "<defs>"
        + "<linearGradient id=\"bg\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">"
        + "<stop offset=\"0%%\" stop-color=\"" + light + "\"/>"
        + "<stop offset=\"100%%\" stop-color=\"" + dark + "\"/>"
        + "</linearGradient>"
        + "<radialGradient id=\"fade\" cx=\"50%%\" cy=\"72%%\" r=\"52%%\">"
        + "<stop offset=\"0%%\" stop-color=\"" + dark + "\" stop-opacity=\"0.85\"/>"
        + "<stop offset=\"100%%\" stop-color=\"" + dark + "\" stop-opacity=\"0\"/>"
        + "</radialGradient>"
        + "<clipPath id=\"cclip\"><circle cx=\"150\" cy=\"150\" r=\"130\"/></clipPath>"
        + "</defs>"
        + "<circle cx=\"150\" cy=\"150\" r=\"130\" fill=\"url(#bg)\"/>"
        + "<circle cx=\"150\" cy=\"150\" r=\"120\" fill=\"none\" stroke=\"" + textColor + "\" stroke-width=\"2\" stroke-opacity=\"0.25\"/>"
        + "<circle cx=\"150\" cy=\"150\" r=\"108\" fill=\"none\" stroke=\"" + textColor + "\" stroke-width=\"1\" stroke-opacity=\"0.15\"/>"
        + "<rect x=\"20\" y=\"195\" width=\"260\" height=\"90\" fill=\"url(#fade)\" clip-path=\"url(#cclip)\"/>"
        + "<text x=\"150\" y=\"158\" text-anchor=\"middle\" font-size=\"72\" dominant-baseline=\"middle\">" + icon + "</text>"
        + "<text x=\"150\" y=\"245\" text-anchor=\"middle\" font-size=\"18\" fill=\"" + textColor + "\" font-weight=\"bold\" font-family=\"system-ui,sans-serif\">" + escapeXml(title) + "</text>"
        + "</svg>");
  }

  private String buildHexSvg(String color, String textColor, String icon, String title) {
    String dark = darken(color, 0.4);
    String light = lighten(color, 0.3);
    return ("<svg viewBox=\"0 0 300 310\" xmlns=\"http://www.w3.org/2000/svg\" width=\"300\" height=\"310\">"
        + "<defs>"
        + "<linearGradient id=\"hbg\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">"
        + "<stop offset=\"0%%\" stop-color=\"" + light + "\"/>"
        + "<stop offset=\"100%%\" stop-color=\"" + dark + "\"/>"
        + "</linearGradient>"
        + "</defs>"
        + "<polygon points=\"150,10 270,75 270,235 150,300 30,235 30,75\" fill=\"url(#hbg)\"/>"
        + "<polygon points=\"150,25 255,83 255,227 150,285 45,227 45,83\" fill=\"none\" stroke=\"" + textColor + "\" stroke-width=\"2\" stroke-opacity=\"0.25\"/>"
        + "<polygon points=\"150,40 240,91 240,219 150,270 60,219 60,91\" fill=\"none\" stroke=\"" + textColor + "\" stroke-width=\"1\" stroke-opacity=\"0.15\"/>"
        + "<text x=\"150\" y=\"170\" text-anchor=\"middle\" font-size=\"72\" dominant-baseline=\"middle\">" + icon + "</text>"
        + "<text x=\"150\" y=\"248\" text-anchor=\"middle\" font-size=\"18\" fill=\"" + textColor + "\" font-weight=\"bold\" font-family=\"system-ui,sans-serif\">" + escapeXml(title) + "</text>"
        + "</svg>");
  }

  private String buildMedalSvg(String color, String textColor, String icon, String title) {
    String dark = darken(color, 0.4);
    String light = lighten(color, 0.3);
    return ("<svg viewBox=\"0 0 300 300\" xmlns=\"http://www.w3.org/2000/svg\" width=\"300\" height=\"300\">"
        + "<defs>"
        + "<linearGradient id=\"mbg\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">"
        + "<stop offset=\"0%%\" stop-color=\"" + light + "\"/>"
        + "<stop offset=\"100%%\" stop-color=\"" + dark + "\"/>"
        + "</linearGradient>"
        + "<radialGradient id=\"shine\" cx=\"38%%\" cy=\"35%%\" r=\"50%%\">"
        + "<stop offset=\"0%%\" stop-color=\"white\" stop-opacity=\"0.22\"/>"
        + "<stop offset=\"100%%\" stop-color=\"white\" stop-opacity=\"0\"/>"
        + "</radialGradient>"
        + "<radialGradient id=\"mfade\" cx=\"50%%\" cy=\"72%%\" r=\"52%%\">"
        + "<stop offset=\"0%%\" stop-color=\"" + dark + "\" stop-opacity=\"0.82\"/>"
        + "<stop offset=\"100%%\" stop-color=\"" + dark + "\" stop-opacity=\"0\"/>"
        + "</radialGradient>"
        + "<clipPath id=\"mclip\"><circle cx=\"150\" cy=\"150\" r=\"130\"/></clipPath>"
        + "</defs>"
        + "<circle cx=\"150\" cy=\"150\" r=\"130\" fill=\"url(#mbg)\"/>"
        + "<circle cx=\"150\" cy=\"150\" r=\"128\" fill=\"none\" stroke=\"" + textColor + "\" stroke-width=\"3\" stroke-dasharray=\"8 5\" stroke-opacity=\"0.45\"/>"
        + "<circle cx=\"150\" cy=\"150\" r=\"116\" fill=\"none\" stroke=\"" + textColor + "\" stroke-width=\"1.5\" stroke-opacity=\"0.2\"/>"
        + "<circle cx=\"150\" cy=\"150\" r=\"104\" fill=\"none\" stroke=\"" + textColor + "\" stroke-width=\"1\" stroke-opacity=\"0.12\"/>"
        + "<circle cx