package com.pm4.istp.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminTopicRequest {
  @NotBlank(message = "Topic value is required")
  @Size(max = 255, message = "Topic must be at most 255 characters")
  private String value;
}

