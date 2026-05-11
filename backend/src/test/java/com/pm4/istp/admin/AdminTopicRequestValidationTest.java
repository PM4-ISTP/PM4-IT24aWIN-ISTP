package com.pm4.istp.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.pm4.istp.admin.dto.AdminTopicRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AdminTopicRequestValidationTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  private AdminTopicRequest req(String value) {
    AdminTopicRequest r = new AdminTopicRequest();
    r.setValue(value);
    return r;
  }

  @ParameterizedTest
  @ValueSource(strings = {"Docker", "Web-Security", "abc", "abc-123"})
  void value_acceptsValidTopics(String value) {
    Set<ConstraintViolation<AdminTopicRequest>> violations = validator.validate(req(value));
    assertThat(violations).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"ab", "1Docker", "invalid topic", "has!", "-leading"})
  void value_rejectsInvalidTopics(String value) {
    Set<ConstraintViolation<AdminTopicRequest>> violations = validator.validate(req(value));
    assertThat(violations).isNotEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void value_rejectsBlank(String value) {
    Set<ConstraintViolation<AdminTopicRequest>> violations = validator.validate(req(value));
    assertThat(violations)
        .extracting(ConstraintViolation::getMessage)
        .contains("Topic value is required");
  }

  @org.junit.jupiter.api.Test
  void value_rejectsTooLong() {
    Set<ConstraintViolation<AdminTopicRequest>> violations = validator.validate(req("A".repeat(25)));
    assertThat(violations).isNotEmpty();
  }
}
