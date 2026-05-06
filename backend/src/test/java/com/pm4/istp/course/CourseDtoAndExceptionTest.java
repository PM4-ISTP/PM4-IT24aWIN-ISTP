package com.pm4.istp.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.pm4.istp.course.dto.DockerImageCheckResponseDto;
import com.pm4.istp.course.dto.ListCourseResponseDto;
import com.pm4.istp.course.exceptions.ChallengeAlreadySolvedException;
import com.pm4.istp.course.exceptions.ChallengeNotFoundException;
import com.pm4.istp.course.exceptions.CourseAccessDeniedException;
import com.pm4.istp.course.exceptions.CourseNotFoundException;
import com.pm4.istp.course.exceptions.CourseParticipantNotFoundException;
import com.pm4.istp.course.exceptions.InvalidCourseCollaboratorException;
import com.pm4.istp.course.exceptions.InvalidCourseLabException;
import com.pm4.istp.course.exceptions.InvalidInviteCodeException;
import com.pm4.istp.course.exceptions.InviteCodeGenerationException;
import com.pm4.istp.course.exceptions.LabAccessDeniedException;
import com.pm4.istp.course.exceptions.LabNotFoundException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CourseDtoAndExceptionTest {

  @Test
  void dockerImageResponseAndListCourseDtoExposeFields() {
    DockerImageCheckResponseDto check = new DockerImageCheckResponseDto(true, "ok");
    UUID id = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();

    ListCourseResponseDto dto =
        new ListCourseResponseDto(
            id, "Title", "Description", "Short", true, false, 2, now, now, "image", "web", "Owner", "picture", "Prof");
    ListCourseResponseDto builderDto =
        ListCourseResponseDto.builder().id(id).title("Title").isPublished(true).isPrivate(true).build();

    assertThat(check.reachable()).isTrue();
    assertThat(check.message()).isEqualTo("ok");
    assertThat(dto.getId()).isEqualTo(id);
    assertThat(dto.getInstructorCount()).isEqualTo(2);
    assertThat(dto.getOwnerTitle()).isEqualTo("Prof");
    assertThat(builderDto.isPublished()).isTrue();
    assertThat(builderDto.isPrivate()).isTrue();
    assertThat(new ListCourseResponseDto()).isNotNull();
  }

  @Test
  void newCodeExceptions_coverConstructors() {
    RuntimeException cause = new RuntimeException("cause");

    assertThat(new ChallengeNotFoundException()).hasNoCause();
    assertThat(new ChallengeNotFoundException("missing")).hasMessage("missing");
    assertThat(new ChallengeNotFoundException("missing", cause)).hasCause(cause);
    assertThat(new ChallengeNotFoundException(cause)).hasCause(cause);
    assertThat(new ChallengeNotFoundException("quiet", cause, false, false)).hasMessage("quiet");
    assertThat(new CourseParticipantNotFoundException()).hasNoCause();
    assertThat(new InvalidCourseCollaboratorException("bad")).hasMessage("bad");
    assertThat(new InvalidCourseLabException("bad", cause)).hasCause(cause);
    assertThat(new InvalidInviteCodeException(cause)).hasCause(cause);
    assertThat(new LabAccessDeniedException("denied")).hasMessage("denied");
    assertThat(new LabNotFoundException("missing", cause, true, true)).hasCause(cause);
    assertThat(new ChallengeAlreadySolvedException()).hasNoCause();
    assertThat(new InviteCodeGenerationException("failed")).hasMessage("failed");
  }

  @Test
  void remainingIstpExceptionConstructors_areCovered() {
    RuntimeException cause = new RuntimeException("cause");

    assertThat(new CourseAccessDeniedException()).hasNoCause();
    assertThat(new CourseAccessDeniedException("denied")).hasMessage("denied");
    assertThat(new CourseAccessDeniedException("denied", cause)).hasCause(cause);
    assertThat(new CourseAccessDeniedException(cause)).hasCause(cause);
    assertThat(new CourseAccessDeniedException("denied", cause, false, false)).hasMessage("denied");
    assertThat(new CourseNotFoundException()).hasNoCause();
    assertThat(new CourseNotFoundException("missing")).hasMessage("missing");
    assertThat(new CourseNotFoundException("missing", cause)).hasCause(cause);
    assertThat(new CourseNotFoundException(cause)).hasCause(cause);
    assertThat(new CourseNotFoundException("missing", cause, false, false)).hasMessage("missing");
    assertThat(new InvalidCourseCollaboratorException()).hasNoCause();
    assertThat(new InvalidCourseCollaboratorException(cause)).hasCause(cause);
    assertThat(new InvalidCourseCollaboratorException("bad", cause, false, false)).hasMessage("bad");
    assertThat(new LabAccessDeniedException()).hasNoCause();
    assertThat(new LabAccessDeniedException(cause)).hasCause(cause);
    assertThat(new LabAccessDeniedException("denied", cause, false, false)).hasMessage("denied");
  }
}
