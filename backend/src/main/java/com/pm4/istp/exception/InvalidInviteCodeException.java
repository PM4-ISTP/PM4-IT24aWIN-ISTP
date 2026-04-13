package com.pm4.istp.exception;

public class InvalidInviteCodeException extends IstpException {

  public InvalidInviteCodeException() {}

  public InvalidInviteCodeException(String message) {
    super(message);
  }

  public InvalidInviteCodeException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidInviteCodeException(Throwable cause) {
    super(cause);
  }

  public InvalidInviteCodeException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
