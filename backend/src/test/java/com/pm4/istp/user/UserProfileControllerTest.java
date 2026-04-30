package com.pm4.istp.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pm4.istp.shared.util.GlobalExceptionHandler;
import com.pm4.istp.user.controllers.UserProfileController;
import com.pm4.istp.user.mappers.UserMapper;
import com.pm4.istp.user.services.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
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
class UserProfileControllerTest {

  private MockMvc mockMvc;

  @Mock private UserProfileService userProfileService;
  @Mock private UserMapper userMapper;

  @InjectMocks private UserProfileController userProfileController;

  @BeforeEach
  void setUp() {
    String subject = java.util.UUID.randomUUID().toString();
    Jwt jwt =
        Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();

    Authentication authentication = org.mockito.Mockito.mock(Authentication.class);

    HandlerMethodArgumentResolver jwtResolver =
        new HandlerMethodArgumentResolver() {
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

    HandlerMethodArgumentResolver authenticationResolver =
        new HandlerMethodArgumentResolver() {
          @Override
          public boolean supportsParameter(MethodParameter parameter) {
            return Authentication.class.isAssignableFrom(parameter.getParameterType());
          }

          @Override
          public Object resolveArgument(
              MethodParameter parameter,
              ModelAndViewContainer mavContainer,
              NativeWebRequest webRequest,
              WebDataBinderFactory binderFactory) {
            return authentication;
          }
        };

    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    mockMvc =
        MockMvcBuilders.standaloneSetup(userProfileController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(jwtResolver, authenticationResolver)
            .setValidator(validator)
            .build();
  }

  @Test
  void updateMyProfile_invalidInput_returnsBadRequest() throws Exception {
    String body =
        """
        {
          "firstName": "",
          "lastName": "Doe",
          "title": "Mr.",
          "pictureUrl": "https://example.com/p.png"
        }
        """;

    mockMvc
        .perform(
            put("/api/v1/users/me/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("firstName: must not be blank"));

    verify(userProfileService, never()).updateProfile(any(), any(), any(), any());
  }
}
