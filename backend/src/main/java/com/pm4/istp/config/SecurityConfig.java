package com.pm4.istp.config;

import com.pm4.istp.filters.UserProvisioningFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      UserProvisioningFilter userProvisioningFilter,
      JwtAuthenticationConverter jwtAuthenticationConverter)
      throws Exception {
    http.authorizeHttpRequests(
            authorize ->
                // NextAuth routes (/api/auth/**) are served by the frontend and must never
                // reach the backend. Permit them here as a defense-in-depth safeguard in
                // case Ingress routing changes in the future.
                authorize
                    .requestMatchers("/api/auth/**")
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
}
