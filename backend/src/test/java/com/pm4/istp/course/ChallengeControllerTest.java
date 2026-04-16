package com.pm4.istp.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.course.controllers.ChallengeController;
import com.pm4.istp.course.db.CreateChallengeRequest;
import com.pm4.istp.course.db.UpdateChallengeRequest;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.ChallengeDifficultyEnum;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import com.pm4.istp.course.db.entities.CourseChallenge;
import com.pm4.istp.course.dto.ChallengeDetailResponseDto;
import com.pm4.istp.course.dto.CreateChallengeRequestDto;
import com.pm4.istp.course.dto.CreateChallengeResponseDto;
import com.pm4.istp.course.dto.ListChallengeResponseDto;
import com.pm4.istp.course.dto.UpdateChallengeRequestDto;
import com.pm4.istp.course.dto.VisibilityImpactResponseDto;
import com.pm4.istp.course.exceptions.ChallengeAccessDeniedException;
import com.pm4.istp.course.exceptions.ChallengeNotFoundException;
import com.pm4.istp.course.mappers.ChallengeMapper;
import com.pm4.istp.course.services.ChallengeService;
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
  private ChallengeService challengeService;
  @Mock
  private ChallengeMapper challengeMapper;

  @InjectMocks
  private ChallengeController challengeController;

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
    requestDto.setStatus(ChallengeStatusEnum.DRAFT);
    requestDto.setDifficulty(ChallengeDifficultyEnum.EASY);

    CreateChallengeRequest mappedRequest = new CreateChallengeRequest();
    Challenge created = new Challenge();
    created.setId(UUID.randomUUID());
    CreateChallengeResponseDto responseDto = new CreateChallengeResponseDto();
    responseDto.setId(created.getId());

    when(challengeMapper.fromDto(requestDto)).thenReturn(mappedRequest);
    when(challengeService.createChallenge(userId, mappedRequest)).thenReturn(created);
    when(challengeMapper.toCreateResponseDto(created)).thenReturn(responseDto);

    ResponseEntity<CreateChallengeResponseDto> response = challengeController.createChallenge(jwt, requestDto);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isSameAs(responseDto);
    verify(challengeService).createChallenge(userId, mappedRequest);
  }

  @Test
  void getChallenge_returnsDtoWithCourseCount() {
    UUID userId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    Challenge challenge = new Challenge();
    challenge.setId(challengeId);
    challenge.setCreator(new User());
    challenge.setCourseChallenges(List.of(new CourseChallenge(), new CourseChallenge()));

    ChallengeDetailResponseDto dto = new ChallengeDetailResponseDto();

    when(challengeService.getChallenge(userId, challengeId)).thenReturn(challenge);
    when(challengeMapper.toDetailResponseDto(challenge)).thenReturn(dto);

    ResponseEntity<ChallengeDetailResponseDto> response = challengeController.getChallenge(jwt, challengeId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(dto);
    assertThat(response.getBody().getCourseCount()).isEqualTo(2L);
  }

  @Test
  void getChallenge_whenServiceThrowsAccessDenied_propagatesException() {
    UUID userId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    when(challengeService.getChallenge(userId, challengeId))
        .thenThrow(new ChallengeAccessDeniedException("nope"));

    assertThatThrownBy(() -> challengeController.getChallenge(jwt, challengeId))
        .isInstanceOf(ChallengeAccessDeniedException.class);
  }

  @Test
  void updateChallenge_returnsUpdatedDto() {
    UUID userId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    UpdateChallengeRequestDto requestDto = new UpdateChallengeRequestDto();
    UpdateChallengeRequest mappedRequest = new UpdateChallengeRequest();
    Challenge updated = new Challenge();
    updated.setId(challengeId);
    updated.setCourseChallenges(List.of(new CourseChallenge()));
    ChallengeDetailResponseDto dto = new ChallengeDetailResponseDto();

    when(challengeMapper.fromDto(requestDto)).thenReturn(mappedRequest);
    when(challengeService.updateChallenge(userId, challengeId, mappedRequest)).thenReturn(updated);
    when(challengeMapper.toDetailResponseDto(updated)).thenReturn(dto);

    ResponseEntity<ChallengeDetailResponseDto> response = challengeController.updateChallenge(jwt, challengeId,
        requestDto);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(dto);
    assertThat(response.getBody().getCourseCount()).isEqualTo(1L);
  }

  @Test
  void deleteChallenge_returnsNoContent() {
    UUID userId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    ResponseEntity<Void> response = challengeController.deleteChallenge(jwt, challengeId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(challengeService).deleteChallenge(userId, challengeId);
  }

  @Test
  void deleteChallenge_whenServiceThrowsNotFound_propagatesException() {
    UUID userId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    org.mockito.Mockito.doThrow(new ChallengeNotFoundException("missing"))
        .when(challengeService)
        .deleteChallenge(userId, challengeId);

    assertThatThrownBy(() -> challengeController.deleteChallenge(jwt, challengeId))
        .isInstanceOf(ChallengeNotFoundException.class);
  }

  @Test
  void listChallenges_returnsPageFromService() {
    UUID userId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);
    Pageable pageable = PageRequest.of(0, 20);
    Page<ListChallengeResponseDto> expected = new PageImpl<>(List.of());

    when(challengeService.listChallengesForCreator(userId, pageable)).thenReturn(expected);

    ResponseEntity<Page<ListChallengeResponseDto>> response = challengeController.listChallenges(jwt, pageable);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(expected);
  }

  @Test
  void searchChallenges_delegatesToService() {
    UUID userId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);
    Pageable pageable = PageRequest.of(0, 20);
    Page<ListChallengeResponseDto> expected = new PageImpl<>(List.of());

    when(challengeService.searchAvailableChallenges(eq(userId), eq("sql"), any(Pageable.class)))
        .thenReturn(expected);

    ResponseEntity<Page<ListChallengeResponseDto>> response = challengeController.searchChallenges(jwt, "sql",
        pageable);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(expected);
    verify(challengeService).searchAvailableChallenges(userId, "sql", pageable);
  }

  @Test
  void getVisibilityImpact_returnsAffectedCount() {
    UUID userId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    when(challengeService.previewVisibilityImpact(userId, challengeId, ChallengeStatusEnum.DRAFT))
        .thenReturn(5);

    ResponseEntity<VisibilityImpactResponseDto> response = challengeController.getVisibilityImpact(jwt, challengeId,
        ChallengeStatusEnum.DRAFT);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getAffectedCourseCount()).isEqualTo(5);
  }

  @Test
  void getVisibilityImpact_whenServiceThrowsAccessDenied_propagatesException() {
    UUID userId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    Jwt jwt = jwtFor(userId);

    when(challengeService.previewVisibilityImpact(userId, challengeId, ChallengeStatusEnum.PRIVATE))
        .thenThrow(new ChallengeAccessDeniedException("nope"));

    assertThatThrownBy(
        () -> challengeController.getVisibilityImpact(
            jwt, challengeId, ChallengeStatusEnum.PRIVATE))
        .isInstanceOf(ChallengeAccessDeniedException.class);
  }
}
