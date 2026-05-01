package com.pm4.istp.course.exceptions;

import com.pm4.istp.shared.IstpException;

public class CourseAccessDeniedException extends IstpException {

  public CourseAccessDeniedException() {}

  public CourseAccessDeniedException(String message) {
    super(message);
  }

  public CourseAccessDeniedException(String message, Throwable cause) {
    super(message, cause);
  }

  public CourseAccessDeniedException(Throwable cause) {
    super(cause);
  }

  public CourseAccessDeniedException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
