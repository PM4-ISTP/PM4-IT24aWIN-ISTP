package com.pm4.istp.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.pm4.istp.course.db.entities.LabDifficultyEnum;
import com.pm4.istp.course.db.entities.LabStatusEnum;
import com.pm4.istp.course.dto.CreateLabRequestDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CreateLabRequestDtoValidationTest {

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

  private CreateLabRequestDto dtoWithDockerImage(String dockerImage) {
    CreateLabRequestDto dto = new CreateLabRequestDto();
    dto.setTitle("Title");
    dto.setStatus(LabStatusEnum.DRAFT);
    dto.setDifficulty(LabDifficultyEnum.EASY);
    dto.setDockerImage(dockerImage);
    return dto;
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ghcr.io/pm4-istp/test",
        "ghcr.io/pm4-istp/test:latest",
        "ghcr.io/pm4-istp/labs/test:1.0.0",
        "ghcr.io/school-org/lab@sha256:"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
      })
  void dockerImage_acceptsValidGhcrReferences(String dockerImage) {
    Set<ConstraintViolation<CreateLabRequestDto>> violations =
        validator.validate(dtoWithDockerImage(dockerImage));

    assertThat(violations)
        .filteredOn(v -> v.getPropertyPath().toString().equals("dockerImage"))
        .isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "bad image",
        "ghcr",
        "ghcr.io",
        "ghcr.io/p",
        "image",
        "registry/image:tag",
        "image:",
        "registry:5000/image",
        "image@sha256:abc",
        "ghcr.io/pm4-istp/test@sha256:abc",
        "ghcr.io/pm4-istp/test:latest@sha256:"
            + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "image!",
      })
  void dockerImage_rejectsInvalidReferences(String dockerImage) {
    Set<ConstraintViolation<CreateLabRequestDto>> violations =
        validator.validate(dtoWithDockerImage(dockerImage));

    assertThat(violations)
        .filteredOn(v -> v.getPropertyPath().toString().equals("dockerImage"))
        .isNotEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void dockerImage_rejectsBlank(String dockerImage) {
    Set<ConstraintViolation<CreateLabRequestDto>> violations =
        validator.validate(dtoWithDockerImage(dockerImage));

    assertThat(violations)
        .filteredOn(v -> v.getPropertyPath().toString().equals("dockerImage"))
        .extracting(ConstraintViolation::getMessage)
        .contains("Docker image is required");
  }
}
