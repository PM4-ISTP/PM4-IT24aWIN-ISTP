package com.pm4.istp.admin;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pm4.istp.admin.controllers.AdminSessionController;
import com.pm4.istp.admin.dto.AdminActiveSessionDto;
import com.pm4.istp.admin.services.AdminSessionService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminSessionControllerTest {

  private MockMvc mockMvc;

  @Mock private AdminSessionService adminSessionService;

  @InjectMocks private AdminSessionController controller;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  void listActiveSessions_returnsSessions() throws Exception {
    when(adminSessionService.listActiveSessions())
        .thenReturn(List.of(new AdminActiveSessionDto("s1", "u1", "alice", "127.0.0.1", 1L, 2L)));

    mockMvc
        .perform(get("/api/admin/sessions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].sessionId").value("s1"))
        .andExpect(jsonPath("$[0].username").value("alice"));
  }

  @Test
  void logoutSession_delegatesAndReturnsNoContent() throws Exception {
    doNothing().when(adminSessionService).logoutSession("session-1");

    mockMvc.perform(delete("/api/admin/sessions/session-1")).andExpect(status().isNoContent());

    verify(adminSessionService).logoutSession("session-1");
  }
}
