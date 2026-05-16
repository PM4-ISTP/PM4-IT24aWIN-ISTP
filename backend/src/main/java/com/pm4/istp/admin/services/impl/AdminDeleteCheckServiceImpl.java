package com.pm4.istp.admin.services.impl;

import com.pm4.istp.admin.dto.DeleteCheckBlockerDto;
import com.pm4.istp.admin.dto.DeleteCheckResponseDto;
import com.pm4.istp.admin.services.AdminDeleteCheckService;
import com.pm4.istp.course.exceptions.CourseNotFoundException;
import com.pm4.istp.course.exceptions.LabNotFoundException;
import com.pm4.istp.course.repositories.ChallengeCompletionRepository;
import com.pm4.istp.course.repositories.ChallengeRepository;
import com.pm4.istp.course.repositories.CourseChallengeScoreOverrideRepository;
import com.pm4.istp.course.repositories.CourseEnrollmentRepository;
import com.pm4.istp.course.repositories.CourseLabRepository;
import com.pm4.istp.course.repositories.CourseRepository;
import com.pm4.istp.course.repositories.LabRepository;
import com.pm4.istp.course.repositories.StudentFlagSubmissionRepository;
import com.pm4.istp.course.repositories.StudentOptionSubmissionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDeleteCheckServiceImpl implements AdminDeleteCheckService {
  private static final String COURSE_NOT_FOUND_MSG = "Course with ID '%s' not found";
  private static final String LAB_NOT_FOUND_MSG = "Lab with ID '%s' not found";

  private final CourseRepository courseRepository;
  private final LabRepository labRepository;
  private final CourseLabRepository courseLabRepository;
  private final CourseEnrollmentRepository courseEnrollmentRepository;
  private final CourseChallengeScoreOverrideRepository courseChallengeScoreOverrideRepository;
  private final ChallengeRepository challengeRepository;
  private final ChallengeCompletionRepository challengeCompletionRepository;
  private final StudentFlagSubmissionRepository studentFlagSubmissionRepository;
  private final StudentOptionSubmissionRepository studentOptionSubmissionRepository;

  @Override
  public DeleteCheckResponseDto checkCourse(UUID courseId) {
    courseRepository
        .findById(courseId)
        .orElseThrow(
            () -> new CourseNotFoundException(String.format(COURSE_NOT_FOUND_MSG, courseId)));

    List<DeleteCheckBlockerDto> blockers = new ArrayList<>();

    long assignedLabs = courseLabRepository.countByCourseId(courseId);
    if (assignedLabs > 0) {
      blockers.add(new DeleteCheckBlockerDto("Course still contains labs", assignedLabs));
    }

    long enrollments = courseEnrollmentRepository.countByCourseId(courseId);
    if (enrollments > 0) {
      blockers.add(new DeleteCheckBlockerDto("Active enrollments still exist", enrollments));
    }

    boolean hasOverrides = courseChallengeScoreOverrideRepository.existsByCourseId(courseId);
    if (hasOverrides) {
      blockers.add(new DeleteCheckBlockerDto("Course score overrides still exist", 1));
    }

    return new DeleteCheckResponseDto(blockers.isEmpty(), List.copyOf(blockers));
  }

  @Override
  public DeleteCheckResponseDto checkLab(UUID labId) {
    labRepository
        .findById(labId)
        .orElseThrow(() -> new LabNotFoundException(String.format(LAB_NOT_FOUND_MSG, labId)));

    List<DeleteCheckBlockerDto> blockers = new ArrayList<>();

    long courseAssignments = courseLabRepository.countByChallengeId(labId);
    if (courseAssignments > 0) {
      blockers.add(
          new DeleteCheckBlockerDto("Lab is still assigned to one or more courses", courseAssignments));
    }

    long challengeCount = challengeRepository.countByLabId(labId);
    if (challengeCount > 0) {
      blockers.add(new DeleteCheckBlockerDto("Lab still contains challenges", challengeCount));
    }

    if (challengeCompletionRepository.existsByLabId(labId)) {
      blockers.add(new DeleteCheckBlockerDto("Challenge completions still exist", 1));
    }

    if (studentFlagSubmissionRepository.existsByLabId(labId)) {
      blockers.add(new DeleteCheckBlockerDto("Flag submissions still exist", 1));
    }

    if (studentOptionSubmissionRepository.existsByLabId(labId)) {
      blockers.add(new DeleteCheckBlockerDto("Multiple-choice submissions still exist", 1));
    }

    if (courseChallengeScoreOverrideRepository.existsByLabId(labId)) {
      blockers.add(new DeleteCheckBlockerDto("Course score overrides still exist", 1));
    }

    return new DeleteCheckResponseDto(blockers.isEmpty(), List.copyOf(blockers));
  }
}
