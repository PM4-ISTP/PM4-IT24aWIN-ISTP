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
import com.pm4.istp.admin.controllers.AdminLabController;
import com.pm4.istp.admin.dto.AdminLabListItemDto;
import com.pm4.istp.admin.dto.AdminUpdateLabRequestDto;
import com.pm4.istp.admin.services.AdminLabService;
import com.pm4.istp.course.db.entities.LabDifficultyEnum;
import com.pm4.istp.course.db.entities.LabStatusEnum;
import com.pm4.istp.course.exceptions.LabNotFoundException;
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
class AdminChallengeControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock private AdminLabService adminChallengeService;

  @InjectMocks private AdminLabController controller;

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
  void listChallenges_noQuery_returns200WithPage() throws Exception {
    Page<AdminLabListItemDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(adminChallengeService.listChallenges(eq(null), any())).thenReturn(page);

    mockMvc
        .perform(get("/api/admin/labs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  void listChallenges_withQuery_passesQueryToService() throws Exception {
    Page<AdminLabListItemDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(adminChallengeService.listChallenges(eq("sql"), any())).thenReturn(page);

    mockMvc.perform(get("/api/admin/labs").param("q", "sql")).andExpect(status().isOk());

    verify(adminChallengeService).listChallenges(eq("sql"), any());
  }

  @Test
  void updateChallenge_validRequest_returns204() throws Exception {
    UUID id = UUID.randomUUID();
    doNothing().when(adminChallengeService).updateChallenge(eq(id), any());

    mockMvc
        .perform(
            put("/api/admin/labs/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUpdateRequest())))
        .andExpect(status().isNoContent());
  }

  @Test
  void updateChallenge_missingTitle_returns400() throws Exception {
    UUID id = UUID.randomUUID();
    AdminUpdateLabRequestDto req = validUpdateRequest();
    req.setTitle("");

    mockMvc
        .perform(
            put("/api/admin/labs/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateChallenge_challengeNotFound_returns404() throws Exception {
    UUID id = UUID.randomUUID();
    doThrow(new LabNotFoundException("not found"))
        .when(adminChallengeService)
        .updateChallenge(eq(id), any());

    mockMvc
        .perform(
            put("/api/admin/labs/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUpdateRequest())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Lab not found"));
  }

  @Test
  void deleteChallenge_ok_returns204() throws Exception {
    UUID id = UUID.randomUUID();
    doNothing().when(adminChallengeService).deleteChallenge(id);

    mockMvc.perform(delete("/api/admin/labs/" + id)).andExpect(status().isNoContent());
  }

  private static AdminUpdateLabRequestDto validUpdateRequest() {
    AdminUpdateLabRequestDto req = new AdminUpdateLabRequestDto();
    req.setTitle("My lab");
    req.setShortDescription("short");
    req.setDescription("<p>desc</p>");
    req.setStatus(LabStatusEnum.DRAFT);
    req.setDifficulty(LabDifficultyEnum.BEGINNER);
    req.setMaxScore(0);
    return req;
  }
}

