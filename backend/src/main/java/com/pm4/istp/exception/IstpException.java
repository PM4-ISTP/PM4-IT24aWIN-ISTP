package com.pm4.istp.exception;

public class IstpException extends RuntimeException {

  public IstpException() {
  }

  public IstpException(String message) {
    super(message);
  }

  public IstpException(String message, Throwable cause) {
    super(message, cause);
  }

  public IstpException(Throwable cause) {
    super(cause);
  }

  public IstpException(String message, Throwable cause, boolean enableSuppression,
                       boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
