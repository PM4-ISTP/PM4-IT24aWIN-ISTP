package com.pm4.istp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pm4.istp.domain.entites.User;
import com.pm4.istp.dto.ListInstructorUserResponseDto;
import com.pm4.istp.mappers.UserMapper;
import com.pm4.istp.service.UserService;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  private MockMvc mockMvc;

  @Mock private UserMapper userMapper;
  @Mock private UserService userService;

  @InjectMocks private UserController userController;

  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(userId.toString())
            .build();

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

    MappingJackson2HttpMessageConverter converter =
        new MappingJackson2HttpMessageConverter(
            new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(buildPageModule()));

    mockMvc =
        MockMvcBuilders.standaloneSetup(userController)
            .setCustomArgumentResolvers(
                jwtResolver, new PageableHandlerMethodArgumentResolver())
            .setMessageConverters(converter)
            .build();
  }

  @Test
  void listCollaboratorUsers_withoutQuery_returnsOk() throws Exception {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setName("Alice");

    Page<User> page = new PageImpl<>(List.of(user));
    ListInstructorUserResponseDto dto = new ListInstructorUserResponseDto();
    dto.setName("Alice");

    when(userService.listCollaboratorUsers(eq(userId), any())).thenReturn(page);
    when(userMapper.toListInstructorUserResponseDto(user)).thenReturn(dto);

    mockMvc
        .perform(get("/api/v1/users/collaborators"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].name").value("Alice"));
  }

  @Test
  void listCollaboratorUsers_withQuery_delegatesToSearch() throws Exception {
    Page<User> page = new PageImpl<>(List.of());

    when(userService.searchCollaboratorUsersByQuery(eq(userId), eq("alice"), any()))
        .thenReturn(page);

    mockMvc
        .perform(get("/api/v1/users/collaborators").param("query", "alice"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  void listCollaboratorUsers_viaInstructorsPath_returnsOk() throws Exception {
    Page<User> page = new PageImpl<>(List.of());

    when(userService.listCollaboratorUsers(eq(userId), any())).thenReturn(page);

    mockMvc
        .perform(get("/api/v1/users/instructors"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  void listCollaboratorUsers_withBlankQuery_delegatesToList() throws Exception {
    Page<User> page = new PageImpl<>(List.of());

    when(userService.listCollaboratorUsers(eq(userId), any())).thenReturn(page);

    mockMvc
        .perform(get("/api/v1/users/collaborators").param("query", "   "))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  // ── Jackson helper ────────────────────────────────────────────────────────

  /**
   * In Spring Data 4.x, {@code Page<T>} no longer implements {@code Iterable}, so Jackson's
   * default serializer fails. This module registers a custom serializer using
   * {@code Page.getContent()} to avoid the {@code UnsupportedOperationException}.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static SimpleModule buildPageModule() {
    SimpleModule module = new SimpleModule("TestPageModule");
    module.addSerializer(Page.class, new PageSerializer());
    return module;
  }

  @SuppressWarnings("rawtypes")
  private static class PageSerializer extends StdSerializer<Page> {

    PageSerializer() {
      super(Page.class);
    }

    @Override
    public void serialize(Page value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeStartObject();
      provider.defaultSerializeField("content", value.getContent(), gen);
      gen.writeNumberField("totalElements", value.getTotalElements());
      gen.writeNumberField("totalPages", value.getTotalPages());
      gen.writeNumberField("size", value.getSize());
      gen.writeNumberField("number", value.getNumber());
      gen.writeEndObject();
    }
  }
}
