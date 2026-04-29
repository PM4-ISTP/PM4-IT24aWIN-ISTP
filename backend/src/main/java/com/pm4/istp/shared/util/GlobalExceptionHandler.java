package com.pm4.istp.shared.util;

import com.pm4.istp.challengepod.exceptions.ChallengePodException;
import com.pm4.istp.course.exceptions.ChallengeAccessDeniedException;
import com.pm4.istp.course.exceptions.ChallengeNotFoundException;
import com.pm4.istp.course.exceptions.CourseAccessDeniedException;
import com.pm4.istp.course.exceptions.CourseNotFoundException;
import com.pm4.istp.course.exceptions.CourseParticipantNotFoundException;
import com.pm4.istp.course.exceptions.InvalidCourseChallengeException;
import com.pm4.istp.course.exceptions.InvalidCourseCollaboratorException;
import com.pm4.istp.course.exceptions.InvalidCourseShortDescriptionException;
import com.pm4.istp.course.exceptions.InvalidInviteCodeException;
import com.pm4.istp.course.exceptions.InviteCodeGenerationException;
import com.pm4.istp.course.exceptions.SubTaskAlreadySolvedException;
import com.pm4.istp.course.exceptions.SubTaskNotFoundException;
import com.pm4.istp.shared.dto.ErrorDto;
import com.pm4.istp.user.exceptions.UserNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(ChallengePodException.class)
  public ResponseEntity<ErrorDto> handleChallengePodException(ChallengePodException ex) {
    log.error("Caught ChallengePodException", ex);
    ErrorDto errorDto = new ErrorDto();
    errorDto.setError("Kubernetes operation failed: " + ex.getMessage());
    return new ResponseEntity<>(errorDto, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(ChallengeAccessDeniedException.class)
  public ResponseEntity<ErrorDto> handleChallengeAccessDeniedException(
      ChallengeAccessDeniedException ex) {
    log.error("Caught ChallengeAccessDeniedException", ex);
    ErrorDto errorDto = new ErrorDto();
    errorDto.setError("Access denied");
    return new ResponseEntity<>(errorDto, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(ChallengeNotFoundException.class)
  public ResponseEntity<ErrorDto> handleChallengeNotFoundException(ChallengeNotFoundException ex) {
    log.error("Caught ChallengeNotFoundException", ex);
    ErrorDto errorDto = new ErrorDto();
    errorDto.setError("Challenge not found");
    return new ResponseEntity<>(errorDto, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(CourseAccessDeniedException.class)
  public ResponseEntity<ErrorDto> handleCourseAccessDeniedException(
      CourseAccessDeniedException ex) {
    log.error("Caught CourseAccessDeniedException", ex);
    ErrorDto errorDto = new ErrorDto();
    errorDto.setError("Access denied");
    return new ResponseEntity<>(errorDto, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(CourseNotFoundException.class)
  public ResponseEntity<ErrorDto> handleCourseNotFoundException(CourseNotFoundException ex) {
    log.error("Caught CourseNotFoundException", ex);
    ErrorDto errorDto = new ErrorDto();
    errorDto.setError("Course not found");
    return new ResponseEntity<>(errorDto, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(CourseParticipantNotFoundException.class)
  public ResponseEntity<ErrorDto> handleCourseParticipantNotFoundException(
      CourseParticipantNotFoundException ex) {
    log.error("Caught CourseParticipantNotFoundException", ex);
    ErrorDto errorDto = new ErrorDto();
    errorDto.setError("Participant not found");
    return new ResponseEntity<>(errorDto, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ErrorDto> handleUserNotFoundException(UserNotFoundException ex) {
    log.error("Caught UserNotFoundException", ex);
    ErrorDto errorDto = new ErrorDto();
    errorDto.setError("User not found");
    return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(InvalidInviteCodeException.class)
  public ResponseEntity<ErrorDto> handleInvalidInviteCodeException(InvalidInviteCodeException ex) {
    log.warn("Caught InvalidInviteCodeException: {}", ex.getMessage());
    ErrorDto errorDto = new ErrorDto();
    errorDto.setError("Invalid invite code");
    return new ResponseEntity<>(errorDto, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(InvalidCourseCollaboratorException.class)
  public ResponseEntity<ErrorDto> handleInvalidCourseCollaboratorException(
      InvalidCourseCollaboratorException ex) {
    log.warn("Caught InvalidCourseCollaboratorException: {}", ex.getMessage());
    ErrorDto errorDto = new ErrorDto();
    errorDto.setError(ex.getMessage());
    return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(InvalidCourseShortDescriptionException.class)
  public ResponseEntity<ErrorDto> handleInvalidCourseShortDescriptionException(
      InvalidCourseShortDescriptionException ex) {
    log.warn("Caught InvalidCourseShortDescriptionException: {}", ex.getMessage());
    ErrorDto errorDto = new ErrorDto();
    errorDto.setError(ex.getMessage());
    return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(InvalidCourseChallengeException.class)
  public ResponseEntity<ErrorDto> handleInvalidCourseChallengeException(
      InvalidCourseChallengeException ex) {
    log.warn("Caught InvalidCourseChallengeException: {}", ex.getMessage());
    ErrorDto errorDto = new ErrorDto();
    errorDto.setError(ex.getMessage());
    return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(SubTaskNotFoundException.class)
  public ResponseEntity<ErrorDto> handleSubTaskNotFoundException(SubTaskNotFoundException ex) {
    log.error("Caught SubTaskNotFoundException", ex);
    ErrorDto errorDto = new ErrorDto();
    errorDto.setError("Sub-task not found");
    return new ResponseEntity<>(errorDto, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(SubTaskAlreadySolvedException.class)
  public ResponseEntity<ErrorDto> handleSubTaskAlreadySolvedException(
      SubTaskAlreadySolvedException ex) {
    log.warn("Caught SubTaskAlreadySolvedException: {}", ex.getMessage());
    ErrorDto errorDto = new ErrorDto();
    errorDto.setError("Sub-task already solved");
    return new ResponseEntity<>(errorDto, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(InviteCodeGenerationException.class)
  public ResponseEntity<ErrorDto> handleInviteCodeGenerationException(
      InviteCodeGenerationException ex) {
    log.error("Caught InviteCodeGenerationException", ex);
    ErrorDto errorDto = new ErrorDto();
    errorDto.setError("Could not regenerate invite code");
    return new ResponseEntity<>(errorDto, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorDto> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex) {
    log.error("Caught MethodArgumentNotValidException", ex);
    ErrorDto errorDto = new ErrorDto();

    BindingResult bindingResult = ex.getBindingResult();
    List<FieldError> fieldErrors = bindingResult.getFieldErrors();
    String errorMessage =
        fieldErrors.stream()
            .findFirst()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .orElse("Validation error occurred");

    errorDto.setError(errorMessage);
    return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorDto> handleConstraintViolation(ConstraintViolationException ex) {
    log.error("Caught ConstraintViolationException", ex);
    ErrorDto errorDto = new ErrorDto();

    String errorMessage =
        ex.getConstraintViolations().stream()
            .findFirst()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .orElse("Constraint violation occurred");
    errorDto.setError(errorMessage);
    return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorDto> handleIllegalArgumentException(IllegalArgumentException ex) {
    log.warn("Caught IllegalArgumentException: {}", ex.getMessage());
    ErrorDto errorDto = new ErrorDto();
    errorDto.setError(ex.getMessage() == null ? "Invalid request" : ex.getMessage());
    return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorDto> handleException(Exception ex) {
    log.error("Caught exception", ex);
    ErrorDto errorDto = new ErrorDto();
    errorDto.setError("An unknown error occurred");
    return new ResponseEntity<>(errorDto, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
