package com.pm4.istp.admin;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm4.istp.admin.controllers.AdminTopicController;
import com.pm4.istp.admin.dto.AdminTopicRequest;
import com.pm4.istp.admin.services.AdminTopicService;
import com.pm4.istp.shared.util.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminTopicControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock private AdminTopicService adminTopicService;

  @InjectMocks private AdminTopicController controller;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  // ── GET /api/admin/topics ────────────────────────────────────────────────────

  @Test
  void listTopics_returns200WithTopicList() throws Exception {
    when(adminTopicService.listTopics()).thenReturn(List.of("Docker", "Security", "Web"));

    mockMvc
        .perform(get("/api/admin/topics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0]").value("Docker"))
        .andExpect(jsonPath("$[1]").value("Security"))
        .andExpect(jsonPath("$[2]").value("Web"));
  }

  @Test
  void listTopics_withNoTopics_returns200WithEmptyArray() throws Exception {
    when(adminTopicService.listTopics()).thenReturn(List.of());

    mockMvc
        .perform(get("/api/admin/topics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  // ── POST /api/admin/topics ───────────────────────────────────────────────────

  @Test
  void addTopic_withValidRequest_returns200WithMessage() throws Exception {
    AdminTopicRequest request = new AdminTopicRequest();
    request.setValue("Docker");
    doNothing().when(adminTopicService).addTopic("Docker");

    mockMvc
        .perform(
            post("/api/admin/topics")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Topic added"));

    verify(adminTopicService).addTopic("Docker");
  }

  @Test
  void addTopic_withMissingValue_returns400() throws Exception {
    AdminTopicRequest request = new AdminTopicRequest();
    request.setValue("");

    mockMvc
        .perform(
            post("/api/admin/topics")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void addTopic_withTooShortValue_returns400() throws Exception {
    AdminTopicRequest request = new AdminTopicRequest();
    request.setValue("ab");

    mockMvc
        .perform(
            post("/api/admin/topics")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void addTopic_withTooLongValue_returns400() throws Exception {
    AdminTopicRequest request = new AdminTopicRequest();
    request.setValue("A".repeat(25));

    mockMvc
        .perform(
            post("/api/admin/topics")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void addTopic_whenServiceThrowsAlreadyExists_returns400() throws Exception {
    AdminTopicRequest request = new AdminTopicRequest();
    request.setValue("Docker");
    doThrow(new IllegalArgumentException("Topic already exists"))
        .when(adminTopicService)
        .addTopic("Docker");

    mockMvc
        .perform(
            post("/api/admin/topics")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void addTopic_whenServiceThrowsTopicLimitReached_returns400() throws Exception {
    AdminTopicRequest request = new AdminTopicRequest();
    request.setValue("Docker");
    doThrow(new IllegalArgumentException("Topic limit reached"))
        .when(adminTopicService)
        .addTopic("Docker");

    mockMvc
        .perform(
            post("/api/admin/topics")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  // ── DELETE /api/admin/topics/{value} ────────────────────────────────────────

  @Test
  void deleteTopic_withExistingTopic_returns200WithMessage() throws Exception {
    doNothing().when(adminTopicService).deleteTopic("Docker");

    mockMvc
        .perform(delete("/api/admin/topics/Docker"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Topic deleted"));

    verify(adminTopicService).deleteTopic("Docker");
  }

  @Test
  void deleteTopic_withNonExistingTopic_returns200BecauseIdempotent() throws Exception {
    doNothing().when(adminTopicService).deleteTopic("NonExistent");

    mockMvc
        .perform(delete("/api/admin/topics/NonExistent"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Topic deleted"));
  }
}
