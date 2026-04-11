package com.pm4.istp.exception;

public class ChallengeAccessDeniedException extends IstpException {

  public ChallengeAccessDeniedException() {}

  public ChallengeAccessDeniedException(String message) {
    super(message);
  }

  public ChallengeAccessDeniedException(String message, Throwable cause) {
    super(message, cause);
  }

  public ChallengeAccessDeniedException(Throwable cause) {
    super(cause);
  }

  public ChallengeAccessDeniedException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
