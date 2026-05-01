package com.pm4.istp.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class ValidFlagValidator implements ConstraintValidator<ValidFlag, String> {

  private static final Pattern FLAG_PATTERN = Pattern.compile("^ISTP\\{[A-Za-z0-9_]+\\}$");

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isEmpty()) {
      return true;
    }
    return FLAG_PATTERN.matcher(value).matches();
  }
}
