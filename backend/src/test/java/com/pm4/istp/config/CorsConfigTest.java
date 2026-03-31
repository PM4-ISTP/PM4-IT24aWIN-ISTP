package com.pm4.istp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class CorsConfigTest {

  private final CorsConfig corsConfig = new CorsConfig();

  @Test
  void corsConfigurationSource_usesConfiguredOriginAndDefaults() {
    CorsConfigurationSource source =
        corsConfig.corsConfigurationSource("http://localhost:3000");

    CorsConfiguration configuration =
        source.getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/v1/users"));

    assertThat(configuration).isNotNull();
    assertThat(configuration.getAllowedOrigins()).containsExactly("http://localhost:3000");
    assertThat(configuration.getAllowedMethods())
        .containsExactly("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
    assertThat(configuration.getAllowedHeaders()).isEqualTo(List.of("*"));
    assertThat(configuration.getAllowCredentials()).isTrue();
  }
}
