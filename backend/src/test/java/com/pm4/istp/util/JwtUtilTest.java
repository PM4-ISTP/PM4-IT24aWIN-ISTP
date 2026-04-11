package com.pm4.istp.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtUtilTest {

  @Test
  void parseUserId_withValidUuidSubject_returnsUuid() {
    UUID expected = UUID.randomUUID();
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(expected.toString())
            .build();

    UUID result = JwtUtil.parseUserId(jwt);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void parseUserId_withInvalidSubject_throwsIllegalArgumentException() {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("not-a-uuid")
            .build();

    assertThatThrownBy(() -> JwtUtil.parseUserId(jwt))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
