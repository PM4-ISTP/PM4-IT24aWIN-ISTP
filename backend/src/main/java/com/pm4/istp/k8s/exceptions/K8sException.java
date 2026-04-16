package com.pm4.istp.k8s.exceptions;

public class K8sException extends RuntimeException {
  public K8sException(String message) {
    super(message);
  }

  public K8sException(String message, Throwable cause) {
    super(message, cause);
  }
}
