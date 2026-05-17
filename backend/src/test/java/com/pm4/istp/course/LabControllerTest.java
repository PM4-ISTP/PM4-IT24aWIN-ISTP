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
import com.pm4.istp.course.dto.LabStudentDto;
import com.pm4.istp.course.dto.CreateLabRequestDto;
import com.pm4.istp.course.dto.CreateLabResponseDto;
import com.pm4.istp.course.dto.LabDetailResponseDto;
import com.pm4.istp.course.dto.ListLabResponseDto;
import com.pm4.istp.course.dto.ChallengeSubmissionRequestDto;
import com.pm4.istp.course.dto.ChallengeSubmissionResponseDto;
import com.pm4.istp.course.dto.ChoiceSubmissionRequestDto;
import com.pm4.istp.course.dto.ChoiceSubmissionResponseDto;
import com.pm4.istp.course.dto.DockerImageCheckResponseDto;
import com.pm4.istp.course.dto.UpdateLabRequestDto;
import com.pm4.istp.course.dto.VisibilityImpactResponseDto;
import com.pm4.istp.course.exceptions.LabAccessDeniedException;
import com.pm4.istp.course.exceptions.LabNotFoundException;
import com.pm4.istp.course.exceptions.ChallengeAlreadySolvedException;
import com.pm4.istp.course.exceptions.ChallengeNotFoundException;
import com.pm4.istp.course.mappers.LabMapper;
import com.pm4.istp.course.services.DockerImageAvailabilityService;
import com.pm4.istp.course.services.DockerImageAvailabilityService.DockerImageAvailabilityResult;
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
class LabControllerTest {

  @Mock
  private LabService labService;
  @Mock
  private LabMapper labMapper;
  @Mock
  private DockerImageAvailabilityService dockerImageAvailabilityService;

  @InjectMocks
  private LabController labController;

  private Jwt jwtFor(UUID userId) {
    Jwt jwt = mock(Jwt.class);
    when(jwt.getSubject()).thenReturn(userId.toString());
    return jwt;
  }

  @Test
  void createLab_returnsCreatedResponse() {
    UUID userId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    CreateLabRequestDto requestDto = new CreateLabRequestDto();
    requestDto.setTitle("Title");
    requestDto.setStatus(LabStatusEnum.DRAFT);
    requestDto.setDifficulty(LabDifficultyEnum.EASY);

    CreateLabRequest mappedRequest = new CreateLabRequest();
    Lab created = new Lab();
    created.setId(UUID.randomUUID());
    CreateLabResponseDto responseDto = new CreateLabResponseDto();
    responseDto.setId(created.getId());

    when(labMapper.fromDto(requestDto)).thenReturn(mappedRequest);
    when(labService.createLab(userId, mappedRequest)).thenReturn(created);
    when(labMapper.toCreateResponseDto(created)).thenReturn(responseDto);

    ResponseEntity<CreateLabResponseDto> response = labController.createLab(jwt, requestDto);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isSameAs(responseDto);
    verify(labService).createLab(userId, mappedRequest);
  }

  @Test
  void getLab_returnsDtoWithCourseCount() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    Lab lab = new Lab();
    lab.setId(labId);
    lab.setCreator(new User());
    lab.setCourseLabs(List.of(new CourseLab(), new CourseLab()));

    LabDetailResponseDto dto = new LabDetailResponseDto();

    when(labService.getLab(userId, labId)).thenReturn(lab);
    when(labMapper.toDetailResponseDto(lab)).thenReturn(dto);

    ResponseEntity<LabDetailResponseDto> response = labController.getLab(jwt, labId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(dto);
    assertThat(response.getBody().getCourseCount()).isEqualTo(2L);
  }

  @Test
  void getLab_whenServiceThrowsAccessDenied_propagatesException() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    when(labService.getLab(userId, labId))
        .thenThrow(new LabAccessDeniedException("nope"));

    assertThatThrownBy(() -> labController.getLab(jwt, labId))
        .isInstanceOf(LabAccessDeniedException.class);
  }

  @Test
  void updateLab_returnsUpdatedDto() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    UpdateLabRequestDto requestDto = new UpdateLabRequestDto();
    UpdateLabRequest mappedRequest = new UpdateLabRequest();
    Lab updated = new Lab();
    updated.setId(labId);
    updated.setCourseLabs(List.of(new CourseLab()));
    LabDetailResponseDto dto = new LabDetailResponseDto();

    when(labMapper.fromDto(requestDto)).thenReturn(mappedRequest);
    when(labService.updateLab(userId, labId, mappedRequest)).thenReturn(updated);
    when(labMapper.toDetailResponseDto(updated)).thenReturn(dto);

    ResponseEntity<LabDetailResponseDto> response =
        labController.updateLab(jwt, labId, requestDto);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(dto);
    assertThat(response.getBody().getCourseCount()).isEqualTo(1L);
  }

  @Test
  void deleteLab_returnsNoContent() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    ResponseEntity<Void> response = labController.deleteLab(jwt, labId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(labService).deleteLab(userId, labId);
  }

  @Test
  void deleteLab_whenServiceThrowsNotFound_propagatesException() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    org.mockito.Mockito.doThrow(new LabNotFoundException("missing"))
        .when(labService)
        .deleteLab(userId, labId);

    assertThatThrownBy(() -> labController.deleteLab(jwt, labId))
        .isInstanceOf(LabNotFoundException.class);
  }

  @Test
  void listLabs_returnsPageFromService() {
    UUID userId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);
    Pageable pageable = PageRequest.of(0, 20);
    Page<ListLabResponseDto> expected = new PageImpl<>(List.of());

    when(labService.listLabsForCreator(userId, pageable)).thenReturn(expected);

    ResponseEntity<Page<ListLabResponseDto>> response = labController.listLabs(jwt, pageable);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(expected);
  }

  @Test
  void searchLabs_delegatesToService() {
    UUID userId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);
    Pageable pageable = PageRequest.of(0, 20);
    Page<ListLabResponseDto> expected = new PageImpl<>(List.of());

    when(labService.searchAvailableLabs(eq(userId), eq("sql"), any(Pageable.class)))
        .thenReturn(expected);

    ResponseEntity<Page<ListLabResponseDto>> response = labController.searchLabs(jwt, "sql",
        pageable);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(expected);
    verify(labService).searchAvailableLabs(userId, "sql", pageable);
  }

  @Test
  void getVisibilityImpact_returnsAffectedCount() {
    UUID userId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    when(labService.previewVisibilityImpact(userId, labId, LabStatusEnum.DRAFT))
        .thenReturn(5);

    ResponseEntity<VisibilityImpactResponseDto> response = labController.getVisibilityImpact(jwt, labId,
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
        () -> labController.getVisibilityImpact(
            jwt, labId, LabStatusEnum.PRIVATE))
        .isInstanceOf(LabAccessDeniedException.class);
  }

  // ── getLabForPlay ────────────────────────────────────────────────────

  @Test
  void getLabForPlay_returnsStudentDto() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    LabStudentDto dto = new LabStudentDto();
    dto.setId(labId);

    when(labService.getLabForPlay(userId, courseId, labId)).thenReturn(dto);

    ResponseEntity<LabStudentDto> response =
        labController.getLabForPlay(jwt, labId, courseId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(dto);
  }

  @Test
  void getLabForPlay_whenNotEnrolled_propagatesAccessDenied() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    when(labService.getLabForPlay(userId, courseId, labId))
        .thenThrow(new LabAccessDeniedException("not enrolled"));

    assertThatThrownBy(
        () -> labController.getLabForPlay(jwt, labId, courseId))
        .isInstanceOf(LabAccessDeniedException.class);
  }

  @Test
  void getLabForPlay_whenLabNotFound_propagatesNotFound() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    when(labService.getLabForPlay(userId, courseId, labId))
        .thenThrow(new LabNotFoundException("missing"));

    assertThatThrownBy(
        () -> labController.getLabForPlay(jwt, labId, courseId))
        .isInstanceOf(LabNotFoundException.class);
  }

  // ── submitChallengeFlag ──────────────────────────────────────────────────────

  @Test
  void submitChallengeFlag_returnsSubmissionResult() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    ChallengeSubmissionRequestDto request = new ChallengeSubmissionRequestDto(courseId, "ISTP{ok}");
    ChallengeSubmissionResponseDto result = new ChallengeSubmissionResponseDto(true, false, 1, 3);

    when(labService.submitChallengeFlag(userId, courseId, labId, challengeId, "ISTP{ok}"))
        .thenReturn(result);

    ResponseEntity<ChallengeSubmissionResponseDto> response =
        labController.submitChallengeFlag(jwt, labId, challengeId, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(result);
    verify(labService).submitChallengeFlag(userId, courseId, labId, challengeId, "ISTP{ok}");
  }

  @Test
  void submitChallengeFlag_returnsIncorrectResult() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    ChallengeSubmissionRequestDto request = new ChallengeSubmissionRequestDto(courseId, "ISTP{wrong}");
    ChallengeSubmissionResponseDto result = new ChallengeSubmissionResponseDto(false, false, 0, 3);

    when(labService.submitChallengeFlag(userId, courseId, labId, challengeId, "ISTP{wrong}"))
        .thenReturn(result);

    ResponseEntity<ChallengeSubmissionResponseDto> response =
        labController.submitChallengeFlag(jwt, labId, challengeId, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().isCorrect()).isFalse();
  }

  @Test
  void submitChallengeFlag_whenNotEnrolled_propagatesAccessDenied() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    ChallengeSubmissionRequestDto request = new ChallengeSubmissionRequestDto(courseId, "ISTP{x}");

    when(labService.submitChallengeFlag(userId, courseId, labId, challengeId, "ISTP{x}"))
        .thenThrow(new LabAccessDeniedException("not enrolled"));

    assertThatThrownBy(
        () -> labController.submitChallengeFlag(jwt, labId, challengeId, request))
        .isInstanceOf(LabAccessDeniedException.class);
  }

  @Test
  void submitChallengeFlag_whenChallengeNotFound_propagatesNotFound() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    ChallengeSubmissionRequestDto request = new ChallengeSubmissionRequestDto(courseId, "ISTP{x}");

    when(labService.submitChallengeFlag(userId, courseId, labId, challengeId, "ISTP{x}"))
        .thenThrow(new ChallengeNotFoundException("missing"));

    assertThatThrownBy(
        () -> labController.submitChallengeFlag(jwt, labId, challengeId, request))
        .isInstanceOf(ChallengeNotFoundException.class);
  }

  @Test
  void submitChallengeFlag_whenAlreadySolved_propagatesConflict() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    ChallengeSubmissionRequestDto request = new ChallengeSubmissionRequestDto(courseId, "ISTP{x}");

    when(labService.submitChallengeFlag(userId, courseId, labId, challengeId, "ISTP{x}"))
        .thenThrow(new ChallengeAlreadySolvedException("already done"));

    assertThatThrownBy(
        () -> labController.submitChallengeFlag(jwt, labId, challengeId, request))
        .isInstanceOf(ChallengeAlreadySolvedException.class);
  }

  @Test
  void checkDockerImage_returnsPublicAndPrivateMessages() {
    when(dockerImageAvailabilityService.checkImageAvailability("ghcr.io/acme/lab:latest"))
        .thenReturn(DockerImageAvailabilityResult.publicGhcrImage());
    when(dockerImageAvailabilityService.checkImageAvailability("ghcr.io/acme/private:latest"))
        .thenReturn(DockerImageAvailabilityResult.privateGhcrImage());

    ResponseEntity<DockerImageCheckResponseDto> publicResponse =
        labController.checkDockerImage("ghcr.io/acme/lab:latest");
    ResponseEntity<DockerImageCheckResponseDto> privateResponse =
        labController.checkDockerImage("ghcr.io/acme/private:latest");

    assertThat(publicResponse.getBody()).isNotNull();
    assertThat(publicResponse.getBody().message()).isEqualTo("Public GHCR image found");
    assertThat(privateResponse.getBody()).isNotNull();
    assertThat(privateResponse.getBody().message()).contains("image pull secret");
  }

  @Test
  void submitChallengeChoiceAndCompleteTheoryAndCompletedCount_delegateToService() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID labId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID optionId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);
    ChoiceSubmissionRequestDto choiceRequest = new ChoiceSubmissionRequestDto(optionId, courseId);
    ChoiceSubmissionResponseDto choiceResponse =
        new ChoiceSubmissionResponseDto(true, true, 2, 2, null);
    ChallengeSubmissionResponseDto theoryResponse =
        new ChallengeSubmissionResponseDto(true, true, 2, 2);

    when(labService.submitChallengeChoice(userId, courseId, labId, challengeId, optionId))
        .thenReturn(choiceResponse);
    when(labService.completeTheoryChallenge(userId, courseId, labId, challengeId))
        .thenReturn(theoryResponse);
    when(labService.countCompletedLabs(userId)).thenReturn(7L);

    assertThat(
            labController
                .submitChallengeChoice(jwt, labId, challengeId, choiceRequest)
                .getBody())
        .isSameAs(choiceResponse);
    assertThat(labController.completeTheoryChallenge(jwt, labId, challengeId, courseId).getBody())
        .isSameAs(theoryResponse);
    assertThat(labController.countMyCompletedLabs(jwt).getBody()).containsEntry("count", 7L);
  }
}
