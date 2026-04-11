package com.pm4.istp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm4.istp.dto.PodCreationRequest;
import com.pm4.istp.dto.PodCreationResponse;
import com.pm4.istp.exception.K8sException;
import com.pm4.istp.service.K8sService;
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
class K8sControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock private K8sService k8sService;

  @InjectMocks private K8sController k8sController;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(k8sController).build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void createPod_returnsCreatedWithResponse() throws Exception {
    PodCreationResponse response =
        new PodCreationResponse(
            "CREATED",
            "challenge-abc123",
            "default",
            "Resources created",
            "http://app-abc123.example.com",
            "http://term-abc123.example.com",
            "secretpass");

    when(k8sService.createPod(any(PodCreationRequest.class))).thenReturn(response);

    PodCreationRequest request = new PodCreationRequest();
    request.setImage("nginx:latest");
    request.setContainerPort(80);

    mockMvc
        .perform(
            post("/api/k8s/pods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.podName").value("challenge-abc123"))
        .andExpect(jsonPath("$.namespace").value("default"));
  }

  @Test
  void createPod_whenK8sServiceThrows_returnsInternalServerError() throws Exception {
    when(k8sService.createPod(any(PodCreationRequest.class)))
        .thenThrow(new K8sException("cluster unreachable"));

    PodCreationRequest request = new PodCreationRequest();
    request.setImage("nginx:latest");
    request.setContainerPort(80);

    mockMvc
        .perform(
            post("/api/k8s/pods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isInternalServerError())
        .andExpect(content().string("Kubernetes operation failed: cluster unreachable"));
  }

  @Test
  void deletePod_returnsNoContent() throws Exception {
    doNothing().when(k8sService).deletePod(eq("challenge-abc123"));

    mockMvc
        .perform(delete("/api/k8s/pods/{instanceName}", "challenge-abc123"))
        .andExpect(status().isNoContent());
  }

  @Test
  void deletePod_whenK8sServiceThrows_returnsInternalServerError() throws Exception {
    doThrow(new K8sException("not found")).when(k8sService).deletePod(eq("challenge-xyz"));

    mockMvc
        .perform(delete("/api/k8s/pods/{instanceName}", "challenge-xyz"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().string("Kubernetes operation failed: not found"));
  }
}
