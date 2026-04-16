package com.pm4.istp.challengepod.exceptions;

public class ChallengePodException extends RuntimeException {
  public ChallengePodException(String message) {
    super(message);
  }

  public ChallengePodException(String message, Throwable cause) {
    super(message, cause);
  }
}
