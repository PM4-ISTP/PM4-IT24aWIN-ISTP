package com.pm4.istp.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminConfigResponse {
    private boolean kubeconfigUploaded;
    private String cpuLimit;
    private String memoryLimit;
    private LocalDateTime updatedAt;
}
