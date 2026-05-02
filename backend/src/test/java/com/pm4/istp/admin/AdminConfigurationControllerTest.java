package com.pm4.istp.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm4.istp.admin.controllers.AdminConfigurationController;
import com.pm4.istp.admin.db.AdminConfig;
import com.pm4.istp.admin.dto.AdminConfigRequest;
import com.pm4.istp.admin.services.AdminConfigurationService;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
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
class AdminConfigurationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AdminConfigurationService adminConfigurationService;

    @InjectMocks
    private AdminConfigurationController adminConfigurationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminConfigurationController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testUploadAndStoreAdminConfig_Success() throws Exception {
        String kubeconfigBase64 = Base64.getEncoder().encodeToString("content".getBytes());

        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 29, 10, 15, 0);
        AdminConfig adminConfig =
                new AdminConfig(UUID.randomUUID(), "1", "1Gi", "ghcr-pull-secret", "content", 3600, updatedAt);

        when(adminConfigurationService.createConfiguration(
                        any(byte[].class), eq("1"), eq("1Gi"), eq("ghcr-pull-secret"), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(adminConfig);

        AdminConfigRequest request = new AdminConfigRequest("1", "1Gi", "ghcr-pull-secret", kubeconfigBase64, null);

        mockMvc.perform(
                post("/api/admin/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpuLimit").value("1"))
                .andExpect(jsonPath("$.memoryLimit").value("1Gi"))
                .andExpect(jsonPath("$.imagePullSecretName").value("ghcr-pull-secret"))
                .andExpect(jsonPath("$.updatedAt").value("2026-03-29T10:15:00"));
    }

    @Test
    void testUploadAndStoreAdminConfig_FileTooLarge() throws Exception {
        byte[] largeContent = new byte[1_048_577];
        String kubeconfigBase64 = Base64.getEncoder().encodeToString(largeContent);

        AdminConfigRequest request = new AdminConfigRequest(null, null, kubeconfigBase64, null);

        mockMvc.perform(
                post("/api/admin/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("Kubeconfig file size exceeds 1 MB limit."));
    }

    @Test
    void testUploadAndStoreAdminConfig_InvalidBase64() throws Exception {
        AdminConfigRequest request = new AdminConfigRequest(null, null, "not-valid-base64!!!", null);

        mockMvc.perform(
                post("/api/admin/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("Kubeconfig is not valid base64."));
    }

    @Test
    void testGetAdminConfig_ReturnsConfigWhenPresent() throws Exception {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 29, 11, 30, 0);
        AdminConfig adminConfig =
                new AdminConfig(UUID.randomUUID(), "2", "2Gi", "ghcr-pull-secret", "content", 3600, updatedAt);

        when(adminConfigurationService.getAdminConfiguration()).thenReturn(Optional.of(adminConfig));

        mockMvc.perform(get("/api/admin/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kubeconfigUploaded").value(true))
                .andExpect(jsonPath("$.cpuLimit").value("2"))
                .andExpect(jsonPath("$.memoryLimit").value("2Gi"))
                .andExpect(jsonPath("$.imagePullSecretName").value("ghcr-pull-secret"))
                .andExpect(jsonPath("$.updatedAt").value("2026-03-29T11:30:00"));
    }

    @Test
    void testGetAdminConfig_ReturnsEmptyWhenNotPresent() throws Exception {
        when(adminConfigurationService.getAdminConfiguration()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kubeconfigUploaded").value(false))
                .andExpect(jsonPath("$.cpuLimit").isEmpty())
                .andExpect(jsonPath("$.memoryLimit").isEmpty())
                .andExpect(jsonPath("$.imagePullSecretName").isEmpty())
                .andExpect(jsonPath("$.updatedAt").isEmpty());
    }

    @Test
    void testUpdateAdminConfig_Success() throws Exception {
        String kubeconfigBase64 = Base64.getEncoder().encodeToString("updated-content".getBytes());

        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 29, 14, 45, 0);
        AdminConfig adminConfig =
                new AdminConfig(UUID.randomUUID(), "3", "3Gi", "ghcr-pull-secret", "updated-content", 3600, updatedAt);

        when(adminConfigurationService.updateConfiguration(
                        any(byte[].class), eq("3"), eq("3Gi"), eq("ghcr-pull-secret"), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(adminConfig);

        AdminConfigRequest request = new AdminConfigRequest("3", "3Gi", "ghcr-pull-secret", kubeconfigBase64, null);

        mockMvc.perform(
                put("/api/admin/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpuLimit").value("3"))
                .andExpect(jsonPath("$.memoryLimit").value("3Gi"))
                .andExpect(jsonPath("$.imagePullSecretName").value("ghcr-pull-secret"))
                .andExpect(jsonPath("$.updatedAt").value("2026-03-29T14:45:00"));
    }

    @Test
    void testUpdateAdminConfig_FileTooLarge() throws Exception {
        byte[] largeContent = new byte[1_048_577];
        String kubeconfigBase64 = Base64.getEncoder().encodeToString(largeContent);

        AdminConfigRequest request = new AdminConfigRequest(null, null, kubeconfigBase64, null);

        mockMvc.perform(
                put("/api/admin/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("Kubeconfig file size exceeds 1 MB limit."));
    }

    @Test
    void testDeleteAdminConfig_Success() throws Exception {
        mockMvc.perform(delete("/api/admin/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Admin configuration deleted successfully"));

        verify(adminConfigurationService).deleteAdminConfiguration();
    }

}
