package com.pm4.istp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PodCreationRequest {
  @NotBlank(message = "Pod name is required")
  private String podName;

  @NotBlank(message = "Container name is required")
  private String containerName;

  @NotBlank(message = "Image is required")
  private String image;

  @NotNull(message = "Container port is required")
  @Positive(message = "Container port must be a positive integer")
  private int containerPort;
}
