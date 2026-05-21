package com.pm4.istp.testing.controllers;

import com.pm4.istp.testing.dto.DatabaseCredentialsDto;
import jakarta.validation.Valid;
import java.sql.Connection;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/testing")
@ConditionalOnProperty(
    name = "istp.features.staging-endpoint-enabled",
    havingValue = "true",
    matchIfMissing = false)
@RequiredArgsConstructor
public class TestingController {

  private final DataSource dataSource;

  @Value("${spring.datasource.username:}")
  private String databaseUsername;

  @Value("${spring.datasource.password:}")
  private String databasePassword;

  @PostMapping("/load-testdata")
  public ResponseEntity<String> loadTestdata(
      @Valid @RequestBody DatabaseCredentialsDto databaseCredentialsDto) {
    ResponseEntity<String> responseEntity = ResponseEntity.ok("Test data loaded");
    try {
      executeScript("loadTestdata.sql", databaseCredentialsDto);
    } catch (IllegalArgumentException e) {
      responseEntity = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
    } catch (IllegalStateException e) {
      responseEntity = ResponseEntity.internalServerError().body(e.getMessage());
    }
    return responseEntity;
  }

  @PostMapping("/cleanup-testdata")
  public ResponseEntity<String> cleanupTestdata(
      @Valid @RequestBody DatabaseCredentialsDto databaseCredentialsDto) {
    ResponseEntity<String> responseEntity = ResponseEntity.ok("Test data cleanup complete");
    try {
      executeScript("cleanupTestdata.sql", databaseCredentialsDto);
    } catch (IllegalArgumentException e) {
      responseEntity = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
    } catch (IllegalStateException e) {
      responseEntity = ResponseEntity.internalServerError().body(e.getMessage());
    }
    return responseEntity;
  }

  private void executeScript(String resourceName, DatabaseCredentialsDto databaseCredentialsDto) {
    if (!databaseUsername.equals(databaseCredentialsDto.getUsername())) {
      throw new IllegalArgumentException("Invalid username or password");
    }
    if (!databasePassword.equals(databaseCredentialsDto.getPassword())) {
      throw new IllegalArgumentException("Invalid username or password");
    }

    Connection connection;
    try {
      connection = DataSourceUtils.getConnection(dataSource);
    } catch (RuntimeException ex) {
      throw new IllegalStateException("Failed to connect to database for " + resourceName, ex);
    }

    try {
      ScriptUtils.executeSqlScript(connection, new ClassPathResource(resourceName));
    } catch (RuntimeException ex) {
      throw new IllegalStateException("Failed to run " + resourceName, ex);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }
}
