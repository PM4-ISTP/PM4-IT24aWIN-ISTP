package com.pm4.istp.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminTopicRequest {
  @NotBlank(message = "Topic value is required")
  @Size(min = 3, max = 24, message = "Topic must be between 3 and 24 characters")
  private String value;
}
