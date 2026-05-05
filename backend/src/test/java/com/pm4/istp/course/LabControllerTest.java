package com.pm4.istp.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.course.controllers.LabController;
import com.pm4.istp.course.db.CreateLabRequest;
import com.pm4.istp.course.db.UpdateLabRequest;
import com.pm4.istp.course.db.entities.Lab;
import com.pm4.istp.course.db.entities.LabDifficultyEnum;
import com.pm4.istp.course.db.entities.LabStatusEnum;
import com.pm4.istp.course.db.entities.CourseLab;
import com.pm4.istp.course.dto.ChallengeDetailResponseDto;
import com.pm4.istp.course.dto.LabStudentDto;
import com.pm4.istp.course.dto.CreateChallengeRequestDto;
import com.pm4.istp.course.dto.CreateChallengeResponseDto;
import com.pm4.istp.course.dto.ListLabResponseDto;
import com.pm4.istp.course.dto.ChallengeSubmissionRequestDto;
import com.pm4.istp.course.dto.ChallengeSubmissionResponseDto;
import com.pm4.istp.course.dto.UpdateChallengeRequestDto;
import com.pm4.istp.course.dto.VisibilityImpactResponseDto;
import com.pm4.istp.course.exceptions.LabAccessDeniedException;
import com.pm4.istp.course.exceptions.LabNotFoundException;
import com.pm4.istp.course.exceptions.ChallengeAlreadySolvedException;
import com.pm4.istp.course.exceptions.ChallengeNotFoundException;
import com.pm4.istp.course.mappers.LabMapper;
import com.pm4.istp.course.services.LabService;
import com.pm4.istp.user.db.entities.User;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class ChallengeControllerTest {

  @Mock
  private LabService labService;
  @Mock
  private LabMapper labMapper;

  @InjectMocks
  private LabController challengeController;

  private Jwt jwtFor(UUID userId) {
    Jwt jwt = mock(Jwt.class);
    when(jwt.getSubject()).thenReturn(userId.toString());
    return jwt;
  }

  @Test
  void createChallenge_returnsCreatedResponse() {
    UUID userId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    CreateChallengeRequestDto requestDto = new CreateChallengeRequestDto();
    requestDto.setTitle("Title");
    requestDto.setStatus(LabStatusEnum.DRAFT);
    requestDto.setDifficulty(LabDifficultyEnum.EASY);

    CreateLabRequest mappedRequest = new CreateLabRequest();
    Lab created = new Lab();
    created.setId(UUID.randomUUID());
    CreateChallengeResponseDto responseDto = new CreateChallengeResponseDto();
    responseDto.setId(created.getId());

    when(labMapper.fromDto(requestDto)).thenReturn(mappedRequest);
    when(labService.createChallenge(userId, mappedRequest)).thenReturn(created);
    when(labMapper.toCreateResponseDto(created)).thenReturn(responseDto);

    ResponseEntity<CreateChallengeResponseDto> response = challengeController.createChallenge(jwt, requestDto);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isSameAs(responseDto);
    verify(labService).createChallenge(userId, mappedRequest);
  }

  @Test
  void getChallenge_returnsDtoWithCourseCount() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    Lab lab = new Lab();
    lab.setId(labId);
    lab.setCreator(new User());
    lab.setCourseLabs(List.of(new CourseLab(), new CourseLab()));

    ChallengeDetailResponseDto dto = new ChallengeDetailResponseDto();

    when(labService.getChallenge(userId, labId)).thenReturn(lab);
    when(labMapper.toDetailResponseDto(lab)).thenReturn(dto);

    ResponseEntity<ChallengeDetailResponseDto> response = challengeController.getChallenge(jwt, labId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(dto);
    assertThat(response.getBody().getCourseCount()).isEqualTo(2L);
  }

  @Test
  void getChallenge_whenServiceThrowsAccessDenied_propagatesException() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    when(labService.getChallenge(userId, labId))
        .thenThrow(new LabAccessDeniedException("nope"));

    assertThatThrownBy(() -> challengeController.getChallenge(jwt, labId))
        .isInstanceOf(LabAccessDeniedException.class);
  }

  @Test
  void updateChallenge_returnsUpdatedDto() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    UpdateChallengeRequestDto requestDto = new UpdateChallengeRequestDto();
    UpdateLabRequest mappedRequest = new UpdateLabRequest();
    Lab updated = new Lab();
    updated.setId(labId);
    updated.setCourseLabs(List.of(new CourseLab()));
    ChallengeDetailResponseDto dto = new ChallengeDetailResponseDto();

    when(labMapper.fromDto(requestDto)).thenReturn(mappedRequest);
    when(labService.updateChallenge(userId, labId, mappedRequest)).thenReturn(updated);
    when(labMapper.toDetailResponseDto(updated)).thenReturn(dto);

    ResponseEntity<ChallengeDetailResponseDto> response = challengeController.updateChallenge(jwt, labId,
        requestDto);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(dto);
    assertThat(response.getBody().getCourseCount()).isEqualTo(1L);
  }

  @Test
  void deleteChallenge_returnsNoContent() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    ResponseEntity<Void> response = challengeController.deleteChallenge(jwt, labId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(labService).deleteChallenge(userId, labId);
  }

  @Test
  void deleteChallenge_whenServiceThrowsNotFound_propagatesException() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    org.mockito.Mockito.doThrow(new LabNotFoundException("missing"))
        .when(labService)
        .deleteChallenge(userId, labId);

    assertThatThrownBy(() -> challengeController.deleteChallenge(jwt, labId))
        .isInstanceOf(LabNotFoundException.class);
  }

  @Test
  void listChallenges_returnsPageFromService() {
    UUID userId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);
    Pageable pageable = PageRequest.of(0, 20);
    Page<ListLabResponseDto> expected = new PageImpl<>(List.of());

    when(labService.listChallengesForCreator(userId, pageable)).thenReturn(expected);

    ResponseEntity<Page<ListLabResponseDto>> response = challengeController.listChallenges(jwt, pageable);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(expected);
  }

  @Test
  void searchChallenges_delegatesToService() {
    UUID userId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);
    Pageable pageable = PageRequest.of(0, 20);
    Page<ListLabResponseDto> expected = new PageImpl<>(List.of());

    when(labService.searchAvailableChallenges(eq(userId), eq("sql"), any(Pageable.class)))
        .thenReturn(expected);

    ResponseEntity<Page<ListLabResponseDto>> response = challengeController.searchChallenges(jwt, "sql",
        pageable);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(expected);
    verify(labService).searchAvailableChallenges(userId, "sql", pageable);
  }

  @Test
  void getVisibilityImpact_returnsAffectedCount() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    when(labService.previewVisibilityImpact(userId, labId, LabStatusEnum.DRAFT))
        .thenReturn(5);

    ResponseEntity<VisibilityImpactResponseDto> response = challengeController.getVisibilityImpact(jwt, labId,
        LabStatusEnum.DRAFT);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getAffectedCourseCount()).isEqualTo(5);
  }

  @Test
  void getVisibilityImpact_whenServiceThrowsAccessDenied_propagatesException() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    when(labService.previewVisibilityImpact(userId, labId, LabStatusEnum.PRIVATE))
        .thenThrow(new LabAccessDeniedException("nope"));

    assertThatThrownBy(
        () -> challengeController.getVisibilityImpact(
            jwt, labId, LabStatusEnum.PRIVATE))
        .isInstanceOf(LabAccessDeniedException.class);
  }

  // ── getChallengeForPlay ────────────────────────────────────────────────────

  @Test
  void getChallengeForPlay_returnsStudentDto() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    LabStudentDto dto = new LabStudentDto();
    dto.setId(labId);

    when(labService.getChallengeForPlay(userId, courseId, labId)).thenReturn(dto);

    ResponseEntity<LabStudentDto> response =
        challengeController.getChallengeForPlay(jwt, labId, courseId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(dto);
  }

  @Test
  void getChallengeForPlay_whenNotEnrolled_propagatesAccessDenied() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    when(labService.getChallengeForPlay(userId, courseId, labId))
        .thenThrow(new LabAccessDeniedException("not enrolled"));

    assertThatThrownBy(
        () -> challengeController.getChallengeForPlay(jwt, labId, courseId))
        .isInstanceOf(LabAccessDeniedException.class);
  }

  @Test
  void getChallengeForPlay_whenChallengeNotFound_propagatesNotFound() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    when(labService.getChallengeForPlay(userId, courseId, labId))
        .thenThrow(new LabNotFoundException("missing"));

    assertThatThrownBy(
        () -> challengeController.getChallengeForPlay(jwt, labId, courseId))
        .isInstanceOf(LabNotFoundException.class);
  }

  // ── submitChallengeFlag ──────────────────────────────────────────────────────

  @Test
  void submitChallengeFlag_returnsSubmissionResult() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    ChallengeSubmissionRequestDto request = new ChallengeSubmissionRequestDto("ISTP{ok}");
    ChallengeSubmissionResponseDto result = new ChallengeSubmissionResponseDto(true, false, 1, 3);

    when(labService.submitChallengeFlag(userId, labId, challengeId, "ISTP{ok}"))
        .thenReturn(result);

    ResponseEntity<ChallengeSubmissionResponseDto> response =
        challengeController.submitChallengeFlag(jwt, labId, challengeId, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(result);
    verify(labService).submitChallengeFlag(userId, labId, challengeId, "ISTP{ok}");
  }

  @Test
  void submitChallengeFlag_returnsIncorrectResult() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    ChallengeSubmissionRequestDto request = new ChallengeSubmissionRequestDto("ISTP{wrong}");
    ChallengeSubmissionResponseDto result = new ChallengeSubmissionResponseDto(false, false, 0, 3);

    when(labService.submitChallengeFlag(userId, labId, challengeId, "ISTP{wrong}"))
        .thenReturn(result);

    ResponseEntity<ChallengeSubmissionResponseDto> response =
        challengeController.submitChallengeFlag(jwt, labId, challengeId, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().isCorrect()).isFalse();
  }

  @Test
  void submitChallengeFlag_whenNotEnrolled_propagatesAccessDenied() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    ChallengeSubmissionRequestDto request = new ChallengeSubmissionRequestDto("ISTP{x}");

    when(labService.submitChallengeFlag(userId, labId, challengeId, "ISTP{x}"))
        .thenThrow(new LabAccessDeniedException("not enrolled"));

    assertThatThrownBy(
        () -> challengeController.submitChallengeFlag(jwt, labId, challengeId, request))
        .isInstanceOf(LabAccessDeniedException.class);
  }

  @Test
  void submitChallengeFlag_whenChallengeNotFound_propagatesNotFound() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    ChallengeSubmissionRequestDto request = new ChallengeSubmissionRequestDto("ISTP{x}");

    when(labService.submitChallengeFlag(userId, labId, challengeId, "ISTP{x}"))
        .thenThrow(new ChallengeNotFoundException("missing"));

    assertThatThrownBy(
        () -> challengeController.submitChallengeFlag(jwt, labId, challengeId, request))
        .isInstanceOf(ChallengeNotFoundException.class);
  }

  @Test
  void submitChallengeFlag_whenAlreadySolved_propagatesConflict() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    ChallengeSubmissionRequestDto request = new ChallengeSubmissionRequestDto("ISTP{x}");

    when(labService.submitChallengeFlag(userId, labId, challengeId, "ISTP{x}"))
        .thenThrow(new ChallengeAlreadySolvedException("already done"));

    assertThatThrownBy(
        () -> challengeController.submitChallengeFlag(jwt, labId, challengeId, request))
        .isInstanceOf(ChallengeAlreadySolvedException.class);
  }
}
