package util;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public final class JwtUtil {
  private JwtUtil(){
  }

  public static UUID parseUserId(Jwt jwt) {
    return UUID.fromString(jwt.getSubject());
  }


}
