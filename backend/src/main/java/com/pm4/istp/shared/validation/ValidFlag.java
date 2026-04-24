package com.pm4.istp.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidFlagValidator.class)
public @interface ValidFlag {
  String message() default
      "Flag must match the format ISTP{...} and may only contain letters, digits and underscores";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
