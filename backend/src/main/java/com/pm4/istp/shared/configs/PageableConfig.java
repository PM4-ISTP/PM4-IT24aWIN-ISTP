package com.pm4.istp.shared.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class PageableConfig {
  private final int maxPageSize;

  public PageableConfig(@Value("${istp.pagination.max-page-size:50}") int maxPageSize) {
    this.maxPageSize = maxPageSize;
  }

  @Bean
  PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
    return resolver -> resolver.setMaxPageSize(maxPageSize);
  }
}
