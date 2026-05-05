package com.pm4.istp.course.exceptions;

import com.pm4.istp.shared.IstpException;

public class InvalidCourseLabException extends IstpException {
  public InvalidCourseLabException() {}

  public InvalidCourseLabException(String message) {
    super(message);
  }

  public InvalidCourseLabException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidCourseLabException(Throwable cause) {
    super(cause);
  }

  public InvalidCourseLabException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
