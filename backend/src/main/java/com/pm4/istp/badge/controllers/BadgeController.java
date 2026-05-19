package com.pm4.istp.badge.controllers;

import static com.pm4.istp.shared.util.JwtUtil.parseUserId;

import com.pm4.istp.badge.dto.CourseBadgeConfigDto;
import com.pm4.istp.badge.dto.UpdateCourseBadgeRequestDto;
import com.pm4.istp.badge.dto.UserBadgeDto;
import com.pm4.istp.badge.services.BadgeService;
import com.pm4.istp.shared.dto.ErrorDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Badge", description = "Course badge configuration and user badge endpoints")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BadgeController {
  private static final String BADGE_TEXT_STYLE =
      "\" font-weight=\"bold\" font-family=\"system-ui,sans-serif\">";

  private final BadgeService badgeService;

  @Operation(
      summary = "Get a course badge configuration",
      description = "Returns the badge design configuration for a course.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Badge configuration retrieved successfully",
            content = @Content(schema = @Schema(implementation = CourseBadgeConfigDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Course not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @GetMapping("/courses/{courseId}/badge")
  public ResponseEntity<CourseBadgeConfigDto> getCourseBadgeConfig(
      @PathVariable UUID courseId, @AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.ok(badgeService.getCourseBadgeConfig(courseId));
  }

  @Operation(
      summary = "Update a course badge configuration",
      description =
          "Updates the badge design for a course. Only instructors of the course may do this.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Badge configuration updated successfully",
            content = @Content(schema = @Schema(implementation = CourseBadgeConfigDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid badge configuration",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Course not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @PutMapping("/courses/{courseId}/badge")
  public ResponseEntity<CourseBadgeConfigDto> updateCourseBadgeConfig(
      @PathVariable UUID courseId,
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody UpdateCourseBadgeRequestDto request) {
    UUID userId = parseUserId(jwt);
    return ResponseEntity.ok(badgeService.updateCourseBadgeConfig(userId, courseId, request));
  }

  @Operation(
      summary = "List my badges",
      description = "Returns all badges earned by the authenticated user.")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Badges retrieved successfully")})
  @GetMapping("/users/me/badges")
  public ResponseEntity<List<UserBadgeDto>> getMyBadges(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = parseUserId(jwt);
    return ResponseEntity.ok(badgeService.getUserBadges(userId));
  }

  @Operation(
      summary = "Render a course badge as SVG",
      description = "Returns the course badge rendered as an SVG image.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Badge SVG rendered successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "Course not found",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
      })
  @GetMapping(value = "/courses/{courseId}/badge/svg", produces = "image/svg+xml")
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
    if (title.length() > 20) {
      title = title.substring(0, 20) + "…";
    }
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
        + "<stop offset=\"0%%\" stop-color=\""
        + light
        + "\"/>"
        + "<stop offset=\"100%%\" stop-color=\""
        + dark
        + "\"/>"
        + "</linearGradient>"
        + "<radialGradient id=\"fade\" cx=\"50%%\" cy=\"72%%\" r=\"52%%\">"
        + "<stop offset=\"0%%\" stop-color=\""
        + dark
        + "\" stop-opacity=\"0.85\"/>"
        + "<stop offset=\"100%%\" stop-color=\""
        + dark
        + "\" stop-opacity=\"0\"/>"
        + "</radialGradient>"
        + "<clipPath id=\"cclip\"><circle cx=\"150\" cy=\"150\" r=\"130\"/></clipPath>"
        + "</defs>"
        + "<circle cx=\"150\" cy=\"150\" r=\"130\" fill=\"url(#bg)\"/>"
        + "<circle cx=\"150\" cy=\"150\" r=\"120\" fill=\"none\" stroke=\""
        + textColor
        + "\" stroke-width=\"2\" stroke-opacity=\"0.25\"/>"
        + "<circle cx=\"150\" cy=\"150\" r=\"108\" fill=\"none\" stroke=\""
        + textColor
        + "\" stroke-width=\"1\" stroke-opacity=\"0.15\"/>"
        + "<rect x=\"20\" y=\"195\" width=\"260\" height=\"90\" fill=\"url(#fade)\" clip-path=\"url(#cclip)\"/>"
        + "<text x=\"150\" y=\"158\" text-anchor=\"middle\" font-size=\"72\" dominant-baseline=\"middle\">"
        + icon
        + "</text>"
        + "<text x=\"150\" y=\"245\" text-anchor=\"middle\" font-size=\"18\" fill=\""
        + textColor
        + BADGE_TEXT_STYLE
        + escapeXml(title)
        + "</text>"
        + "</svg>");
  }

  private String buildHexSvg(String color, String textColor, String icon, String title) {
    String dark = darken(color, 0.4);
    String light = lighten(color, 0.3);
    return ("<svg viewBox=\"0 0 300 310\" xmlns=\"http://www.w3.org/2000/svg\" width=\"300\" height=\"310\">"
        + "<defs>"
        + "<linearGradient id=\"hbg\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">"
        + "<stop offset=\"0%%\" stop-color=\""
        + light
        + "\"/>"
        + "<stop offset=\"100%%\" stop-color=\""
        + dark
        + "\"/>"
        + "</linearGradient>"
        + "</defs>"
        + "<polygon points=\"150,10 270,75 270,235 150,300 30,235 30,75\" fill=\"url(#hbg)\"/>"
        + "<polygon points=\"150,25 255,83 255,227 150,285 45,227 45,83\" fill=\"none\" stroke=\""
        + textColor
        + "\" stroke-width=\"2\" stroke-opacity=\"0.25\"/>"
        + "<polygon points=\"150,40 240,91 240,219 150,270 60,219 60,91\" fill=\"none\" stroke=\""
        + textColor
        + "\" stroke-width=\"1\" stroke-opacity=\"0.15\"/>"
        + "<text x=\"150\" y=\"170\" text-anchor=\"middle\" font-size=\"72\" dominant-baseline=\"middle\">"
        + icon
        + "</text>"
        + "<text x=\"150\" y=\"248\" text-anchor=\"middle\" font-size=\"18\" fill=\""
        + textColor
        + BADGE_TEXT_STYLE
        + escapeXml(title)
        + "</text>"
        + "</svg>");
  }

  private String buildMedalSvg(String color, String textColor, String icon, String title) {
    String dark = darken(color, 0.4);
    String light = lighten(color, 0.3);
    String ribbonMid = lighten(color, 0.15);
    return ("<svg viewBox=\"0 0 300 360\" xmlns=\"http://www.w3.org/2000/svg\" width=\"300\" height=\"360\">"
        + "<defs>"
        + "<linearGradient id=\"mbg\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">"
        + "<stop offset=\"0%%\" stop-color=\""
        + light
        + "\"/>"
        + "<stop offset=\"100%%\" stop-color=\""
        + dark
        + "\"/>"
        + "</linearGradient>"
        + "<linearGradient id=\"mribbon\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"0\">"
        + "<stop offset=\"0%%\" stop-color=\""
        + dark
        + "\"/>"
        + "<stop offset=\"50%%\" stop-color=\""
        + ribbonMid
        + "\"/>"
        + "<stop offset=\"100%%\" stop-color=\""
        + dark
        + "\"/>"
        + "</linearGradient>"
        + "<radialGradient id=\"mshine\" cx=\"38%%\" cy=\"35%%\" r=\"50%%\">"
        + "<stop offset=\"0%%\" stop-color=\"white\" stop-opacity=\"0.22\"/>"
        + "<stop offset=\"100%%\" stop-color=\"white\" stop-opacity=\"0\"/>"
        + "</radialGradient>"
        + "<radialGradient id=\"mfade\" cx=\"50%%\" cy=\"78%%\" r=\"45%%\">"
        + "<stop offset=\"0%%\" stop-color=\""
        + dark
        + "\" stop-opacity=\"0.82\"/>"
        + "<stop offset=\"100%%\" stop-color=\""
        + dark
        + "\" stop-opacity=\"0\"/>"
        + "</radialGradient>"
        + "<clipPath id=\"mclip\"><circle cx=\"150\" cy=\"215\" r=\"120\"/></clipPath>"
        + "</defs>"
        + "<polygon points=\"122,28 150,28 148,95 124,95\" fill=\"url(#mribbon)\"/>"
        + "<polygon points=\"150,28 178,28 176,95 152,95\" fill=\"url(#mribbon)\"/>"
        + "<line x1=\"150\" y1=\"28\" x2=\"150\" y2=\"95\" stroke=\""
        + textColor
        + "\" stroke-width=\"0.5\" stroke-opacity=\"0.2\"/>"
        + "<rect x=\"113\" y=\"12\" width=\"74\" height=\"18\" rx=\"5\" fill=\""
        + ribbonMid
        + "\" stroke=\""
        + textColor
        + "\" stroke-width=\"1\" stroke-opacity=\"0.3\"/>"
        + "<rect x=\"118\" y=\"16\" width=\"64\" height=\"10\" rx=\"3\" fill=\"none\" stroke=\""
        + textColor
        + "\" stroke-width=\"0.5\" stroke-opacity=\"0.2\"/>"
        + "<circle cx=\"150\" cy=\"215\" r=\"120\" fill=\"url(#mbg)\"/>"
        + "<circle cx=\"150\" cy=\"215\" r=\"118\" fill=\"none\" stroke=\""
        + textColor
        + "\" stroke-width=\"3\" stroke-dasharray=\"8 5\" stroke-opacity=\"0.45\"/>"
        + "<circle cx=\"150\" cy=\"215\" r=\"106\" fill=\"none\" stroke=\""
        + textColor
        + "\" stroke-width=\"1.5\" stroke-opacity=\"0.2\"/>"
        + "<circle cx=\"150\" cy=\"215\" r=\"94\" fill=\"none\" stroke=\""
        + textColor
        + "\" stroke-width=\"1\" stroke-opacity=\"0.12\"/>"
        + "<circle cx=\"150\" cy=\"215\" r=\"120\" fill=\"url(#mshine)\"/>"
        + "<rect x=\"20\" y=\"258\" width=\"260\" height=\"80\" fill=\"url(#mfade)\" clip-path=\"url(#mclip)\"/>"
        + "<text x=\"150\" y=\"220\" text-anchor=\"middle\" font-size=\"72\" dominant-baseline=\"middle\">"
        + icon
        + "</text>"
        + "<text x=\"150\" y=\"305\" text-anchor=\"middle\" font-size=\"17\" fill=\""
        + textColor
        + BADGE_TEXT_STYLE
        + escapeXml(title)
        + "</text>"
        + "</svg>");
  }

  private String escapeXml(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
