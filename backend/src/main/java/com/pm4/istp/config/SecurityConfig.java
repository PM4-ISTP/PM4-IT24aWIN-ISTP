package com.pm4.istp.config;

import com.pm4.istp.filters.UserProvisioningFilter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Slf4j
@Configuration
public class SecurityConfig {
  @Value("${cors.allowed-origin}")
  private String allowedOrigin;

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      UserProvisioningFilter userProvisioningFilter,
      JwtAuthenticationConverter jwtAuthenticationConverter) {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            authorize ->
                // NextAuth routes (/api/auth/**) are served by the frontend and must never
                // reach the backend. Permit them here as a defense-in-depth safeguard in
                // case Ingress routing changes in the future.
                authorize
                    .requestMatchers("/api/auth/**")
                    .permitAll()
                    .requestMatchers("/v3/api-docs/**", "/v3/api-docs.yaml")
                    .permitAll()
                    .requestMatchers("/api/v1/courses/**")
                    .hasAnyRole("INSTRUCTOR", "ADMINISTRATOR")
                    // .requestMatchers("/api/v1/public/**").permitAll() --> if you want to allow
                    // catch-all rule to require authentication for all requests
                    .anyRequest()
                    .authenticated())
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
        .addFilterAfter(userProvisioningFilter, BearerTokenAuthenticationFilter.class);

    return http.build();
  }

  // CORS configuration to allow requests from the frontend application
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(allowedOrigin));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
