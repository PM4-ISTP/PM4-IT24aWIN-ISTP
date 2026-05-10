package com.pm4.istp.shared.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakUserRepresentation {
  private String id;
  private String username;
  private String email;

  @JsonProperty("emailVerified")
  private Boolean emailVerified;

  private Boolean enabled;

  @JsonProperty("firstName")
  private String firstName;

  @JsonProperty("lastName")
  private String lastName;

  @Setter(AccessLevel.NONE)
  private Map<String, List<String>> attributes;

  @JsonProperty("attributes")
  public void setAttributes(Map<String, ?> attributes) {
    if (attributes == null) {
      this.attributes = null;
      return;
    }
    Map<String, List<String>> normalized = HashMap.newHashMap(attributes.size());
    for (Map.Entry<String, ?> entry : attributes.entrySet()) {
      if (entry.getKey() == null) {
        continue;
      }
      normalized.put(entry.getKey(), toStringList(entry.getValue()));
    }
    this.attributes = normalized;
  }

  private List<String> toStringList(Object value) {
    if (value == null) {
      return List.of();
    }
    if (value instanceof List<?> values) {
      List<String> normalized = new ArrayList<>(values.size());
      for (Object item : values) {
        normalized.add(item == null ? null : String.valueOf(item));
      }
      return normalized;
    }
    return List.of(String.valueOf(value));
  }
}
