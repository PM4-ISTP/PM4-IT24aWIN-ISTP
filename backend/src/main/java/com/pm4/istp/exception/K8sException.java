package com.pm4.istp.exception;

public class K8sException extends RuntimeException {
  public K8sException(String message) {
    super(message);
  }

  public K8sException(String message, Throwable cause) {
    super(message, cause);
  }
}
