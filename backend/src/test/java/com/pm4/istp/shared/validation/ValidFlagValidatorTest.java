package com.pm4.istp.shared.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidFlagValidatorTest {

  private ValidFlagValidator validator;

  @BeforeEach
  void setUp() {
    validator = new ValidFlagValidator();
  }

  @Test
  void isValid_whenNull_returnsTrue() {
    assertThat(validator.isValid(null, null)).isTrue();
  }

  @Test
  void isValid_whenEmpty_returnsTrue() {
    assertThat(validator.isValid("", null)).isTrue();
  }

  @Test
  void isValid_whenWellFormedUppercase_returnsTrue() {
    assertThat(validator.isValid("ISTP{ABC}", null)).isTrue();
  }

  @Test
  void isValid_whenWellFormedLowercase_returnsTrue() {
    assertThat(validator.isValid("ISTP{abc}", null)).isTrue();
  }

  @Test
  void isValid_whenContainsDigitsAndUnderscores_returnsTrue() {
    assertThat(validator.isValid("ISTP{Test_123_value}", null)).isTrue();
  }

  @Test
  void isValid_whenMissingPrefix_returnsFalse() {
    assertThat(validator.isValid("{abc}", null)).isFalse();
  }

  @Test
  void isValid_whenMissingBraces_returnsFalse() {
    assertThat(validator.isValid("ISTPabc", null)).isFalse();
  }

  @Test
  void isValid_whenMissingClosingBrace_returnsFalse() {
    assertThat(validator.isValid("ISTP{abc", null)).isFalse();
  }

  @Test
  void isValid_whenInnerContentIsEmpty_returnsFalse() {
    assertThat(validator.isValid("ISTP{}", null)).isFalse();
  }

  @Test
  void isValid_whenInnerContentContainsSpace_returnsFalse() {
    assertThat(validator.isValid("ISTP{has space}", null)).isFalse();
  }

  @Test
  void isValid_whenInnerContentContainsHyphen_returnsFalse() {
    assertThat(validator.isValid("ISTP{a-b}", null)).isFalse();
  }

  @Test
  void isValid_whenInnerContentContainsBraces_returnsFalse() {
    assertThat(validator.isValid("ISTP{a{b}c}", null)).isFalse();
  }

  @Test
  void isValid_whenSurroundedByWhitespace_returnsFalse() {
    assertThat(validator.isValid(" ISTP{abc} ", null)).isFalse();
  }

  @Test
  void isValid_whenWrongPrefixCase_returnsFalse() {
    assertThat(validator.isValid("istp{abc}", null)).isFalse();
  }
}
