package com.pm4.istp.shared.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;

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

  private Map<String, List<String>> attributes;
}
