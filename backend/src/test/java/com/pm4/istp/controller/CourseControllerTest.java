package com.pm4.istp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;
import com.pm4.istp.domain.CreateCourseRequest;
import com.pm4.istp.domain.UpdateCourseRequest;
import com.pm4.istp.domain.entites.Course;
import com.pm4.istp.dto.CourseDetailResponseDto;
import com.pm4.istp.dto.CreateCourseRequestDto;
import com.pm4.istp.dto.CreateCourseResponseDto;
import com.pm4.istp.dto.JoinByInviteCodeRequestDto;
import com.pm4.istp.dto.ListCourseResponseDto;
import com.pm4.istp.dto.UpdateCourseRequestDto;
import com.pm4.istp.exception.CourseAccessDeniedException;
import com.pm4.istp.exception.CourseNotFoundException;
import com.pm4.istp.exception.InvalidInviteCodeException;
import com.pm4.istp.mappers.CourseMapper;
import com.pm4.istp.repositories.CourseEnrollmentRepository;
import com.pm4.istp.service.CourseService;
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
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock private CourseMapper courseMapper;
  @Mock private CourseService courseService;
  @Mock private CourseEnrollmentRepository courseEnrollmentRepository;

  @InjectMocks private CourseController courseController;

  private UUID userId;
  private UUID courseId;
  private Jwt jwt;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    courseId = UUID.randomUUID();
    jwt =
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

    objectMapper = new ObjectMapper();
    JacksonJsonHttpMessageConverter converter =
        new JacksonJsonHttpMessageConverter(JsonMapper.builder().addModule(buildPageModule()).build());

    mockMvc =
        MockMvcBuilders.standaloneSetup(courseController)
            .setCustomArgumentResolvers(
                jwtResolver, new PageableHandlerMethodArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(converter)
            .build();
  }

  // ── createCourse ──────────────────────────────────────────────────────────

  @Test
  void createCourse_returnsCreated() throws Exception {
    Course course = new Course();
    course.setId(courseId);
    course.setTitle("Secure Coding");

    CreateCourseResponseDto dto = new CreateCourseResponseDto();
    dto.setId(courseId);
    dto.setTitle("Secure Coding");

    when(courseMapper.fromDto(any(CreateCourseRequestDto.class)))
        .thenReturn(new CreateCourseRequest());
    when(courseService.createCourse(eq(userId), any(CreateCourseRequest.class))).thenReturn(course);
    when(courseMapper.toDto(course)).thenReturn(dto);

    CreateCourseRequestDto requestDto =
        new CreateCourseRequestDto(
            "Secure Coding", "Desc", "Short desc.", false, false, null, null, List.of());

    mockMvc
        .perform(
            post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(courseId.toString()))
        .andExpect(jsonPath("$.title").value("Secure Coding"));
  }

  @Test
  void createCourse_whenTitleBlank_returnsBadRequest() throws Exception {
    CreateCourseRequestDto requestDto =
        new CreateCourseRequestDto("", "Desc", "Short desc.", false, false, null, null, List.of());

    mockMvc
        .perform(
            post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").exists());
  }

  // ── getCourse ─────────────────────────────────────────────────────────────

  @Test
  void getCourse_returnsOk() throws Exception {
    Course course = new Course();
    course.setId(courseId);

    CourseDetailResponseDto dto = new CourseDetailResponseDto();
    dto.setId(courseId);
    dto.setTitle("Secure Coding");

    when(courseService.getCourse(userId, courseId)).thenReturn(course);
    when(courseMapper.toCourseDetailDto(course)).thenReturn(dto);
    when(courseEnrollmentRepository.countByCourseId(courseId)).thenReturn(3L);
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
        .thenReturn(false);
    when(courseEnrollmentRepository.findByCourseIdFetchParticipant(courseId))
        .thenReturn(List.of());

    mockMvc
        .perform(get("/api/v1/courses/{id}", courseId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(courseId.toString()));
  }

  @Test
  void getCourse_whenNotFound_returnsNotFound() throws Exception {
    when(courseService.getCourse(userId, courseId))
        .thenThrow(new CourseNotFoundException("not found"));

    mockMvc
        .perform(get("/api/v1/courses/{id}", courseId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Course not found"));
  }

  @Test
  void getCourse_whenAccessDenied_returnsForbidden() throws Exception {
    when(courseService.getCourse(userId, courseId))
        .thenThrow(new CourseAccessDeniedException("denied"));

    mockMvc
        .perform(get("/api/v1/courses/{id}", courseId))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("Access denied"));
  }

  // ── updateCourse ──────────────────────────────────────────────────────────

  @Test
  void updateCourse_returnsOk() throws Exception {
    Course course = new Course();
    course.setId(courseId);

    CourseDetailResponseDto dto = new CourseDetailResponseDto();
    dto.setId(courseId);
    dto.setTitle("Updated Title");

    when(courseMapper.fromDto(any(UpdateCourseRequestDto.class)))
        .thenReturn(new UpdateCourseRequest());
    when(courseService.updateCourse(eq(userId), eq(courseId), any(UpdateCourseRequest.class)))
        .thenReturn(course);
    when(courseMapper.toCourseDetailDto(course)).thenReturn(dto);
    when(courseEnrollmentRepository.countByCourseId(courseId)).thenReturn(0L);
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
        .thenReturn(false);
    when(courseEnrollmentRepository.findByCourseIdFetchParticipant(courseId))
        .thenReturn(List.of());

    UpdateCourseRequestDto requestDto =
        new UpdateCourseRequestDto(
            "Updated Title", "Desc", "Short summary.", false, false, null, null, List.of());

    mockMvc
        .perform(
            put("/api/v1/courses/{id}", courseId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(courseId.toString()));
  }

  // ── deleteCourse ──────────────────────────────────────────────────────────

  @Test
  void deleteCourse_returnsNoContent() throws Exception {
    doNothing().when(courseService).deleteCourse(userId, courseId);

    mockMvc.perform(delete("/api/v1/courses/{id}", courseId)).andExpect(status().isNoContent());
  }

  @Test
  void deleteCourse_whenNotOwner_returnsForbidden() throws Exception {
    doThrow(new CourseAccessDeniedException("not owner"))
        .when(courseService)
        .deleteCourse(userId, courseId);

    mockMvc
        .perform(delete("/api/v1/courses/{id}", courseId))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("Access denied"));
  }

  // ── listCourses ───────────────────────────────────────────────────────────

  @Test
  void listCourses_returnsOkWithPage() throws Exception {
    Page<ListCourseResponseDto> page = new PageImpl<>(List.of(new ListCourseResponseDto()));

    when(courseService.listCoursesForInstructors(eq(userId), any())).thenReturn(page);

    mockMvc
        .perform(get("/api/v1/courses"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  // ── listPublishedCourses ──────────────────────────────────────────────────

  @Test
  void listPublishedCourses_withoutQuery_returnsOkWithPage() throws Exception {
    Page<ListCourseResponseDto> page = new PageImpl<>(List.of());

    when(courseService.listPublishedCourses(eq(null), any())).thenReturn(page);

    mockMvc
        .perform(get("/api/v1/courses/catalog"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  void listPublishedCourses_withQuery_returnsOkWithPage() throws Exception {
    Page<ListCourseResponseDto> page = new PageImpl<>(List.of());

    when(courseService.listPublishedCourses(eq("security"), any())).thenReturn(page);

    mockMvc
        .perform(get("/api/v1/courses/catalog").param("query", "security"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  // ── getPublicCourse ───────────────────────────────────────────────────────

  @Test
  void getPublicCourse_returnsOk() throws Exception {
    Course course = new Course();
    course.setId(courseId);

    CourseDetailResponseDto dto = new CourseDetailResponseDto();
    dto.setId(courseId);
    dto.setTitle("Public Course");

    when(courseService.getCourse(userId, courseId)).thenReturn(course);
    when(courseMapper.toCourseDetailDto(course)).thenReturn(dto);
    when(courseEnrollmentRepository.countByCourseId(courseId)).thenReturn(0L);
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
        .thenReturn(false);

    mockMvc
        .perform(get("/api/v1/courses/catalog/{id}", courseId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(courseId.toString()));
  }

  // ── enrollInCourse ────────────────────────────────────────────────────────

  @Test
  void enrollInCourse_returnsOk() throws Exception {
    Course course = new Course();
    course.setId(courseId);

    CourseDetailResponseDto dto = new CourseDetailResponseDto();
    dto.setId(courseId);

    when(courseService.enrollInCourse(userId, courseId)).thenReturn(course);
    when(courseMapper.toCourseDetailDto(course)).thenReturn(dto);
    when(courseEnrollmentRepository.countByCourseId(courseId)).thenReturn(1L);
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
        .thenReturn(true);
    when(courseEnrollmentRepository.findByCourseIdFetchParticipant(courseId))
        .thenReturn(List.of());

    mockMvc
        .perform(post("/api/v1/courses/{id}/enroll", courseId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(courseId.toString()));
  }

  @Test
  void enrollInPublicCourse_returnsOk() throws Exception {
    Course course = new Course();
    course.setId(courseId);

    CourseDetailResponseDto dto = new CourseDetailResponseDto();
    dto.setId(courseId);

    when(courseService.enrollInCourse(userId, courseId)).thenReturn(course);
    when(courseMapper.toCourseDetailDto(course)).thenReturn(dto);
    when(courseEnrollmentRepository.countByCourseId(courseId)).thenReturn(1L);
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
        .thenReturn(true);

    mockMvc
        .perform(post("/api/v1/courses/catalog/{id}/enroll", courseId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(courseId.toString()));
  }

  // ── joinByInviteCode ──────────────────────────────────────────────────────

  @Test
  void joinByInviteCode_returnsOk() throws Exception {
    Course course = new Course();
    course.setId(courseId);

    CourseDetailResponseDto dto = new CourseDetailResponseDto();
    dto.setId(courseId);

    when(courseService.joinByInviteCode(eq("ABC123"), eq(userId))).thenReturn(course);
    when(courseMapper.toCourseDetailDto(course)).thenReturn(dto);
    when(courseEnrollmentRepository.countByCourseId(courseId)).thenReturn(1L);
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
        .thenReturn(true);

    JoinByInviteCodeRequestDto requestDto = new JoinByInviteCodeRequestDto("ABC123");

    mockMvc
        .perform(
            post("/api/v1/courses/catalog/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(courseId.toString()));
  }

  @Test
  void joinByInviteCode_withInvalidCode_returnsNotFound() throws Exception {
    when(courseService.joinByInviteCode(eq("BADCOD"), eq(userId)))
        .thenThrow(new InvalidInviteCodeException("Invalid invite code"));

    JoinByInviteCodeRequestDto requestDto = new JoinByInviteCodeRequestDto("BADCOD");

    mockMvc
        .perform(
            post("/api/v1/courses/catalog/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Invalid invite code"));
  }

  @Test
  void joinByInviteCode_withBlankCode_returnsBadRequest() throws Exception {
    JoinByInviteCodeRequestDto requestDto = new JoinByInviteCodeRequestDto("");

    mockMvc
        .perform(
            post("/api/v1/courses/catalog/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").exists());
  }

  // ── regenerateInviteCode ──────────────────────────────────────────────────

  @Test
  void regenerateInviteCode_returnsOk() throws Exception {
    Course course = new Course();
    course.setId(courseId);
    course.setInviteCode("NEWCOD");

    CourseDetailResponseDto dto = new CourseDetailResponseDto();
    dto.setId(courseId);
    dto.setInviteCode("NEWCOD");

    when(courseService.regenerateInviteCode(courseId, userId)).thenReturn(course);
    when(courseMapper.toCourseDetailDto(course)).thenReturn(dto);
    when(courseEnrollmentRepository.countByCourseId(courseId)).thenReturn(0L);
    when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
        .thenReturn(false);
    when(courseEnrollmentRepository.findByCourseIdFetchParticipant(courseId))
        .thenReturn(List.of());

    mockMvc
        .perform(post("/api/v1/courses/{id}/invite-code/regenerate", courseId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(courseId.toString()))
        .andExpect(jsonPath("$.inviteCode").value("NEWCOD"));
  }

  @Test
  void regenerateInviteCode_whenNotOwner_returnsForbidden() throws Exception {
    when(courseService.regenerateInviteCode(courseId, userId))
        .thenThrow(new CourseAccessDeniedException("not owner"));

    mockMvc
        .perform(post("/api/v1/courses/{id}/invite-code/regenerate", courseId))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("Access denied"));
  }

  @Test
  void regenerateInviteCode_whenCourseNotFound_returnsNotFound() throws Exception {
    when(courseService.regenerateInviteCode(courseId, userId))
        .thenThrow(new CourseNotFoundException("not found"));

    mockMvc
        .perform(post("/api/v1/courses/{id}/invite-code/regenerate", courseId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Course not found"));
  }

  // ── Jackson helper ────────────────────────────────────────────────────────

  /**
   * Registers a custom {@link Page} serializer so that standalone MockMvc tests produce a
   * reduced page JSON structure with {@code content}, {@code totalElements},
   * {@code totalPages}, {@code size}, and {@code number} using Jackson 3's
   * {@link JacksonJsonHttpMessageConverter}.
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
    public void serialize(Page value, JsonGenerator gen, SerializationContext ctxt)
        throws JacksonException {
      gen.writeStartObject();
      ctxt.defaultSerializeProperty("content", value.getContent(), gen);
      gen.writeNumberProperty("totalElements", value.getTotalElements());
      gen.writeNumberProperty("totalPages", value.getTotalPages());
      gen.writeNumberProperty("size", value.getSize());
      gen.writeNumberProperty("number", value.getNumber());
      gen.writeEndObject();
    }
  }
}
