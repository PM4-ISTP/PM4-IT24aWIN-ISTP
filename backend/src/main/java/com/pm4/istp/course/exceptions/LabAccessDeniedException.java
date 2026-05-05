package com.pm4.istp.course.exceptions;

import com.pm4.istp.shared.IstpException;

public class LabAccessDeniedException extends IstpException {

  public LabAccessDeniedException() {}

  public LabAccessDeniedException(String message) {
    super(message);
  }

  public LabAccessDeniedException(String message, Throwable cause) {
    super(message, cause);
  }

  public LabAccessDeniedException(Throwable cause) {
    super(cause);
  }

  public LabAccessDeniedException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
