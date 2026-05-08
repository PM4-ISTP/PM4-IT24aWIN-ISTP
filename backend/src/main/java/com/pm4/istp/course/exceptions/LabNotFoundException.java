package com.pm4.istp.course.exceptions;

import com.pm4.istp.shared.IstpException;

public class LabNotFoundException extends IstpException {

  public LabNotFoundException() {}

  public LabNotFoundException(String message) {
    super(message);
  }

  public LabNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public LabNotFoundException(Throwable cause) {
    super(cause);
  }

  public LabNotFoundException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
