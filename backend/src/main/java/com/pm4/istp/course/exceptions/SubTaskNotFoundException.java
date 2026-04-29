package com.pm4.istp.course.exceptions;

import com.pm4.istp.shared.IstpException;

public class SubTaskNotFoundException extends IstpException {

  public SubTaskNotFoundException() {}

  public SubTaskNotFoundException(String message) {
    super(message);
  }

  public SubTaskNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public SubTaskNotFoundException(Throwable cause) {
    super(cause);
  }

  public SubTaskNotFoundException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
