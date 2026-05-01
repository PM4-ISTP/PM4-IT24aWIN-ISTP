package com.pm4.istp.course.exceptions;

import com.pm4.istp.shared.IstpException;

public class CourseNotFoundException extends IstpException {

  public CourseNotFoundException() {}

  public CourseNotFoundException(String message) {
    super(message);
  }

  public CourseNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public CourseNotFoundException(Throwable cause) {
    super(cause);
  }

  public CourseNotFoundException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
