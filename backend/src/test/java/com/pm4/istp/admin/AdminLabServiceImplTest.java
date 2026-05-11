package com.pm4.istp.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.admin.dto.AdminUpdateLabRequestDto;
import com.pm4.istp.admin.services.impl.AdminLabServiceImpl;
import com.pm4.istp.course.db.entities.Lab;
import com.pm4.istp.course.db.entities.LabDifficultyEnum;
import com.pm4.istp.course.db.entities.LabStatusEnum;
import com.pm4.istp.course.exceptions.LabNotFoundException;
import com.pm4.istp.course.repositories.LabRepository;
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

  @Mock private LabRepository labRepository;

  @InjectMocks private AdminLabServiceImpl adminChallengeService;

  // ── listChallenges ──────────────────────────────────────────────────────────

  @Test
  void listChallenges_withNullQuery_callsFindAllWithoutFilter() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<com.pm4.istp.admin.dto.AdminLabListItemDto> expected =
        new PageImpl<>(List.of(), pageable, 0);
    when(labRepository.findAllChallengesForAdmin(pageable)).thenReturn(expected);

    Page<com.pm4.istp.admin.dto.AdminLabListItemDto> result =
        adminChallengeService.listChallenges(null, pageable);

    assertThat(result).isSameAs(expected);
    verify(labRepository).findAllChallengesForAdmin(pageable);
    verify(labRepository, never()).findAllChallengesForAdminByQuery(any(), any());
  }

  @Test
  void listChallenges_withBlankQuery_treatsAsNullAndCallsFindAll() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<com.pm4.istp.admin.dto.AdminLabListItemDto> expected =
        new PageImpl<>(List.of(), pageable, 0);
    when(labRepository.findAllChallengesForAdmin(pageable)).thenReturn(expected);

    Page<com.pm4.istp.admin.dto.AdminLabListItemDto> result =
        adminChallengeService.listChallenges("   ", pageable);

    assertThat(result).isSameAs(expected);
    verify(labRepository).findAllChallengesForAdmin(pageable);
    verify(labRepository, never()).findAllChallengesForAdminByQuery(any(), any());
  }

  @Test
  void listChallenges_withNonBlankQuery_callsFindByQuery() {
    Pageable pageable = PageRequest.of(0, 10);
    String query = "sql injection";
    Page<com.pm4.istp.admin.dto.AdminLabListItemDto> expected =
        new PageImpl<>(List.of(), pageable, 0);
    when(labRepository.findAllChallengesForAdminByQuery(query, pageable))
        .thenReturn(expected);

    Page<com.pm4.istp.admin.dto.AdminLabListItemDto> result =
        adminChallengeService.listChallenges(query, pageable);

    assertThat(result).isSameAs(expected);
    verify(labRepository).findAllChallengesForAdminByQuery(query, pageable);
    verify(labRepository, never()).findAllChallengesForAdmin(any());
  }

  // ── updateChallenge ─────────────────────────────────────────────────────────

  @Test
  void updateChallenge_withValidRequest_updatesEditableFields() {
    UUID id = UUID.randomUUID();
    Lab lab = new Lab();
    lab.setId(id);
    lab.setMaxScore(0);

    AdminUpdateLabRequestDto request = new AdminUpdateLabRequestDto();
    request.setTitle("Updated Title");
    request.setShortDescription("  short  ");
    request.setDescription("Full description");
    request.setStatus(LabStatusEnum.PUBLIC);
    request.setDifficulty(LabDifficultyEnum.HARD);

    when(labRepository.findById(id)).thenReturn(Optional.of(lab));
    when(labRepository.save(any(Lab.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    adminChallengeService.updateChallenge(id, request);

    ArgumentCaptor<Lab> captor = ArgumentCaptor.forClass(Lab.class);
    verify(labRepository).save(captor.capture());
    Lab saved = captor.getValue();

    assertThat(saved.getTitle()).isEqualTo("Updated Title");
    assertThat(saved.getShortDescription()).isEqualTo("short");
    assertThat(saved.getDescription()).isEqualTo("Full description");
    assertThat(saved.getStatus()).isEqualTo(LabStatusEnum.PUBLIC);
    assertThat(saved.getDifficulty()).isEqualTo(LabDifficultyEnum.HARD);
  }

  @Test
  void updateChallenge_doesNotOverwriteExistingMaxScore() {
    UUID id = UUID.randomUUID();
    Lab lab = new Lab();
    lab.setId(id);
    lab.setMaxScore(42);

    AdminUpdateLabRequestDto request = new AdminUpdateLabRequestDto();
    request.setTitle("Title");
    request.setStatus(LabStatusEnum.PRIVATE);
    request.setDifficulty(LabDifficultyEnum.EASY);

    when(labRepository.findById(id)).thenReturn(Optional.of(lab));
    when(labRepository.save(any(Lab.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    adminChallengeService.updateChallenge(id, request);

    ArgumentCaptor<Lab> captor = ArgumentCaptor.forClass(Lab.class);
    verify(labRepository).save(captor.capture());
    assertThat(captor.getValue().getMaxScore()).isEqualTo(42);
  }

  @Test
  void updateChallenge_withBlankShortDescription_normalizesToNull() {
    UUID id = UUID.randomUUID();
    Lab lab = new Lab();
    lab.setId(id);
    lab.setMaxScore(0);

    AdminUpdateLabRequestDto request = new AdminUpdateLabRequestDto();
    request.setTitle("Title");
    request.setShortDescription("   ");
    request.setStatus(LabStatusEnum.PUBLIC);
    request.setDifficulty(LabDifficultyEnum.MEDIUM);

    when(labRepository.findById(id)).thenReturn(Optional.of(lab));
    when(labRepository.save(any(Lab.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    adminChallengeService.updateChallenge(id, request);

    ArgumentCaptor<Lab> captor = ArgumentCaptor.forClass(Lab.class);
    verify(labRepository).save(captor.capture());
    assertThat(captor.getValue().getShortDescription()).isNull();
  }

  @Test
  void updateChallenge_whenNotFound_throwsChallengeNotFoundException() {
    UUID id = UUID.randomUUID();
    when(labRepository.findById(id)).thenReturn(Optional.empty());

    AdminUpdateLabRequestDto request = new AdminUpdateLabRequestDto();
    request.setTitle("Title");
    request.setStatus(LabStatusEnum.PUBLIC);
    request.setDifficulty(LabDifficultyEnum.EASY);

    assertThatThrownBy(() -> adminChallengeService.updateChallenge(id, request))
        .isInstanceOf(LabNotFoundException.class)
        .hasMessageContaining(id.toString());

    verify(labRepository, never()).save(any());
  }

  // ── deleteChallenge ─────────────────────────────────────────────────────────

  @Test
  void deleteChallenge_whenExists_deletesChallenge() {
    UUID id = UUID.randomUUID();
    Lab lab = new Lab();
    lab.setId(id);
    when(labRepository.findById(id)).thenReturn(Optional.of(lab));

    adminChallengeService.deleteChallenge(id);

    verify(labRepository).delete(lab);
  }

  @Test
  void deleteChallenge_whenNotFound_throwsChallengeNotFoundException() {
    UUID id = UUID.randomUUID();
    when(labRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adminChallengeService.deleteChallenge(id))
        .isInstanceOf(LabNotFoundException.class)
        .hasMessageContaining(id.toString());

    verify(labRepository, never()).delete(any());
  }
}
