package com.pm4.istp.shared.keycloak;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KeycloakUserRepresentationTest {

  @Test
  void setAttributes_normalizesScalarsListsNullsAndSkipsNullKeys() {
    KeycloakUserRepresentation user = new KeycloakUserRepresentation();
    Map<String, Object> attributes = new LinkedHashMap<>();
    attributes.put("department", "IT");
    attributes.put("levels", List.of(1, "two"));
    attributes.put("empty", null);
    attributes.put(null, "ignored");

    user.setAttributes(attributes);

    assertThat(user.getAttributes())
        .containsEntry("department", List.of("IT"))
        .containsEntry("levels", List.of("1", "two"))
        .containsEntry("empty", List.of())
        .doesNotContainKey(null);
  }

  @Test
  void setAttributes_acceptsNullMap() {
    KeycloakUserRepresentation user = new KeycloakUserRepresentation();
    user.setAttributes(Map.of("department", "IT"));

    user.setAttributes(null);

    assertThat(user.getAttributes()).isNull();
  }
}
