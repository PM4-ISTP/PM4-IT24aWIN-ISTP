package com.pm4.istp.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.pm4.istp.dto.ErrorDto;
import com.pm4.istp.exception.CourseAccessDeniedException;
import com.pm4.istp.exception.CourseNotFoundException;
import com.pm4.istp.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void handleCourseAccessDeniedException_returnsForbidden() {
    ResponseEntity<ErrorDto> response =
        handler.handleCourseAccessDeniedException(new CourseAccessDeniedException("no access"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("Access denied");
  }

  @Test
  void handleCourseNotFoundException_returnsNotFound() {
    ResponseEntity<ErrorDto> response =
        handler.handleCourseNotFoundException(new CourseNotFoundException("missing"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("Course not found");
  }

  @Test
  void handleUserNotFoundException_returnsBadRequest() {
    ResponseEntity<ErrorDto> response =
        handler.handleUserNotFoundException(new UserNotFoundException("unknown user"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("User not found");
  }

  @Test
  void handleException_returnsInternalServerError() {
    ResponseEntity<ErrorDto> response = handler.handleExcception(new RuntimeException("boom"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("An unknown error occurred");
  }
}
