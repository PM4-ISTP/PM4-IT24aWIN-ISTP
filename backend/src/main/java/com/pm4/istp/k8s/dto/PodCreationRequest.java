package com.pm4.istp.k8s.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PodCreationRequest {
  private String containerName = "app";

  @NotBlank(message = "Image is required")
  private String image;

  @Positive(message = "Container port must be a positive integer")
  private int containerPort;
}
