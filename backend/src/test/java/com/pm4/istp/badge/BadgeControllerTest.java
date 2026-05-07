package com.pm4.istp.badge;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm4.istp.badge.controllers.BadgeController;
import com.pm4.istp.badge.dto.CourseBadgeConfigDto;
import com.pm4.istp.badge.dto.UpdateCourseBadgeRequestDto;
import com.pm4.istp.badge.dto.UserBadgeDto;
import com.pm4.istp.badge.services.BadgeService;
import com.pm4.istp.shared.util.GlobalExceptionHandler;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class BadgeControllerTest {

  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock private BadgeService badgeService;

  @InjectMocks private BadgeController controller;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(jwtResolver())
            .setValidator(validator)
            .build();
  }

  @Test
  void getCourseBadgeConfig_returnsConfig() throws Exception {
    UUID courseId = UUID.randomUUID();
    when(badgeService.getCourseBadgeConfig(courseId)).thenReturn(config(courseId, 1, "Course"));

    mockMvc
        .perform(get("/api/v1/courses/" + courseId + "/badge"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.courseId").value(courseId.toString()))
        .andExpect(jsonPath("$.courseTitle").value("Course"));
  }

  @Test
  void updateCourseBadgeConfig_validRequest_usesAuthenticatedUser() throws Exception {
    UUID courseId = UUID.randomUUID();
    UpdateCourseBadgeRequestDto request =
        new UpdateCourseBadgeRequestDto("#123456", "#ffffff", 2, "star", true);
    when(badgeService.updateCourseBadgeConfig(eq(USER_ID), eq(courseId), any()))
        .thenReturn(config(courseId, 2, "Course"));

    mockMvc
        .perform(
            put("/api/v1/courses/" + courseId + "/badge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.template").value(2));

    verify(badgeService).updateCourseBadgeConfig(eq(USER_ID), eq(courseId), any());
  }

  @Test
  void updateCourseBadgeConfig_invalidColor_returnsBadRequest() throws Exception {
    UUID courseId = UUID.randomUUID();
    UpdateCourseBadgeRequestDto request =
        new UpdateCourseBadgeRequestDto("blue", "#ffffff", 1, "star", true);

    mockMvc
        .perform(
            put("/api/v1/courses/" + courseId + "/badge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(badgeService);
  }

  @Test
  void getMyBadges_returnsBadgesForAuthenticatedUser() throws Exception {
    UUID badgeId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    when(badgeService.getUserBadges(USER_ID))
        .thenReturn(
            List.of(
                new UserBadgeDto(
                    badgeId, courseId, "Course", "#123456", "#ffffff", 1, "star", LocalDateTime.now())));

    mockMvc
        .perform(get("/api/v1/users/me/badges"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].badgeId").value(badgeId.toString()));
  }

  @Test
  void getCourseBadgeSvg_templateOne_returnsCircleSvgAndEscapesTitle() throws Exception {
    UUID courseId = UUID.randomUUID();
    when(badgeService.getCourseBadgeConfig(courseId))
        .thenReturn(config(courseId, 1, "A&B <Course> Name That Is Long"));

    mockMvc
        .perform(get("/api/v1/courses/" + courseId + "/badge/svg"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/svg+xml"))
        .andExpect(content().string(containsString("<circle cx=\"150\" cy=\"150\" r=\"130\"")))
        .andExpect(content().string(containsString("A&amp;B &lt;Course&gt;")));
  }

  @Test
  void getCourseBadgeSvg_templateTwo_returnsHexSvg() throws Exception {
    UUID courseId = UUID.randomUUID();
    when(badgeService.getCourseBadgeConfig(courseId)).thenReturn(config(courseId, 2, "Course"));

    mockMvc
        .perform(get("/api/v1/courses/" + courseId + "/badge/svg"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<polygon points=\"150,10 270,75")));
  }

  @Test
  void getCourseBadgeSvg_templateThree_returnsMedalSvg() throws Exception {
    UUID courseId = UUID.randomUUID();
    when(badgeService.getCourseBadgeConfig(courseId)).thenReturn(config(courseId, 3, "Course"));

    mockMvc
        .perform(get("/api/v1/courses/" + courseId + "/badge/svg"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<rect x=\"113\" y=\"12\"")));
  }

  private static CourseBadgeConfigDto config(UUID courseId, int template, String title) {
    return new CourseBadgeConfigDto(courseId, title, "#336699", "#ffffff", template, "star", true);
  }

  private static HandlerMethodArgumentResolver jwtResolver() {
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(USER_ID.toString()).build();
    return new HandlerMethodArgumentResolver() {
      @Override
      public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(AuthenticationPrincipal.class) != null
            && Jwt.class.isAssignableFrom(parameter.getParameterType());
      }

      @Override
      public Object resolveArgument(
          MethodParameter parameter,
          ModelAndViewContainer mavContainer,
          NativeWebRequest webRequest,
          WebDataBinderFactory binderFactory) {
        return jwt;
      }
    };
  }
}
