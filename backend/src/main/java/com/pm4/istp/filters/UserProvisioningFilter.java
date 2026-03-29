package com.pm4.istp.filters;

import com.pm4.istp.domain.entites.User;
import com.pm4.istp.domain.entites.UserRoleEnum;
import com.pm4.istp.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class UserProvisioningFilter extends OncePerRequestFilter {

  private final UserRepository userRepository;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null
        && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof Jwt jwt) {

      UUID keycloakId = UUID.fromString(jwt.getSubject());
      String username = jwt.getClaimAsString("preferred_username");
      String email = jwt.getClaimAsString("email");

      Set<UserRoleEnum> roles =
          authentication.getAuthorities().stream()
              .map(GrantedAuthority::getAuthority)
              .map(UserRoleEnum::fromString)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toSet());

      userRepository
          .findById(keycloakId)
          .ifPresentOrElse(
              user -> {
                if (!Objects.equals(user.getName(), username)
                    || !Objects.equals(user.getEmail(), email)
                    || !Objects.equals(user.getRoles(), roles)) {
                  user.setName(username);
                  user.setEmail(email);
                  user.setRoles(roles);
                  userRepository.save(user);
                }
              },
              () -> {
                User newUser = new User();
                newUser.setId(keycloakId);
                newUser.setName(username);
                newUser.setEmail(email);
                newUser.setRoles(roles);
                userRepository.save(newUser);
              });
    }

    filterChain.doFilter(request, response);
  }
}
