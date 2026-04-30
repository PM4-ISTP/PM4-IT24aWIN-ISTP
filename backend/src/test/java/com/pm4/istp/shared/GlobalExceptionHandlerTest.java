package com.pm4.istp.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.pm4.istp.course.exceptions.ChallengeAccessDeniedException;
import com.pm4.istp.course.exceptions.ChallengeNotFoundException;
import com.pm4.istp.course.exceptions.CourseAccessDeniedException;
import com.pm4.istp.course.exceptions.CourseNotFoundException;
import com.pm4.istp.course.exceptions.InvalidCourseChallengeException;
import com.pm4.istp.course.exceptions.InvalidCourseCollaboratorException;
import com.pm4.istp.course.exceptions.InvalidCourseShortDescriptionException;
import com.pm4.istp.course.exceptions.InviteCodeGenerationException;
import com.pm4.istp.shared.dto.ErrorDto;
import com.pm4.istp.shared.util.GlobalExceptionHandler;
import com.pm4.istp.user.exceptions.UserNotFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void handleCourseAccessDeniedException_returnsForbidden() {
    ResponseEntity<ErrorDto> response = handler
        .handleCourseAccessDeniedException(new CourseAccessDeniedException("no access"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("Access denied");
  }

  @Test
  void handleCourseNotFoundException_returnsNotFound() {
    ResponseEntity<ErrorDto> response = handler.handleCourseNotFoundException(new CourseNotFoundException("missing"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("Course not found");
  }

  @Test
  void handleUserNotFoundException_returnsNotFound() {
    ResponseEntity<ErrorDto> response = handler.handleUserNotFoundException(new UserNotFoundException("unknown user"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("User not found");
  }

  @Test
  void handleException_returnsInternalServerError() {
    ResponseEntity<ErrorDto> response = handler.handleException(new RuntimeException("boom"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("An unknown error occurred");
  }

  @Test
  void handleChallengeAccessDeniedException_returnsForbidden() {
    ResponseEntity<ErrorDto> response = handler.handleChallengeAccessDeniedException(
        new ChallengeAccessDeniedException("no access"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("Access denied");
  }

  @Test
  void handleChallengeNotFoundException_returnsNotFound() {
    ResponseEntity<ErrorDto> response = handler
        .handleChallengeNotFoundException(new ChallengeNotFoundException("missing"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("Challenge not found");
  }

  @Test
  void handleInvalidCourseChallengeException_returnsBadRequestWithMessage() {
    ResponseEntity<ErrorDto> response = handler.handleInvalidCourseChallengeException(
        new InvalidCourseChallengeException("Challenge 'Foo' is a draft"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("Challenge 'Foo' is a draft");
  }

  @Test
  void handleInvalidCourseCollaboratorException_returnsBadRequest() {
    ResponseEntity<ErrorDto> response = handler.handleInvalidCourseCollaboratorException(
        new InvalidCourseCollaboratorException("invalid collaborator"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("invalid collaborator");
  }

  @Test
  void handleInvalidCourseShortDescriptionException_returnsBadRequest() {
    ResponseEntity<ErrorDto> response = handler.handleInvalidCourseShortDescriptionException(
        new InvalidCourseShortDescriptionException("short description too long"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("short description too long");
  }

  @Test
  void handleInviteCodeGenerationException_returnsInternalServerErrorWithSpecificMessage() {
    ResponseEntity<ErrorDto> response = handler.handleInviteCodeGenerationException(
        new InviteCodeGenerationException("could not regenerate"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("Could not regenerate invite code");
  }

  @Test
  void handleMethodArgumentNotValidException_returnsBadRequestWithFieldError() {
    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    BindingResult bindingResult = mock(BindingResult.class);
    FieldError fieldError = new FieldError("dto", "title", "must not be blank");

    when(ex.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

    ResponseEntity<ErrorDto> response = handler.handleMethodArgumentNotValidException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("title: must not be blank");
  }

  @Test
  void handleMethodArgumentNotValidException_withNoFieldErrors_returnsDefaultMessage() {
    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    BindingResult bindingResult = mock(BindingResult.class);

    when(ex.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors()).thenReturn(List.of());

    ResponseEntity<ErrorDto> response = handler.handleMethodArgumentNotValidException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("Validation error occurred");
  }

  @Test
  @SuppressWarnings("unchecked")
  void handleConstraintViolation_returnsBadRequestWithViolationMessage() {
    ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
    Path path = mock(Path.class);

    when(path.toString()).thenReturn("myField");
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn("must not be null");

    ConstraintViolationException ex = new ConstraintViolationException("violations", Set.of(violation));

    ResponseEntity<ErrorDto> response = handler.handleConstraintViolation(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("myField: must not be null");
  }

  @Test
  void handleConstraintViolation_withNoViolations_returnsDefaultMessage() {
    ConstraintViolationException ex = new ConstraintViolationException("no violations", Set.of());

    ResponseEntity<ErrorDto> response = handler.handleConstraintViolation(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("Constraint violation occurred");
  }

  @Test
  void handleIllegalArgumentException_returnsBadRequestWithMessage() {
    ResponseEntity<ErrorDto> response = handler
        .handleIllegalArgumentException(new IllegalArgumentException("invalid visibility"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getError()).isEqualTo("invalid visibility");
  }
}
