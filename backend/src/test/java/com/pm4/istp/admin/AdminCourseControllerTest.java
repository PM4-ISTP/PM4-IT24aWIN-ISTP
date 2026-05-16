package com.pm4.istp.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm4.istp.admin.controllers.AdminCourseController;
import com.pm4.istp.admin.dto.AdminCourseListItemDto;
import com.pm4.istp.admin.dto.AdminUpdateCourseRequestDto;
import com.pm4.istp.admin.services.AdminCourseService;
import com.pm4.istp.course.exceptions.CourseNotFoundException;
import com.pm4.istp.shared.util.GlobalExceptionHandler;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminCourseControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock private AdminCourseService adminCourseService;

  @InjectMocks private AdminCourseController controller;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void listCourses_noQuery_returns200WithPage() throws Exception {
    Page<AdminCourseListItemDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(adminCourseService.listCourses(eq(null), any())).thenReturn(page);

    mockMvc
        .perform(get("/api/admin/courses"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  void listCourses_withQuery_passesQueryToService() throws Exception {
    Page<AdminCourseListItemDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(adminCourseService.listCourses(eq("spring"), any())).thenReturn(page);

    mockMvc.perform(get("/api/admin/courses").param("q", "spring")).andExpect(status().isOk());

    verify(adminCourseService).listCourses(eq("spring"), any());
  }

  @Test
  void updateCourse_validRequest_returns204() throws Exception {
    UUID id = UUID.randomUUID();
    doNothing().when(adminCourseService).updateCourse(eq(id), any());

    mockMvc
        .perform(
            put("/api/admin/courses/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUpdateRequest())))
        .andExpect(status().isNoContent());
  }

  @Test
  void updateCourse_missingTitle_returns400() throws Exception {
    UUID id = UUID.randomUUID();
    AdminUpdateCourseRequestDto req = validUpdateRequest();
    req.setTitle("");

    mockMvc
        .perform(
            put("/api/admin/courses/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateCourse_courseNotFound_returns404() throws Exception {
    UUID id = UUID.randomUUID();
    doThrow(new CourseNotFoundException("not found")).when(adminCourseService).updateCourse(eq(id), any());

    mockMvc
        .perform(
            put("/api/admin/courses/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUpdateRequest())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Course not found"));
  }

  @Test
  void deleteCourse_ok_returns204() throws Exception {
    UUID id = UUID.randomUUID();
    doNothing().when(adminCourseService).deleteCourse(id);

    mockMvc.perform(delete("/api/admin/courses/" + id)).andExpect(status().isNoContent());
  }

  private static AdminUpdateCourseRequestDto validUpdateRequest() {
    AdminUpdateCourseRequestDto req = new AdminUpdateCourseRequestDto();
    req.setTitle("My course");
    req.setDescription("<p>desc</p>");
    req.setShortDescription("short");
    req.setPublished(false);
    req.setPrivate(false);
    req.setTopic(null);
    req.setImageUrl(null);
    return req;
  }
}
