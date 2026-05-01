package com.pm4.istp.course.exceptions;

import com.pm4.istp.shared.IstpException;

public class ChallengeNotFoundException extends IstpException {

  public ChallengeNotFoundException() {}

  public ChallengeNotFoundException(String message) {
    super(message);
  }

  public ChallengeNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public ChallengeNotFoundException(Throwable cause) {
    super(cause);
  }

  public ChallengeNotFoundException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
