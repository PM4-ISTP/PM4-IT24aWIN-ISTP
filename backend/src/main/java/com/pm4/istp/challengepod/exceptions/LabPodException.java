package com.pm4.istp.challengepod.exceptions;

public class LabPodException extends RuntimeException {
  public LabPodException(String message) {
    super(message);
  }

  public LabPodException(String message, Throwable cause) {
    super(message, cause);
  }
}
