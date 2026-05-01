package com.pm4.istp.course.exceptions;

import com.pm4.istp.shared.IstpException;

public class InvalidCourseChallengeException extends IstpException {
  public InvalidCourseChallengeException() {}

  public InvalidCourseChallengeException(String message) {
    super(message);
  }

  public InvalidCourseChallengeException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidCourseChallengeException(Throwable cause) {
    super(cause);
  }

  public InvalidCourseChallengeException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
