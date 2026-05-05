package com.pm4.istp.course.exceptions;

import com.pm4.istp.shared.IstpException;

public class ChallengeAlreadySolvedException extends IstpException {

  public ChallengeAlreadySolvedException() {}

  public ChallengeAlreadySolvedException(String message) {
    super(message);
  }

  public ChallengeAlreadySolvedException(String message, Throwable cause) {
    super(message, cause);
  }

  public ChallengeAlreadySolvedException(Throwable cause) {
    super(cause);
  }

  public ChallengeAlreadySolvedException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
