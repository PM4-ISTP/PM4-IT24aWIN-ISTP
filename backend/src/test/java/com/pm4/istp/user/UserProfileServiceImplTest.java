package com.pm4.istp.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm4.istp.shared.keycloak.KeycloakAdminApiException;
import com.pm4.istp.shared.keycloak.KeycloakAdminClient;
import com.pm4.istp.shared.keycloak.KeycloakUserRepresentation;
import com.pm4.istp.user.db.entities.User;
import com.pm4.istp.user.dto.UpdateUserProfileRequestDto;
import com.pm4.istp.user.exceptions.UserNotFoundException;
import com.pm4.istp.user.exceptions.UserProfileSyncException;
import com.pm4.istp.user.repositories.UserRepository;
import com.pm4.istp.user.services.UserProfileService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private KeycloakAdminClient keycloakAdminClient;

  @InjectMocks private UserProfileService userProfileService;

  @Test
  void getProfile_returnsExistingUserAndThrowsWhenMissing() {
    UUID userId = UUID.randomUUID();
    UUID missingId = UUID.randomUUID();
    User user = new User();
    user.setId(userId);

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
    when(userRepository.findByIdAndDeletedAtIsNull(missingId)).thenReturn(Optional.empty());

    assertThat(userProfileService.getProfile(userId)).isSameAs(user);
    assertThatThrownBy(() -> userProfileService.getProfile(missingId))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining(missingId.toString());
  }

  @Test
  void updateProfile_self_success_updatesKeycloakAndDb() {
    UUID userId = UUID.randomUUID();

    User dbUser = new User();
    dbUser.setId(userId);
    dbUser.setName("Old Name");
    dbUser.setEmail("test@example.com");

    KeycloakUserRepresentation before = new KeycloakUserRepresentation();
    before.setId(userId.toString());
    before.setUsername("user");
    before.setEmail("test@example.com");
    before.setEnabled(true);
    before.setFirstName("Old");
    before.setLastName("Name");
    before.setAttributes(Map.of("someOtherAttr", List.of("keep")));

    UpdateUserProfileRequestDto request = new UpdateUserProfileRequestDto();
    request.setFirstName("Alice");
    request.setLastName("Example");
    request.setTitle("Dr.");
    request.setPictureUrl("https://example.com/pic.png");

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(dbUser));
    when(keycloakAdminClient.getUser(userId)).thenReturn(before);
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    User updated =
        userProfileService.updateProfile(userId, List.of(), userId, request);

    ArgumentCaptor<KeycloakUserRepresentation> kcUpdateCaptor =
        ArgumentCaptor.forClass(KeycloakUserRepresentation.class);
    verify(keycloakAdminClient).updateUser(eq(userId), kcUpdateCaptor.capture());
    KeycloakUserRepresentation kcUpdated = kcUpdateCaptor.getValue();

    assertThat(kcUpdated.getFirstName()).isEqualTo("Alice");
    assertThat(kcUpdated.getLastName()).isEqualTo("Example");
    assertThat(kcUpdated.getAttributes()).containsEntry("title", List.of("Dr."));
    assertThat(kcUpdated.getAttributes())
        .containsEntry("picture", List.of("https://example.com/pic.png"));
    assertThat(kcUpdated.getAttributes()).containsEntry("someOtherAttr", List.of("keep"));

    assertThat(updated.getFirstName()).isEqualTo("Alice");
    assertThat(updated.getLastName()).isEqualTo("Example");
    assertThat(updated.getName()).isEqualTo("Alice Example");
    assertThat(updated.getTitle()).isEqualTo("Dr.");
    assertThat(updated.getPicture()).isEqualTo("https://example.com/pic.png");
  }

  @Test
  void updateProfile_otherUser_withoutAdminRole_isForbidden() {
    UUID actorId = UUID.randomUUID();
    UUID targetId = UUID.randomUUID();
    List<GrantedAuthority> authorities = List.of();
    UpdateUserProfileRequestDto request = new UpdateUserProfileRequestDto();

    assertThatThrownBy(
            () -> userProfileService.updateProfile(actorId, authorities, targetId, request))
        .isInstanceOf(AccessDeniedException.class);

    verify(userRepository, never()).findByIdAndDeletedAtIsNull(any());
    verify(keycloakAdminClient, never()).getUser(any());
    verify(keycloakAdminClient, never()).updateUser(any(), any());
  }

  @Test
  void updateProfile_otherUser_withNullAuthorities_isForbidden() {
    UUID actorId = UUID.randomUUID();
    UUID targetId = UUID.randomUUID();
    UpdateUserProfileRequestDto request = new UpdateUserProfileRequestDto();

    assertThatThrownBy(() -> userProfileService.updateProfile(actorId, null, targetId, request))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void updateProfile_otherUser_withAdminRole_isAllowed() {
    UUID actorId = UUID.randomUUID();
    UUID targetId = UUID.randomUUID();

    User dbUser = new User();
    dbUser.setId(targetId);
    dbUser.setName("Old Name");
    dbUser.setEmail("test@example.com");

    KeycloakUserRepresentation before = new KeycloakUserRepresentation();
    before.setId(targetId.toString());
    before.setEmail("test@example.com");
    before.setEnabled(true);
    before.setAttributes(Map.of());

    UpdateUserProfileRequestDto request = new UpdateUserProfileRequestDto();
    request.setFirstName("Admin");
    request.setLastName("Updated");

    when(userRepository.findByIdAndDeletedAtIsNull(targetId)).thenReturn(Optional.of(dbUser));
    when(keycloakAdminClient.getUser(targetId)).thenReturn(before);
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    GrantedAuthority admin = () -> "ROLE_ADMINISTRATOR";
    User updated = userProfileService.updateProfile(actorId, List.of(admin), targetId, request);

    assertThat(updated.getFirstName()).isEqualTo("Admin");
    assertThat(updated.getLastName()).isEqualTo("Updated");
  }

  @Test
  void updateProfile_keycloakFailure_doesNotUpdateDb() {
    UUID userId = UUID.randomUUID();

    User dbUser = new User();
    dbUser.setId(userId);
    dbUser.setName("Old Name");
    dbUser.setEmail("test@example.com");

    KeycloakUserRepresentation before = new KeycloakUserRepresentation();
    before.setId(userId.toString());
    before.setEmail("test@example.com");
    before.setEnabled(true);

    UpdateUserProfileRequestDto request = new UpdateUserProfileRequestDto();
    request.setFirstName("Alice");
    request.setLastName("Example");

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(dbUser));
    when(keycloakAdminClient.getUser(userId)).thenReturn(before);
    doThrow(new KeycloakAdminApiException("boom")).when(keycloakAdminClient).updateUser(eq(userId), any());

    List<GrantedAuthority> authorities = List.of();

    assertThatThrownBy(() -> userProfileService.updateProfile(userId, authorities, userId, request))
        .isInstanceOf(KeycloakAdminApiException.class);

    verify(userRepository, never()).save(any());
  }

  @Test
  void updateProfile_whenKeycloakUserMissing_throwsSyncException() {
    UUID userId = UUID.randomUUID();
    User dbUser = new User();
    dbUser.setId(userId);
    UpdateUserProfileRequestDto request = new UpdateUserProfileRequestDto();
    request.setFirstName("Alice");
    request.setLastName("Example");

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(dbUser));
    when(keycloakAdminClient.getUser(userId)).thenReturn(null);

    assertThatThrownBy(() -> userProfileService.updateProfile(userId, List.of(), userId, request))
        .isInstanceOf(UserProfileSyncException.class)
        .hasMessageContaining("could not be loaded");
    verify(keycloakAdminClient, never()).updateUser(any(), any());
  }

  @Test
  void updateProfile_blankOptionalValuesRemoveKeycloakAttributesAndCollapseName() {
    UUID userId = UUID.randomUUID();
    User dbUser = new User();
    dbUser.setId(userId);
    KeycloakUserRepresentation before = new KeycloakUserRepresentation();
    before.setAttributes(Map.of("title", List.of("Old"), "picture", List.of("old.png")));
    UpdateUserProfileRequestDto request = new UpdateUserProfileRequestDto();
    request.setFirstName("  Alice  ");
    request.setLastName("  Example  ");
    request.setTitle(" ");
    request.setPictureUrl("");

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(dbUser));
    when(keycloakAdminClient.getUser(userId)).thenReturn(before);
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    User updated = userProfileService.updateProfile(userId, List.of(), userId, request);

    ArgumentCaptor<KeycloakUserRepresentation> captor =
        ArgumentCaptor.forClass(KeycloakUserRepresentation.class);
    verify(keycloakAdminClient).updateUser(eq(userId), captor.capture());
    assertThat(captor.getValue().getAttributes()).doesNotContainKeys("title", "picture");
    assertThat(updated.getName()).isEqualTo("Alice Example");
    assertThat(updated.getTitle()).isNull();
    assertThat(updated.getPicture()).isNull();
  }

  @Test
  void updateProfile_blankRequiredFields_throwIllegalArgumentException() {
    UUID userId = UUID.randomUUID();
    User dbUser = new User();
    dbUser.setId(userId);
    KeycloakUserRepresentation before = new KeycloakUserRepresentation();
    UpdateUserProfileRequestDto request = new UpdateUserProfileRequestDto();
    request.setFirstName(" ");
    request.setLastName("Example");

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(dbUser));
    when(keycloakAdminClient.getUser(userId)).thenReturn(before);

    assertThatThrownBy(() -> userProfileService.updateProfile(userId, List.of(), userId, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("firstName");
    verify(keycloakAdminClient, never()).updateUser(any(), any());
  }

  @Test
  void updateProfile_dbFailure_rollsBackKeycloak() {
    UUID userId = UUID.randomUUID();

    User dbUser = new User();
    dbUser.setId(userId);
    dbUser.setName("Old Name");
    dbUser.setEmail("test@example.com");

    KeycloakUserRepresentation before = new KeycloakUserRepresentation();
    before.setId(userId.toString());
    before.setEmail("test@example.com");
    before.setEnabled(true);
    before.setFirstName("Old");
    before.setLastName("Name");

    UpdateUserProfileRequestDto request = new UpdateUserProfileRequestDto();
    request.setFirstName("Alice");
    request.setLastName("Example");

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(dbUser));
    when(keycloakAdminClient.getUser(userId)).thenReturn(before);
    when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("db down"));

    List<GrantedAuthority> authorities = List.of();

    assertThatThrownBy(() -> userProfileService.updateProfile(userId, authorities, userId, request))
        .isInstanceOf(UserProfileSyncException.class);

    verify(keycloakAdminClient, times(2)).updateUser(eq(userId), any(KeycloakUserRepresentation.class));
  }

  @Test
  void addOnlineTime_accumulates() {
    UUID userId = UUID.randomUUID();

    User dbUser = new User();
    dbUser.setId(userId);
    dbUser.setTotalSecondsOnline(100L);

    User updatedUser = new User();
    updatedUser.setId(userId);
    updatedUser.setTotalSecondsOnline(150L);

    when(userRepository.findByIdAndDeletedAtIsNull(userId))
        .thenReturn(Optional.of(dbUser))
        .thenReturn(Optional.of(updatedUser));
    when(userRepository.incrementTotalSecondsOnlineById(userId, 50L)).thenReturn(1);

    long result = userProfileService.addOnlineTime(userId, 50L);

    assertThat(result).isEqualTo(150L);
    verify(userRepository).incrementTotalSecondsOnlineById(userId, 50L);
    verify(userRepository, never()).save(any());
  }

  @Test
  void addOnlineTime_zeroSeconds_doesNotSave() {
    UUID userId = UUID.randomUUID();

    User dbUser = new User();
    dbUser.setId(userId);
    dbUser.setTotalSecondsOnline(200L);

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(dbUser));

    long result = userProfileService.addOnlineTime(userId, 0L);

    assertThat(result).isEqualTo(200L);
    verify(userRepository, never()).incrementTotalSecondsOnlineById(any(), anyLong());
    verify(userRepository, never()).save(any());
  }

  @Test
  void addOnlineTime_whenUserDisappearsAfterIncrement_throwsNotFound() {
    UUID userId = UUID.randomUUID();
    User dbUser = new User();
    dbUser.setId(userId);

    when(userRepository.findByIdAndDeletedAtIsNull(userId))
        .thenReturn(Optional.of(dbUser))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> userProfileService.addOnlineTime(userId, 10L))
        .isInstanceOf(UserNotFoundException.class);
    verify(userRepository).incrementTotalSecondsOnlineById(userId, 10L);
  }
}
