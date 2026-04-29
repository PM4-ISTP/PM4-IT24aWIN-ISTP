package com.pm4.istp.course.exceptions;

import com.pm4.istp.shared.IstpException;

public class SubTaskAlreadySolvedException extends IstpException {

  public SubTaskAlreadySolvedException() {}

  public SubTaskAlreadySolvedException(String message) {
    super(message);
  }

  public SubTaskAlreadySolvedException(String message, Throwable cause) {
    super(message, cause);
  }

  public SubTaskAlreadySolvedException(Throwable cause) {
    super(cause);
  }

  public SubTaskAlreadySolvedException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
