package com.pm4.istp.exception;

public class InvalidCourseCollaboratorException extends IstpException {
  public InvalidCourseCollaboratorException() {}

  public InvalidCourseCollaboratorException(String message) {
    super(message);
  }

  public InvalidCourseCollaboratorException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidCourseCollaboratorException(Throwable cause) {
    super(cause);
  }

  public InvalidCourseCollaboratorException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
