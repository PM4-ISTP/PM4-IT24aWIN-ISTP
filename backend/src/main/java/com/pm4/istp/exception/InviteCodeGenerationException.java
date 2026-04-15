package com.pm4.istp.exception;

public class InviteCodeGenerationException extends IstpException {

  public InviteCodeGenerationException() {}

  public InviteCodeGenerationException(String message) {
    super(message);
  }

  public InviteCodeGenerationException(String message, Throwable cause) {
    super(message, cause);
  }

  public InviteCodeGenerationException(Throwable cause) {
    super(cause);
  }

  public InviteCodeGenerationException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}

