package com.pm4.istp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Slf4j
@Configuration
@EnableJpaAuditing
public class JpaConfiguration {}
