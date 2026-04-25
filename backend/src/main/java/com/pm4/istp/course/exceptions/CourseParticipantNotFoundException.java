package com.pm4.istp.course.exceptions;

import com.pm4.istp.shared.IstpException;

public class CourseParticipantNotFoundException extends IstpException {

  public CourseParticipantNotFoundException() {}

  public CourseParticipantNotFoundException(String message) {
    super(message);
  }

  public CourseParticipantNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public CourseParticipantNotFoundException(Throwable cause) {
    super(cause);
  }

  public CourseParticipantNotFoundException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}

