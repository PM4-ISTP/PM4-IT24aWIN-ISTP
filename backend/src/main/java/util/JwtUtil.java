package util;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtUtil {
  private JwtUtil() {}

  public static UUID parseUserId(Jwt jwt) {
    return UUID.fromString(jwt.getSubject());
  }
}
