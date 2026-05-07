package com.pm4.istp.course.services;

import com.pm4.istp.course.db.CreateCourseRequest;
import com.pm4.istp.course.db.UpdateCourseRequest;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.dto.CourseChallengeSubmissionEntryDto;
import com.pm4.istp.course.dto.CourseLabDeadlineDto;
import com.pm4.istp.course.dto.CourseLabItemDto;
import com.pm4.istp.course.dto.CourseLabSubmissionDetailDto;
import com.pm4.istp.course.dto.CourseLabSubmissionsResponseDto;
import com.pm4.istp.course.dto.ListCourseResponseDto;
import com.pm4.istp.course.dto.UpdateCourseChallengeScoreRequestDto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseService {
  Course createCourse(UUID userId, CreateCourseRequest course);

  Course getCourse(UUID userId, UUID courseId);

  Course enrollInCourse(UUID userId, UUID courseId);

  Course updateCourse(UUID userId, UUID courseId, UpdateCourseRequest request);

  void deleteCourse(UUID userId, UUID courseId);

  Course updateCourseChallenges(UUID userId, UUID courseId, List<CourseLabItemDto> labs);

  CourseLabSubmissionsResponseDto getCourseChallengeSubmissions(UUID userId, UUID courseId);

  CourseLabSubmissionDetailDto getCourseLabSubmissionDetails(
      UUID instructorUserId, UUID courseId, UUID participantId, UUID labId);

  CourseChallengeSubmissionEntryDto updateCourseChallengeScore(
      UUID instructorUserId,
      UUID courseId,
      UUID participantId,
      UUID challengeId,
      UpdateCourseChallengeScoreRequestDto request);

  List<CourseLabDeadlineDto> listUpcomingDeadlines(UUID userId);

  Page<ListCourseResponseDto> listCoursesForInstructors(UUID instructorId, Pageable pageable);

  Page<ListCourseResponseDto> listUserEnrollments(UUID userId, Pageable pageable);

  Page<ListCourseResponseDto> listPublishedCourses(String query, String topic, Pageable pageable);

  Course joinByInviteCode(String code, UUID studentId);

  Course regenerateInviteCode(UUID courseId, UUID userId);

  void removeParticipant(UUID ownerId, UUID courseId, UUID participantId);

  /** Allows a student to remove themselves from a course they are enrolled in. */
  void leaveCourse(UUID userId, UUID courseId);
}
