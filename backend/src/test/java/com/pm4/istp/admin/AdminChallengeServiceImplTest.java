package com.pm4.istp.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.admin.dto.AdminUpdateChallengeRequestDto;
import com.pm4.istp.admin.services.impl.AdminChallengeServiceImpl;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.ChallengeDifficultyEnum;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import com.pm4.istp.course.exceptions.ChallengeNotFoundException;
import com.pm4.istp.course.repositories.ChallengeRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminChallengeServiceImplTest {

  @Mock private ChallengeRepository challengeRepository;

  @InjectMocks private AdminChallengeServiceImpl adminChallengeService;

  // ── listChallenges ──────────────────────────────────────────────────────────

  @Test
  void listChallenges_withNullQuery_callsFindAllWithoutFilter() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<com.pm4.istp.admin.dto.AdminChallengeListItemDto> expected =
        new PageImpl<>(List.of(), pageable, 0);
    when(challengeRepository.findAllChallengesForAdmin(pageable)).thenReturn(expected);

    Page<com.pm4.istp.admin.dto.AdminChallengeListItemDto> result =
        adminChallengeService.listChallenges(null, pageable);

    assertThat(result).isSameAs(expected);
    verify(challengeRepository).findAllChallengesForAdmin(pageable);
    verify(challengeRepository, never()).findAllChallengesForAdminByQuery(any(), any());
  }

  @Test
  void listChallenges_withBlankQuery_treatsAsNullAndCallsFindAll() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<com.pm4.istp.admin.dto.AdminChallengeListItemDto> expected =
        new PageImpl<>(List.of(), pageable, 0);
    when(challengeRepository.findAllChallengesForAdmin(pageable)).thenReturn(expected);

    Page<com.pm4.istp.admin.dto.AdminChallengeListItemDto> result =
        adminChallengeService.listChallenges("   ", pageable);

    assertThat(result).isSameAs(expected);
    verify(challengeRepository).findAllChallengesForAdmin(pageable);
    verify(challengeRepository, never()).findAllChallengesForAdminByQuery(any(), any());
  }

  @Test
  void listChallenges_withNonBlankQuery_callsFindByQuery() {
    Pageable pageable = PageRequest.of(0, 10);
    String query = "sql injection";
    Page<com.pm4.istp.admin.dto.AdminChallengeListItemDto> expected =
        new PageImpl<>(List.of(), pageable, 0);
    when(challengeRepository.findAllChallengesForAdminByQuery(query, pageable))
        .thenReturn(expected);

    Page<com.pm4.istp.admin.dto.AdminChallengeListItemDto> result =
        adminChallengeService.listChallenges(query, pageable);

    assertThat(result).isSameAs(expected);
    verify(challengeRepository).findAllChallengesForAdminByQuery(query, pageable);
    verify(challengeRepository, never()).findAllChallengesForAdmin(any());
  }

  // ── updateChallenge ─────────────────────────────────────────────────────────

  @Test
  void updateChallenge_withValidRequest_updatesAllFields() {
    UUID id = UUID.randomUUID();
    Challenge challenge = new Challenge();
    challenge.setId(id);
    challenge.setMaxScore(0);

    AdminUpdateChallengeRequestDto request = new AdminUpdateChallengeRequestDto();
    request.setTitle("Updated Title");
    request.setShortDescription("  short  ");
    request.setDescription("Full description");
    request.setStatus(ChallengeStatusEnum.PUBLIC);
    request.setDifficulty(ChallengeDifficultyEnum.HARD);
    request.setMaxScore(100);

    when(challengeRepository.findById(id)).thenReturn(Optional.of(challenge));
    when(challengeRepository.save(any(Challenge.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    adminChallengeService.updateChallenge(id, request);

    ArgumentCaptor<Challenge> captor = ArgumentCaptor.forClass(Challenge.class);
    verify(challengeRepository).save(captor.capture());
    Challenge saved = captor.getValue();

    assertThat(saved.getTitle()).isEqualTo("Updated Title");
    assertThat(saved.getShortDescription()).isEqualTo("short");
    assertThat(saved.getDescription()).isEqualTo("Full description");
    assertThat(saved.getStatus()).isEqualTo(ChallengeStatusEnum.PUBLIC);
    assertThat(saved.getDifficulty()).isEqualTo(ChallengeDifficultyEnum.HARD);
    assertThat(saved.getMaxScore()).isEqualTo(100);
  }

  @Test
  void updateChallenge_withNullMaxScore_doesNotOverwriteExistingMaxScore() {
    UUID id = UUID.randomUUID();
    Challenge challenge = new Challenge();
    challenge.setId(id);
    challenge.setMaxScore(42);

    AdminUpdateChallengeRequestDto request = new AdminUpdateChallengeRequestDto();
    request.setTitle("Title");
    request.setStatus(ChallengeStatusEnum.PRIVATE);
    request.setDifficulty(ChallengeDifficultyEnum.EASY);
    request.setMaxScore(null);

    when(challengeRepository.findById(id)).thenReturn(Optional.of(challenge));
    when(challengeRepository.save(any(Challenge.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    adminChallengeService.updateChallenge(id, request);

    ArgumentCaptor<Challenge> captor = ArgumentCaptor.forClass(Challenge.class);
    verify(challengeRepository).save(captor.capture());
    assertThat(captor.getValue().getMaxScore()).isEqualTo(42);
  }

  @Test
  void updateChallenge_withBlankShortDescription_normalizesToNull() {
    UUID id = UUID.randomUUID();
    Challenge challenge = new Challenge();
    challenge.setId(id);
    challenge.setMaxScore(0);

    AdminUpdateChallengeRequestDto request = new AdminUpdateChallengeRequestDto();
    request.setTitle("Title");
    request.setShortDescription("   ");
    request.setStatus(ChallengeStatusEnum.PUBLIC);
    request.setDifficulty(ChallengeDifficultyEnum.MEDIUM);
    request.setMaxScore(0);

    when(challengeRepository.findById(id)).thenReturn(Optional.of(challenge));
    when(challengeRepository.save(any(Challenge.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    adminChallengeService.updateChallenge(id, request);

    ArgumentCaptor<Challenge> captor = ArgumentCaptor.forClass(Challenge.class);
    verify(challengeRepository).save(captor.capture());
    assertThat(captor.getValue().getShortDescription()).isNull();
  }

  @Test
  void updateChallenge_whenNotFound_throwsChallengeNotFoundException() {
    UUID id = UUID.randomUUID();
    when(challengeRepository.findById(id)).thenReturn(Optional.empty());

    AdminUpdateChallengeRequestDto request = new AdminUpdateChallengeRequestDto();
    request.setTitle("Title");
    request.setStatus(ChallengeStatusEnum.PUBLIC);
    request.setDifficulty(ChallengeDifficultyEnum.EASY);

    assertThatThrownBy(() -> adminChallengeService.updateChallenge(id, request))
        .isInstanceOf(ChallengeNotFoundException.class)
        .hasMessageContaining(id.toString());

    verify(challengeRepository, never()).save(any());
  }

  // ── deleteChallenge ─────────────────────────────────────────────────────────

  @Test
  void deleteChallenge_whenExists_deletesChallenge() {
    UUID id = UUID.randomUUID();
    Challenge challenge = new Challenge();
    challenge.setId(id);
    when(challengeRepository.findById(id)).thenReturn(Optional.of(challenge));

    adminChallengeService.deleteChallenge(id);

    verify(challengeRepository).delete(challenge);
  }

  @Test
  void deleteChallenge_whenNotFound_throwsChallengeNotFoundException() {
    UUID id = UUID.randomUUID();
    when(challengeRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adminChallengeService.deleteChallenge(id))
        .isInstanceOf(ChallengeNotFoundException.class)
        .hasMessageContaining(id.toString());

    verify(challengeRepository, never()).delete(any());
  }
}
