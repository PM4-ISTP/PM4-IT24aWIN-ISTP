package com.pm4.istp.user;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pm4.istp.shared.keycloak.KeycloakAdminClient;
import com.pm4.istp.user.controllers.UserPasswordController;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class UserPasswordControllerTest {

  private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

  private MockMvc mockMvc;

  @Mock private KeycloakAdminClient keycloakAdminClient;

  @InjectMocks private UserPasswordController controller;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(jwtResolver())
            .build();
  }

  @Test
  void sendMyPasswordResetEmail_delegatesToKeycloakAndReturnsNoContent() throws Exception {
    mockMvc
        .perform(post("/api/v1/users/me/password-reset-email"))
        .andExpect(status().isNoContent());

    verify(keycloakAdminClient).executeActionsEmail(USER_ID, List.of("UPDATE_PASSWORD"));
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
