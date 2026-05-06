package com.pm4.istp.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.pm4.istp.challengepod.exceptions.LabPodException;
import com.pm4.istp.shared.keycloak.KeycloakAdminApiException;
import com.pm4.istp.user.exceptions.UserNotFoundException;
import com.pm4.istp.user.exceptions.UserProfileSyncException;
import org.junit.jupiter.api.Test;

class UserExceptionTest {

  @Test
  void exceptionConstructorsPreserveMessagesAndCauses() {
    RuntimeException cause = new RuntimeException("cause");

    assertThat(new UserNotFoundException()).hasNoCause();
    assertThat(new UserNotFoundException("missing")).hasMessage("missing");
    assertThat(new UserNotFoundException("missing", cause)).hasCause(cause);
    assertThat(new UserNotFoundException(cause)).hasCause(cause);
    assertThat(new UserNotFoundException("missing", cause, false, false)).hasMessage("missing");
    assertThat(new UserProfileSyncException("sync")).hasMessage("sync");
    assertThat(new UserProfileSyncException("sync", cause)).hasCause(cause);
    assertThat(new LabPodException("pod")).hasMessage("pod");
    assertThat(new LabPodException("pod", cause)).hasCause(cause);
    assertThat(new KeycloakAdminApiException("keycloak")).hasMessage("keycloak");
    assertThat(new KeycloakAdminApiException("keycloak", cause)).hasCause(cause);
  }
}
