package com.pm4.istp;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@OpenAPIDefinition(
    info =
        @Info(
            title = "ISTP API",
            version = "v1",
            description = "Interactive Security Training Platform"))
public class IstpApplication {

  public static void main(String[] args) {
    SpringApplication.run(IstpApplication.class, args);
  }
}
