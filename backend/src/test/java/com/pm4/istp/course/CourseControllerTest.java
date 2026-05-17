package com.pm4.istp.course;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
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

import com.pm4.istp.course.controllers.CourseController;
import com.pm4.istp.course.db.CreateCourseRequest;
import com.pm4.istp.course.db.InstructorRoleEnum;
import com.pm4.istp.course.db.UpdateCourseRequest;
import com.pm4.istp.course.db.entities.LabStatusEnum;
import com.pm4.istp.course.db.entities.CourseStatusEnum;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.db.entities.CourseInstructor;
import com.pm4.istp.course.dto.LabCreatorResponseDto;
import com.pm4.istp.course.dto.LabStudentDto;
import com.pm4.istp.course.dto.CourseDetailInstructorResponseDto;
import com.pm4.istp.course.dto.CourseDetailResponseDto;
import com.pm4.istp.course.dto.CourseLabDeadlineDto;
import com.pm4.istp.course.dto.CourseLabSubmissionEntryDto;
import com.pm4.istp.course.dto.CourseLabSubmissionDetailDto;
import com.pm4.istp.course.dto.CourseLabSubmissionStatusEnum;
import com.pm4.istp.course.dto.CourseLabSubmissionsResponseDto;
import com.pm4.istp.course.dto.CourseParticipantResponseDto;
import com.pm4.istp.course.dto.CreateCourseRequestDto;
import com.pm4.istp.course.dto.CreateCourseResponseDto;
import com.pm4.istp.course.dto.JoinByInviteCodeRequestDto;
import com.pm4.istp.course.dto.ListCourseResponseDto;
import com.pm4.istp.course.dto.PublicCourseDetailResponseDto;
import com.pm4.istp.course.dto.UpdateCourseRequestDto;
import com.pm4.istp.course.exceptions.CourseAccessDeniedException;
import com.pm4.istp.course.exceptions.CourseNotFoundException;
import com.pm4.istp.course.exceptions.InvalidInviteCodeException;
import com.pm4.istp.course.mappers.CourseMapper;
import com.pm4.istp.course.repositories.CourseEnrollmentRepository;
import com.pm4.istp.course.services.CourseService;
import com.pm4.istp.course.services.CourseTopicService;
import com.pm4.istp.shared.util.GlobalExceptionHandler;
import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.dto.UserDto;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
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

    @Mock
    private CourseMapper courseMapper;
    @Mock
    private CourseService courseService;
    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock
    private CourseTopicService courseTopicService;
    @Mock
    private com.pm4.istp.course.repositories.ChallengeCompletionRepository challengeCompletionRepository;
    @Mock
    private com.pm4.istp.course.repositories.ChallengeRepository challengeRepository;

    @InjectMocks
    private CourseController courseController;

    private UUID userId;
    private UUID courseId;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(userId.toString())
                .build();

        lenient().when(courseTopicService.normalizeAndValidate(any()))
            .thenAnswer(invocation -> {
                String value = invocation.getArgument(0);
                if (value == null || value.trim().isBlank()) {
                    return null;
                }
                return value.trim();
            });

        HandlerMethodArgumentResolver jwtResolver = new HandlerMethodArgumentResolver() {
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
        JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter(
                JsonMapper.builder().addModule(buildPageModule()).build());

        mockMvc = MockMvcBuilders.standaloneSetup(courseController)
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

        CreateCourseRequestDto requestDto = new CreateCourseRequestDto(
                "Secure Coding",
                "Desc",
                "Short desc.",
                CourseStatusEnum.DRAFT,
                null,
                null,
                List.of(),
                "UNLIMITED");

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
                new CreateCourseRequestDto(
                        "",
                        "Desc",
                        "Short desc.",
                        CourseStatusEnum.DRAFT,
                        null,
                        null,
                        List.of(),
                        "UNLIMITED");

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
    void getCourse_whenCallerIsInstructor_includesInviteCode() throws Exception {
        User instructor = new User();
        instructor.setId(userId);

        CourseInstructor ownerRelation = new CourseInstructor();
        ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
        ownerRelation.setInstructor(instructor);

        Course course = new Course();
        course.setId(courseId);
        course.addCourseInstructor(ownerRelation);

        CourseDetailResponseDto dto = new CourseDetailResponseDto();
        dto.setId(courseId);
        dto.setInviteCode("INVITE");

        when(courseService.getCourse(userId, courseId)).thenReturn(course);
        when(courseMapper.toCourseDetailDto(course)).thenReturn(dto);
        when(courseEnrollmentRepository.countByCourseId(courseId)).thenReturn(0L);
        when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
                .thenReturn(false);
        when(courseEnrollmentRepository.findByCourseIdFetchParticipant(courseId))
                .thenReturn(List.of());

        mockMvc
                .perform(get("/api/v1/courses/{id}", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inviteCode").value("INVITE"));
    }

    @Test
    void getCourse_whenCallerIsNotInstructor_nullsInviteCode() throws Exception {
        Course course = new Course();
        course.setId(courseId);
        // no instructors → userId is not an instructor

        CourseDetailResponseDto dto = new CourseDetailResponseDto();
        dto.setId(courseId);
        dto.setInviteCode("SECRET");

        when(courseService.getCourse(userId, courseId)).thenReturn(course);
        when(courseMapper.toCourseDetailDto(course)).thenReturn(dto);
        when(courseEnrollmentRepository.countByCourseId(courseId)).thenReturn(1L);
        when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
                .thenReturn(true);
        when(courseEnrollmentRepository.findByCourseIdFetchParticipant(courseId))
                .thenReturn(List.of());

        mockMvc
                .perform(get("/api/v1/courses/{id}", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inviteCode").doesNotExist());
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

        UpdateCourseRequestDto requestDto = new UpdateCourseRequestDto(
                "Updated Title",
                "Desc",
                "Short summary.",
                CourseStatusEnum.DRAFT,
                null,
                null,
                List.of(),
                "UNLIMITED");

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

    // ── listEnrollments
    // ───────────────────────────────────────────────────────────

    @Test
    void listEnrollments_returnsOkWithPage() throws Exception {
        Page<ListCourseResponseDto> page = new PageImpl<>(List.of(new ListCourseResponseDto()));

        when(courseService.listUserEnrollments(eq(userId), any())).thenReturn(page);

        mockMvc
                .perform(get("/api/v1/courses/my-enrollments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ── listPublishedCourses ──────────────────────────────────────────────────

    @Test
    void listPublishedCourses_withoutQuery_returnsOkWithPage() throws Exception {
        Page<ListCourseResponseDto> page = new PageImpl<>(List.of());

        when(courseService.listPublishedCourses(eq(null), eq(null), any())).thenReturn(page);

        mockMvc
                .perform(get("/api/v1/courses/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void listPublishedCourses_withQuery_returnsOkWithPage() throws Exception {
        Page<ListCourseResponseDto> page = new PageImpl<>(List.of());

        when(courseService.listPublishedCourses(eq("security"), eq(null), any())).thenReturn(page);

        mockMvc
                .perform(get("/api/v1/courses/catalog").param("query", "security"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ── getPublicCourse ───────────────────────────────────────────────────────

    @Test
    void getPublicCourse_returnsOk() throws Exception {
        LabStudentDto challenge1 = generateChallengeStudentDto("Lab 1",
                LabStatusEnum.PUBLIC, "Creator 1");
        LabStudentDto challenge2 = generateChallengeStudentDto("Lab 2",
                LabStatusEnum.PRIVATE, "Creator 2");
        LabStudentDto challenge3 = generateChallengeStudentDto("Lab 3",
                LabStatusEnum.DRAFT, "Creator 3");
        LabStudentDto challenge4 = generateChallengeStudentDto("Lab 4",
                LabStatusEnum.PUBLIC, "Creator 4");

        Course course = new Course();
        course.setId(courseId);

        UUID instructorId = UUID.randomUUID();
        UUID nestedUserId = UUID.randomUUID();

        UserDto instructorUser = new UserDto();
        instructorUser.setId(nestedUserId);
        instructorUser.setName("Instructor");

        CourseDetailInstructorResponseDto instructor = new CourseDetailInstructorResponseDto();
        instructor.setId(instructorId);
        instructor.setInstructor(instructorUser);

        PublicCourseDetailResponseDto dto = new PublicCourseDetailResponseDto();
        dto.setId(courseId);
        dto.setTitle("Public Course");
        dto.setCourseInstructors(List.of(instructor));
        dto.setCourseLabs(List.of(challenge1, challenge2, challenge3, challenge4));
        dto.setParticipants(
            List.of(new CourseParticipantResponseDto(UUID.randomUUID(), "Student", null, null)));

        when(courseService.getCourse(userId, courseId)).thenReturn(course);
        when(courseMapper.toPublicCourseDetailDto(course)).thenReturn(dto);
        when(courseEnrollmentRepository.countByCourseId(courseId)).thenReturn(0L);
        when(courseEnrollmentRepository.existsByCourseIdAndParticipantId(courseId, userId))
                .thenReturn(false);

        mockMvc
                .perform(get("/api/v1/courses/catalog/{id}", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courseId.toString()))
                .andExpect(jsonPath("$.courseInstructors").isArray())
                .andExpect(jsonPath("$.courseInstructors", hasSize(1)))
                .andExpect(jsonPath("$.courseInstructors[*].id").value(everyItem(nullValue())))
                .andExpect(jsonPath("$.courseInstructors[*].instructor.id").value(everyItem(nullValue())))
                .andExpect(jsonPath("$.courseInstructors[0].instructor.name").value("Instructor"))
                .andExpect(jsonPath("$.courseLabs").isArray())
                .andExpect(jsonPath("$.courseLabs", hasSize(3)))
                .andExpect(jsonPath("$.courseLabs[*].creator.id").value(everyItem(nullValue())))
                .andExpect(jsonPath("$.courseLabs[0].creator.name").value("Creator 1"))
                .andExpect(jsonPath("$.courseLabs[1].creator.name").value("Creator 2"))
                .andExpect(jsonPath("$.courseLabs[2].creator.name").value("Creator 4"))
                .andExpect(jsonPath("$.participants").value(nullValue()));
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

        PublicCourseDetailResponseDto dto = new PublicCourseDetailResponseDto();
        dto.setId(courseId);
        dto.setCourseInstructors(Collections.emptyList());
        dto.setCourseLabs(Collections.emptyList());

        when(courseService.enrollInCourse(userId, courseId)).thenReturn(course);
        when(courseMapper.toPublicCourseDetailDto(course)).thenReturn(dto);
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

        PublicCourseDetailResponseDto dto = new PublicCourseDetailResponseDto();
        dto.setId(courseId);
        dto.setCourseInstructors(Collections.emptyList());
        dto.setCourseLabs(Collections.emptyList());

        when(courseService.joinByInviteCode("ABC123", userId)).thenReturn(course);
        when(courseMapper.toPublicCourseDetailDto(course)).thenReturn(dto);
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
        when(courseService.joinByInviteCode("BADCOD", userId))
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
        User instructor = new User();
        instructor.setId(userId);

        CourseInstructor ownerRelation = new CourseInstructor();
        ownerRelation.setInstructorRole(InstructorRoleEnum.OWNER);
        ownerRelation.setInstructor(instructor);

        Course course = new Course();
        course.setId(courseId);
        course.setInviteCode("NEWCOD");
        course.addCourseInstructor(ownerRelation);

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

    @Test
    void remainingCourseEndpoints_delegateToServices() throws Exception {
        UUID participantId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        Course course = new Course();
        course.setId(courseId);
        CourseDetailResponseDto detailDto = new CourseDetailResponseDto();
        detailDto.setId(courseId);
        CourseLabSubmissionsResponseDto submissions =
                new CourseLabSubmissionsResponseDto(courseId, List.of(), List.of(), List.of());
        CourseLabSubmissionDetailDto submissionDetail =
                new CourseLabSubmissionDetailDto(
                        courseId,
                        participantId,
                        labId,
                        "Lab",
                        null,
                        null,
                        CourseLabSubmissionStatusEnum.SUBMITTED,
                        4,
                        5,
                        List.of());
        CourseLabSubmissionEntryDto scoreEntry =
                new CourseLabSubmissionEntryDto(
                        participantId,
                        labId,
                        1,
                        1,
                        4,
                        5,
                        null,
                        CourseLabSubmissionStatusEnum.SUBMITTED);
        CourseLabDeadlineDto deadline =
                new CourseLabDeadlineDto(courseId, "Course", labId, "Lab", LocalDateTime.now());

        doNothing().when(courseService).removeParticipant(userId, courseId, participantId);
        doNothing().when(courseService).leaveCourse(userId, courseId);
        when(courseService.updateCourseLabs(eq(userId), eq(courseId), any()))
                .thenReturn(course);
        when(courseMapper.toCourseDetailDto(course)).thenReturn(detailDto);
        when(courseService.getCourseLabSubmissions(userId, courseId)).thenReturn(submissions);
        when(courseService.getCourseLabSubmissionDetails(userId, courseId, participantId, labId))
                .thenReturn(submissionDetail);
        when(courseService.updateCourseChallengeScore(eq(userId), eq(courseId), eq(participantId), eq(challengeId), any()))
                .thenReturn(scoreEntry);
        when(courseService.listUpcomingDeadlines(userId)).thenReturn(List.of(deadline));
        when(courseTopicService.listActiveTopics()).thenReturn(List.of("web", "crypto"));

        mockMvc.perform(delete("/api/v1/courses/{id}/participants/{participantId}", courseId, participantId))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/courses/catalog/{id}/leave", courseId))
                .andExpect(status().isNoContent());
        mockMvc
                .perform(
                        put("/api/v1/courses/{id}/labs", courseId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"labs":[{"labId":"%s","orderIndex":1,"dueAt":"2026-05-06T12:00:00"}]}
                                        """
                                                .formatted(labId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courseId.toString()));
        mockMvc.perform(get("/api/v1/courses/{id}/submissions", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(courseId.toString()));
        mockMvc.perform(get("/api/v1/courses/{id}/submissions/{participantId}/{labId}", courseId, participantId, labId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantId").value(participantId.toString()));
        mockMvc
                .perform(
                        put("/api/v1/courses/{id}/submissions/{participantId}/{challengeId}/score",
                                        courseId, participantId, challengeId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"points\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.awardedPoints").value(4));
        mockMvc.perform(get("/api/v1/courses/my-deadlines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseId").value(courseId.toString()));
        mockMvc.perform(get("/api/v1/courses/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("web"));
    }

    @Test
    void listPublishedCourses_withTopic_normalizesTopic() throws Exception {
        Page<ListCourseResponseDto> page = new PageImpl<>(List.of());

        when(courseService.listPublishedCourses(eq("security"), eq("web"), any())).thenReturn(page);

        mockMvc
                .perform(get("/api/v1/courses/catalog").param("query", "security").param("topic", " web "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ── Mock object generators ────────────────────────────────────────────────
    private LabStudentDto generateChallengeStudentDto(String title,
            LabStatusEnum labStatus, String creatorName) {
        LabCreatorResponseDto challengeCreatorResponseDto = new LabCreatorResponseDto();
        challengeCreatorResponseDto.setId(UUID.randomUUID());
        challengeCreatorResponseDto.setName(creatorName);

        LabStudentDto dto = new LabStudentDto();
        dto.setId(UUID.randomUUID());
        dto.setTitle(title);
        dto.setCreator(challengeCreatorResponseDto);
        dto.setStatus(labStatus);
        dto.setChallenges(List.of());
        return dto;
    }

    // ── Jackson helper ────────────────────────────────────────────────────────

    /**
     * Registers a custom {@link Page} serializer so that standalone MockMvc tests
     * produce a
     * reduced page JSON structure with {@code content}, {@code totalElements},
     * {@code totalPages}, {@code size}, and {@code number} using Jackson 3's
     * {@link JacksonJsonHttpMessageConverter}.
     */
    @SuppressWarnings({ })
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
