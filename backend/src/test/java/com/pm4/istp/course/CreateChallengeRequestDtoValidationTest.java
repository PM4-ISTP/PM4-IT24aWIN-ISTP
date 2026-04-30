package com.pm4.istp.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.pm4.istp.course.db.entities.ChallengeDifficultyEnum;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import com.pm4.istp.course.dto.CreateChallengeRequestDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;



class CreateChallengeRequestDtoValidationTest {

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

  private CreateChallengeRequestDto dtoWithDockerImage(String dockerImage) {
    CreateChallengeRequestDto dto = new CreateChallengeRequestDto();
    dto.setTitle("Title");
    dto.setStatus(ChallengeStatusEnum.DRAFT);
    dto.setDifficulty(ChallengeDifficultyEnum.EASY);
    dto.setDockerImage(dockerImage);
    return dto;
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ghcr.io/pm4-istp/test",
        "ghcr.io/pm4-istp/test:latest",
        "ghcr.io/pm4-istp/challenges/test:1.0.0",
      })
  void dockerImage_acceptsValidGhcrReferences(String dockerImage) {
    Set<ConstraintViolation<CreateChallengeRequestDto>> violations =
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
        "image!",
      })
  void dockerImage_rejectsInvalidReferences(String dockerImage) {
    Set<ConstraintViolation<CreateChallengeRequestDto>> violations =
        validator.validate(dtoWithDockerImage(dockerImage));

    assertThat(violations)
        .filteredOn(v -> v.getPropertyPath().toString().equals("dockerImage"))
        .isNotEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void dockerImage_rejectsBlank(String dockerImage) {
    Set<ConstraintViolation<CreateChallengeRequestDto>> violations =
        validator.validate(dtoWithDockerImage(dockerImage));

    assertThat(violations)
        .filteredOn(v -> v.getPropertyPath().toString().equals("dockerImage"))
        .isNotEmpty();
  }
}
