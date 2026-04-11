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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import com.pm4.istp.domain.CreateCourseRequest;
import com.pm4.istp.domain.UpdateCourseRequest;
import com.pm4.istp.domain.entites.Course;
import com.pm4.istp.dto.CourseDetailResponseDto;
import com.pm4.istp.dto.CreateCourseInstructorRequestDto;
import com.pm4.istp.dto.CreateCourseRequestDto;
import com.pm4.istp.dto.CreateCourseResponseDto;
import com.pm4.istp.dto.ListCourseResponseDto;
import com.pm4.istp.dto.UpdateCourseInstructorRequestDto;
import com.pm4.istp.dto.UpdateCourseRequestDto;
import com.pm4.istp.exception.CourseAccessDeniedException;
import com.pm4.istp.exception.CourseNotFoundException;
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

    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()).registerModule(buildPageModule());
    MappingJackson2HttpMessageConverter converter =
        new MappingJackson2HttpMessageConverter(objectMapper);

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
        new CreateCourseRequestDto("Secure Coding", "Desc", "Short desc.", false, null, null, List.of());

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
        new CreateCourseRequestDto("", "Desc", "Short desc.", false, null, null, List.of());

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
            "Updated Title", "Desc", "Short summary.", false, null, null, List.of());

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
