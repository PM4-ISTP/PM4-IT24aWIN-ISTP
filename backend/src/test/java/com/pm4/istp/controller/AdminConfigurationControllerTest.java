package com.pm4.istp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pm4.istp.domain.AdminConfig;
import com.pm4.istp.exception.StorageException;
import com.pm4.istp.service.AdminConfigurationService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminConfigurationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminConfigurationService adminConfigurationService;

    @InjectMocks
    private AdminConfigurationController adminConfigurationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminConfigurationController).build();
    }

    @Test
    void testUploadAndStoreAdminConfig_Success() throws Exception {
        MockMultipartFile kubeconfig = new MockMultipartFile(
                "kubeconfig",
                "config.yml",
                MediaType.TEXT_PLAIN_VALUE,
                "content".getBytes());

        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 29, 10, 15, 0);
        AdminConfig adminConfig = new AdminConfig(UUID.randomUUID(), "1", "1Gi", "content", updatedAt);

        when(adminConfigurationService.createConfiguration(any(), eq("1"), eq("1Gi")))
                .thenReturn(adminConfig);

        mockMvc.perform(
                multipart("/api/admin/config")
                        .file(kubeconfig)
                        .param("cpuLimit", "1")
                        .param("memoryLimit", "1Gi"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpuLimit").value("1"))
                .andExpect(jsonPath("$.memoryLimit").value("1Gi"))
                .andExpect(jsonPath("$.updatedAt").value("2026-03-29T10:15:00"));
    }

    @Test
    void testUploadAndStoreAdminConfig_FileTooLarge() throws Exception {
        byte[] largeContent = new byte[1_048_577];
        MockMultipartFile kubeconfig = new MockMultipartFile(
                "kubeconfig",
                "config.yml",
                MediaType.TEXT_PLAIN_VALUE,
                largeContent);

        mockMvc.perform(multipart("/api/admin/config").file(kubeconfig))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Kubeconfig file size exceeds 1 MB limit."));
    }

    @Test
    void testGetAdminConfig_ReturnsConfigWhenPresent() throws Exception {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 29, 11, 30, 0);
        AdminConfig adminConfig = new AdminConfig(UUID.randomUUID(), "2", "2Gi", "content", updatedAt);

        when(adminConfigurationService.getAdminConfiguration()).thenReturn(Optional.of(adminConfig));

        mockMvc.perform(get("/api/admin/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kubeconfigUploaded").value(true))
                .andExpect(jsonPath("$.cpuLimit").value("2"))
                .andExpect(jsonPath("$.memoryLimit").value("2Gi"))
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
                .andExpect(jsonPath("$.updatedAt").isEmpty());
    }

    @Test
    void testUpdateAdminConfig_Success() throws Exception {
        MockMultipartFile kubeconfig = new MockMultipartFile(
                "kubeconfig",
                "config.yml",
                MediaType.TEXT_PLAIN_VALUE,
                "updated-content".getBytes());

        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 29, 14, 45, 0);
        AdminConfig adminConfig = new AdminConfig(UUID.randomUUID(), "3", "3Gi", "updated-content", updatedAt);

        when(adminConfigurationService.updateConfiguration(any(), eq("3"), eq("3Gi")))
                .thenReturn(adminConfig);

        mockMvc.perform(
                MockMvcRequestBuilders.multipart("/api/admin/config")
                        .file(kubeconfig)
                        .param("cpuLimit", "3")
                        .param("memoryLimit", "3Gi")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpuLimit").value("3"))
                .andExpect(jsonPath("$.memoryLimit").value("3Gi"))
                .andExpect(jsonPath("$.updatedAt").value("2026-03-29T14:45:00"));
    }

    @Test
    void testUpdateAdminConfig_FileTooLarge() throws Exception {
        byte[] largeContent = new byte[1_048_577];
        MockMultipartFile kubeconfig = new MockMultipartFile(
                "kubeconfig",
                "config.yml",
                MediaType.TEXT_PLAIN_VALUE,
                largeContent);

        mockMvc.perform(
                MockMvcRequestBuilders.multipart("/api/admin/config")
                        .file(kubeconfig)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Kubeconfig file size exceeds 1 MB limit."));
    }

    @Test
    void testDeleteAdminConfig_Success() throws Exception {
        mockMvc.perform(delete("/api/admin/config"))
                .andExpect(status().isOk())
                .andExpect(content().string("Admin configuration deleted successfully"));

        verify(adminConfigurationService).deleteAdminConfiguration();
    }

    @Test
    void testHandleStorageException_OnCreate() throws Exception {
        MockMultipartFile kubeconfig = new MockMultipartFile("kubeconfig", "config.yml", MediaType.TEXT_PLAIN_VALUE,
                "content".getBytes());

        when(adminConfigurationService.createConfiguration(any(), any(), any()))
                .thenThrow(new StorageException("Failed to store", new RuntimeException()));

        mockMvc.perform(multipart("/api/admin/config").file(kubeconfig))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Storage error: Failed to store"));
    }

    @Test
    void testHandleStorageException_OnUpdate() throws Exception {
        MockMultipartFile kubeconfig = new MockMultipartFile("kubeconfig", "config.yml", MediaType.TEXT_PLAIN_VALUE,
                "content".getBytes());

        when(adminConfigurationService.updateConfiguration(any(), any(), any()))
                .thenThrow(new StorageException("Failed to update", new RuntimeException()));

        mockMvc.perform(
                MockMvcRequestBuilders.multipart("/api/admin/config")
                        .file(kubeconfig)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Storage error: Failed to update"));
    }
}