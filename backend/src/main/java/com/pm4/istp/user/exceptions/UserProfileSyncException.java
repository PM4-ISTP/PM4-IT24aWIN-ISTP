package com.pm4.istp.user.exceptions;

public class UserProfileSyncException extends RuntimeException {
  public UserProfileSyncException(String message) {
    super(message);
  }

  public UserProfileSyncException(String message, Throwable cause) {
    super(message, cause);
  }
}
