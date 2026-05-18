package com.pm4.istp.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.pm4.istp.course.db.InstructorRoleEnum;
import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.course.db.entities.ChallengeOption;
import com.pm4.istp.course.db.entities.ChallengeType;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.db.entities.CourseInstructor;
import com.pm4.istp.course.db.entities.CourseLab;
import com.pm4.istp.course.db.entities.CourseStatusEnum;
import com.pm4.istp.course.db.entities.Lab;
import com.pm4.istp.course.db.entities.LabDifficultyEnum;
import com.pm4.istp.course.db.entities.LabStatusEnum;
import com.pm4.istp.course.db.entities.McAttemptsMode;
import com.pm4.istp.course.dto.ChallengeOptionRequestDto;
import com.pm4.istp.course.dto.ChallengeRequestDto;
import com.pm4.istp.course.dto.CreateLabRequestDto;
import com.pm4.istp.course.dto.CreateCourseInstructorRequestDto;
import com.pm4.istp.course.dto.CreateCourseRequestDto;
import com.pm4.istp.course.dto.UpdateLabRequestDto;
import com.pm4.istp.course.dto.UpdateCourseInstructorRequestDto;
import com.pm4.istp.course.dto.UpdateCourseRequestDto;
import com.pm4.istp.course.mappers.CourseMapperImpl;
import com.pm4.istp.course.mappers.LabMapperImpl;
import com.pm4.istp.user.db.entities.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CourseMapperCoverageTest {

  private LabMapperImpl labMapper;
  private CourseMapperImpl courseMapper;

  @BeforeEach
  void setUp() {
    labMapper = new LabMapperImpl();
    courseMapper = new CourseMapperImpl();
    ReflectionTestUtils.setField(courseMapper, "labMapper", labMapper);
  }

  @Test
  void labMapper_mapsRequestsEntitiesAndNulls() {
    ChallengeOptionRequestDto optionDto =
        new ChallengeOptionRequestDto(UUID.randomUUID(), "Option A", true, 1);
    ChallengeRequestDto challengeDto =
        new ChallengeRequestDto(
            UUID.randomUUID(),
            "Challenge",
            "Description",
            "flag",
            2,
            ChallengeType.MULTIPLE_CHOICE,
            5,
            "hint",
            List.of(optionDto));
    CreateLabRequestDto createDto =
        new CreateLabRequestDto(
            "Lab",
            "Long",
            LabStatusEnum.PUBLIC,
            LabDifficultyEnum.EASY,
            "ghcr.io/acme/lab:latest",
            8080,
            null,
            List.of(challengeDto));
    UpdateLabRequestDto updateDto =
        new UpdateLabRequestDto(
            "Lab 2",
            "Long 2",
            LabStatusEnum.DRAFT,
            LabDifficultyEnum.HARD,
            "ghcr.io/acme/lab:2",
            8081,
            null,
            List.of(challengeDto));

    assertLabRequestMappings(optionDto, challengeDto, createDto, updateDto);

    Lab lab = labFixture();
    Challenge challenge = lab.getChallenges().get(0);
    ChallengeOption option = challenge.getOptions().get(0);
    CourseLab courseLab = new CourseLab();
    courseLab.setLab(lab);
    courseLab.setOrderIndex(3);
    courseLab.setDueAt(LocalDateTime.now());

    assertLabNullMappings();
    assertLabEntityMappings(lab, challenge, option, courseLab);
    challenge.setFlag(" ");
    assertThat(labMapper.toChallengeStudentDto(challenge).isTheory()).isTrue();

    courseLab.setLab(null);
    assertThat(labMapper.toCourseLabResponseDto(courseLab).getLabId()).isNull();
  }

  @Test
  void courseMapper_mapsRequestsEntitiesNestedLabsAndNulls() {
    CreateCourseInstructorRequestDto instructorDto =
        new CreateCourseInstructorRequestDto(UUID.randomUUID(), InstructorRoleEnum.OWNER);
    CreateCourseRequestDto createDto =
        new CreateCourseRequestDto(
            "Course",
            "Description",
            "Short",
            CourseStatusEnum.PUBLIC,
            "image",
            "web",
            List.of(instructorDto),
            "ONCE");
    UpdateCourseInstructorRequestDto updateInstructorDto =
        new UpdateCourseInstructorRequestDto(UUID.randomUUID(), InstructorRoleEnum.COLLABORATOR);
    UpdateCourseRequestDto updateDto =
        new UpdateCourseRequestDto(
            "Course 2",
            "Description 2",
            "Short 2",
            CourseStatusEnum.PRIVATE,
            "image2",
            "crypto",
            List.of(updateInstructorDto),
            "UNLIMITED");

    assertCourseRequestMappings(instructorDto, createDto, updateInstructorDto, updateDto);

    Course course = courseFixture();
    CourseLab courseLab = course.getCourseLabs().get(0);

    assertCourseNullMappings();
    assertCourseEntityMappings(course, courseLab);

    courseLab.setLab(null);
    assertThat(courseMapper.toLabDetailResponseDto(courseLab).getId()).isNull();
    assertThat(courseMapper.toChallengeStudentDto(courseLab).getId()).isNull();

    course.setCourseInstructors(null);
    course.setCourseLabs(null);
    course.setMcAttemptsMode(null);
    assertThat(courseMapper.toCourseDetailDto(course).getCourseInstructors()).isNull();
    assertThat(courseMapper.toPublicCourseDetailDto(course).getCourseLabs()).isNull();
    assertThat(courseMapper.toListCourseResponseDto(course).getInstructorCount()).isZero();
  }

  private void assertLabRequestMappings(
      ChallengeOptionRequestDto optionDto,
      ChallengeRequestDto challengeDto,
      CreateLabRequestDto createDto,
      UpdateLabRequestDto updateDto) {
    assertThat(labMapper.fromDto((CreateLabRequestDto) null)).isNull();
    assertThat(labMapper.fromDto((UpdateLabRequestDto) null)).isNull();
    assertThat(labMapper.fromDto((ChallengeRequestDto) null)).isNull();
    assertThat(labMapper.fromDto((ChallengeOptionRequestDto) null)).isNull();
    assertThat(labMapper.fromDto(createDto).getChallenges()).hasSize(1);
    assertThat(labMapper.fromDto(createDto).getContainerPort()).isEqualTo(8080);
    assertThat(labMapper.fromDto(createDto).getPodTtlSeconds()).isNull();
    assertThat(labMapper.fromDto(updateDto).getStatus()).isEqualTo(LabStatusEnum.DRAFT);
    assertThat(labMapper.fromDto(updateDto).getPodTtlSeconds()).isNull();
    assertThat(labMapper.fromDto(challengeDto).getOptions().get(0).isCorrect()).isTrue();
    assertThat(labMapper.fromDto(optionDto).getText()).isEqualTo("Option A");
  }

  private void assertLabNullMappings() {
    assertThat(labMapper.toCreateResponseDto(null)).isNull();
    assertThat(labMapper.toDetailResponseDto(null)).isNull();
    assertThat(labMapper.toCreatorDto(null)).isNull();
    assertThat(labMapper.toChallengeResponseDto(null)).isNull();
    assertThat(labMapper.toOptionStudentDto(null)).isNull();
    assertThat(labMapper.toOptionResponseDto(null)).isNull();
    assertThat(labMapper.toChallengeStudentDto(null)).isNull();
    assertThat(labMapper.toStudentDto(null)).isNull();
    assertThat(labMapper.toCourseLabResponseDto(null)).isNull();
  }

  private void assertLabEntityMappings(
      Lab lab, Challenge challenge, ChallengeOption option, CourseLab courseLab) {
    assertThat(labMapper.toCreateResponseDto(lab).getCreatorId()).isEqualTo(lab.getCreator().getId());
    assertThat(labMapper.toDetailResponseDto(lab).getChallenges()).hasSize(1);
    assertThat(labMapper.toCreatorDto(lab.getCreator()).getName()).isEqualTo("Alice");
    assertThat(labMapper.toChallengeResponseDto(challenge).getOptions()).hasSize(1);
    assertThat(labMapper.toOptionStudentDto(option).getText()).isEqualTo("Answer");
    assertThat(labMapper.toOptionResponseDto(option).isCorrect()).isTrue();
    assertThat(labMapper.toChallengeStudentDto(challenge).isTheory()).isFalse();
    assertThat(labMapper.toStudentDto(lab).getCreator().getName()).isEqualTo("Alice");
    assertThat(labMapper.toCourseLabResponseDto(courseLab).getLabTitle()).isEqualTo("Lab");
  }

  private void assertCourseRequestMappings(
      CreateCourseInstructorRequestDto instructorDto,
      CreateCourseRequestDto createDto,
      UpdateCourseInstructorRequestDto updateInstructorDto,
      UpdateCourseRequestDto updateDto) {
    assertThat(courseMapper.fromDto((CreateCourseInstructorRequestDto) null)).isNull();
    assertThat(courseMapper.fromDto((CreateCourseRequestDto) null)).isNull();
    assertThat(courseMapper.fromDto((UpdateCourseInstructorRequestDto) null)).isNull();
    assertThat(courseMapper.fromDto((UpdateCourseRequestDto) null)).isNull();
    assertThat(courseMapper.fromDto(instructorDto).getInstructorRole()).isEqualTo(InstructorRoleEnum.OWNER);
    assertThat(courseMapper.fromDto(createDto).getMcAttemptsMode()).isEqualTo(McAttemptsMode.ONCE);
    assertThat(courseMapper.fromDto(updateInstructorDto).getInstructorRole()).isEqualTo(InstructorRoleEnum.COLLABORATOR);
    assertThat(courseMapper.fromDto(updateDto).getMcAttemptsMode()).isEqualTo(McAttemptsMode.UNLIMITED);
  }

  private void assertCourseNullMappings() {
    assertThat(courseMapper.toDto(null)).isNull();
    assertThat(courseMapper.toCourseDetailDto(null)).isNull();
    assertThat(courseMapper.toPublicCourseDetailDto(null)).isNull();
    assertThat(courseMapper.toLabDetailResponseDto(null)).isNull();
    assertThat(courseMapper.toChallengeStudentDto(null)).isNull();
    assertThat(courseMapper.toListCourseResponseDto(null)).isNull();
  }

  private void assertCourseEntityMappings(Course course, CourseLab courseLab) {
    assertThat(courseMapper.toDto(course).getCourseInstructors()).hasSize(1);
    assertThat(courseMapper.toCourseDetailDto(course).getCourseLabs()).hasSize(1);
    assertThat(courseMapper.toCourseDetailDto(course).getMcAttemptsMode()).isEqualTo("ONCE");
    assertThat(courseMapper.toPublicCourseDetailDto(course).getCourseLabs()).hasSize(1);
    assertThat(courseMapper.toLabDetailResponseDto(courseLab).getCreator().getName())
        .isEqualTo("Alice");
    assertThat(courseMapper.toChallengeStudentDto(courseLab).getChallenges()).hasSize(1);
    assertThat(courseMapper.toListCourseResponseDto(course).getInstructorCount()).isEqualTo(1);
    assertThat(courseMapper.mapInstructorCount(null)).isZero();
  }

  private static Course courseFixture() {
    Course course = new Course();
    course.setId(UUID.randomUUID());
    course.setTitle("Course");
    course.setDescription("Description");
    course.setShortDescription("Short");
    course.setStatus(CourseStatusEnum.PUBLIC);
    course.setImageUrl("image");
    course.setTopic("web");
    course.setInviteCode("ABC123");
    course.setMcAttemptsMode(McAttemptsMode.ONCE);
    course.setCreatedAt(LocalDateTime.now());
    course.setUpdatedAt(LocalDateTime.now());

    User instructorUser = user("Instructor");
    CourseInstructor instructor = new CourseInstructor();
    instructor.setId(UUID.randomUUID());
    instructor.setInstructorRole(InstructorRoleEnum.OWNER);
    instructor.setAccepted(true);
    instructor.setInstructor(instructorUser);
    instructor.setInvitedAt(LocalDateTime.now());
    instructor.setAcceptedAt(LocalDateTime.now());
    instructor.setCreatedAt(LocalDateTime.now());
    instructor.setUpdatedAt(LocalDateTime.now());
    course.addCourseInstructor(instructor);

    CourseLab courseLab = new CourseLab();
    courseLab.setId(UUID.randomUUID());
    courseLab.setLab(labFixture());
    courseLab.setOrderIndex(1);
    courseLab.setDueAt(LocalDateTime.now());
    course.addCourseLab(courseLab);
    return course;
  }

  private static Lab labFixture() {
    Lab lab = new Lab();
    lab.setId(UUID.randomUUID());
    lab.setTitle("Lab");
    lab.setDescription("Long");
    lab.setStatus(LabStatusEnum.PUBLIC);
    lab.setDifficulty(LabDifficultyEnum.EASY);
    lab.setDockerImage("ghcr.io/acme/lab:latest");
    lab.setMaxScore(10);
    lab.setCreator(user("Alice"));
    lab.setCreatedAt(LocalDateTime.now());
    lab.setUpdatedAt(LocalDateTime.now());

    Challenge challenge = new Challenge();
    challenge.setId(UUID.randomUUID());
    challenge.setLab(lab);
    challenge.setTitle("Challenge");
    challenge.setDescription("Description");
    challenge.setFlag("flag");
    challenge.setOrderIndex(1);
    challenge.setType(ChallengeType.MULTIPLE_CHOICE);
    challenge.setPoints(5);
    challenge.setHint("hint");

    ChallengeOption option = new ChallengeOption();
    option.setId(UUID.randomUUID());
    option.setChallenge(challenge);
    option.setText("Answer");
    option.setCorrect(true);
    option.setOrderIndex(1);
    challenge.getOptions().add(option);
    lab.getChallenges().add(challenge);
    return lab;
  }

  private static User user(String name) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setName(name);
    user.setEmail(name.toLowerCase() + "@example.com");
    user.setUsername(name.toLowerCase());
    user.setFirstName(name);
    user.setLastName("Example");
    user.setPicture("picture");
    user.setTitle("Prof");
    user.setTotalSecondsOnline(42);
    return user;
  }
}
