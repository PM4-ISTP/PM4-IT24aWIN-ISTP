package com.pm4.istp.course.repositories;

import com.pm4.istp.admin.dto.AdminCourseListItemDto;
import com.pm4.istp.course.db.entities.Course;
import com.pm4.istp.course.dto.ListCourseResponseDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {
  Page<Course> findByCourseInstructorsInstructorId(UUID instructorId, Pageable pageable);

  Optional<Course> findByInviteCode(String inviteCode);

  boolean existsByInviteCode(String inviteCode);

  @Query(
      value =
          """
          select new com.pm4.istp.course.dto.ListCourseResponseDto(
            c.id,
            c.title,
            c.description,
            c.shortDescription,
            c.isPublished,
            c.isPrivate,
            count(distinct ciAll.id),
            c.createdAt,
            c.updatedAt,
            c.imageUrl,
            c.topic,
            ownerUser.name,
            ownerUser.picture,
            ownerUser.title
          )
          from Course c
          left join c.courseInstructors ciAll
          left join c.courseInstructors ciOwner on ciOwner.instructorRole = com.pm4.istp.course.db.InstructorRoleEnum.OWNER
          left join ciOwner.instructor ownerUser
          where exists (
            select 1
            from CourseInstructor ciFilter
            where ciFilter.course = c and ciFilter.instructor.id = :instructorId
          )
          group by c.id, c.title, c.description, c.shortDescription, c.isPublished, c.isPrivate, c.createdAt, c.updatedAt, c.imageUrl, c.topic, ownerUser.name, ownerUser.picture, ownerUser.title
          """,
      countQuery =
          """
          select count(c)
          from Course c
          where exists (
            select 1
            from CourseInstructor ciFilter
            where ciFilter.course = c and ciFilter.instructor.id = :instructorId
          )
          """)
  Page<ListCourseResponseDto> findListCoursesForInstructor(
      @Param("instructorId") UUID instructorId, Pageable pageable);

  @Query(
      value =
          """
          select new com.pm4.istp.course.dto.ListCourseResponseDto(
            c.id,
            c.title,
            c.description,
            c.shortDescription,
            c.isPublished,
            c.isPrivate,
            count(distinct ciAll.id),
            c.createdAt,
            c.updatedAt,
            c.imageUrl,
            c.topic,
            ownerUser.name,
            ownerUser.picture,
            ownerUser.title
          )
          from Course c
          left join c.courseInstructors ciAll
          left join c.courseInstructors ciOwner
            on ciOwner.instructorRole = com.pm4.istp.course.db.InstructorRoleEnum.OWNER
          left join ciOwner.instructor ownerUser
          where
            ((c.isPublished = true or c.isPrivate = true) and exists (
              select 1
              from CourseEnrollment eFilter
              where eFilter.course = c and eFilter.participant.id = :userId
            ))
          group by c.id, c.title, c.description, c.shortDescription, c.isPublished,
            c.isPrivate, c.createdAt, c.updatedAt, c.imageUrl, c.topic,
            ownerUser.name, ownerUser.picture, ownerUser.title
          """,
      countQuery =
          """
          select count(distinct c.id)
          from Course c
          where
            ((c.isPublished = true or c.isPrivate = true) and exists (
              select 1
              from CourseEnrollment eFilter
              where eFilter.course = c and eFilter.participant.id = :userId
            ))
          """)
  Page<ListCourseResponseDto> findListEnrollmentsForUser(
      @Param("userId") UUID userId, Pageable pageable);

  @Query(
      value =
          """
          select new com.pm4.istp.course.dto.ListCourseResponseDto(
            c.id,
            c.title,
            c.description,
            c.shortDescription,
            c.isPublished,
            c.isPrivate,
            count(distinct ciAll.id),
            c.createdAt,
            c.updatedAt,
            c.imageUrl,
            c.topic,
            ownerUser.name,
            ownerUser.picture,
            ownerUser.title
          )
          from Course c
          left join c.courseInstructors ciAll
          left join c.courseInstructors ciOwner on ciOwner.instructorRole = com.pm4.istp.course.db.InstructorRoleEnum.OWNER
          left join ciOwner.instructor ownerUser
          where c.isPublished = true and c.isPrivate = false
          group by c.id, c.title, c.description, c.shortDescription, c.isPublished, c.isPrivate, c.createdAt, c.updatedAt, c.imageUrl, c.topic, ownerUser.name, ownerUser.picture, ownerUser.title
          """,
      countQuery =
          """
          select count(c)
          from Course c
          where c.isPublished = true and c.isPrivate = false
          """)
  Page<ListCourseResponseDto> findPublishedCourses(Pageable pageable);

  @Query(
      value =
          """
          select new com.pm4.istp.course.dto.ListCourseResponseDto(
            c.id,
            c.title,
            c.description,
            c.shortDescription,
            c.isPublished,
            c.isPrivate,
            count(distinct ciAll.id),
            c.createdAt,
            c.updatedAt,
            c.imageUrl,
            c.topic,
            ownerUser.name,
            ownerUser.picture,
            ownerUser.title
          )
          from Course c
          left join c.courseInstructors ciAll
          left join c.courseInstructors ciOwner on ciOwner.instructorRole = com.pm4.istp.course.db.InstructorRoleEnum.OWNER
          left join ciOwner.instructor ownerUser
          where c.isPublished = true and c.isPrivate = false and c.topic = :topic
          group by c.id, c.title, c.description, c.shortDescription, c.isPublished, c.isPrivate, c.createdAt, c.updatedAt, c.imageUrl, c.topic, ownerUser.name, ownerUser.picture, ownerUser.title
          """,
      countQuery =
          """
          select count(c)
          from Course c
          where c.isPublished = true and c.isPrivate = false and c.topic = :topic
          """)
  Page<ListCourseResponseDto> findPublishedCoursesByTopic(
      @Param("topic") String topic, Pageable pageable);

  @Query(
      value =
          """
          select new com.pm4.istp.course.dto.ListCourseResponseDto(
            c.id,
            c.title,
            c.description,
            c.shortDescription,
            c.isPublished,
            c.isPrivate,
            count(distinct ciAll.id),
            c.createdAt,
            c.updatedAt,
            c.imageUrl,
            c.topic,
            ownerUser.name,
            ownerUser.picture,
            ownerUser.title
          )
          from Course c
          left join c.courseInstructors ciAll
          left join c.courseInstructors ciOwner on ciOwner.instructorRole = com.pm4.istp.course.db.InstructorRoleEnum.OWNER
          left join ciOwner.instructor ownerUser
          where c.isPublished = true and c.isPrivate = false and c.topic = :topic
            and (
              lower(c.title) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.shortDescription, '')) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.description, '')) like lower(concat('%', :query, '%'))
            )
          group by c.id, c.title, c.description, c.shortDescription, c.isPublished, c.isPrivate, c.createdAt, c.updatedAt, c.imageUrl, c.topic, ownerUser.name, ownerUser.picture, ownerUser.title
          """,
      countQuery =
          """
          select count(c)
          from Course c
          where c.isPublished = true and c.isPrivate = false and c.topic = :topic
            and (
              lower(c.title) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.shortDescription, '')) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.description, '')) like lower(concat('%', :query, '%'))
            )
          """)
  Page<ListCourseResponseDto> findPublishedCoursesByQueryAndTopic(
      @Param("query") String query, @Param("topic") String topic, Pageable pageable);

  @Query(
      value =
          """
          select new com.pm4.istp.course.dto.ListCourseResponseDto(
            c.id,
            c.title,
            c.description,
            c.shortDescription,
            c.isPublished,
            c.isPrivate,
            count(distinct ciAll.id),
            c.createdAt,
            c.updatedAt,
            c.imageUrl,
            c.topic,
            ownerUser.name,
            ownerUser.picture,
            ownerUser.title
          )
          from Course c
          left join c.courseInstructors ciAll
          left join c.courseInstructors ciOwner on ciOwner.instructorRole = com.pm4.istp.course.db.InstructorRoleEnum.OWNER
          left join ciOwner.instructor ownerUser
          where c.isPublished = true and c.isPrivate = false
            and (
              lower(c.title) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.shortDescription, '')) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.description, '')) like lower(concat('%', :query, '%'))
            )
          group by c.id, c.title, c.description, c.shortDescription, c.isPublished, c.isPrivate, c.createdAt, c.updatedAt, c.imageUrl, c.topic, ownerUser.name, ownerUser.picture, ownerUser.title
          """,
      countQuery =
          """
          select count(c)
          from Course c
          where c.isPublished = true and c.isPrivate = false
            and (
              lower(c.title) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.shortDescription, '')) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.description, '')) like lower(concat('%', :query, '%'))
            )
          """)
  Page<ListCourseResponseDto> findPublishedCoursesByQuery(
      @Param("query") String query, Pageable pageable);

  @Query(
      value =
          """
          select distinct new com.pm4.istp.admin.dto.AdminCourseListItemDto(
            c.id,
            c.title,
            c.description,
            c.shortDescription,
            c.isPublished,
            c.isPrivate,
            c.createdAt,
            c.updatedAt,
            c.topic,
            c.imageUrl,
            ownerUser.id,
            ownerUser.name,
            ownerUser.username
          )
          from Course c
          left join c.courseInstructors ciOwner
            on ciOwner.instructorRole = com.pm4.istp.course.db.InstructorRoleEnum.OWNER
          left join ciOwner.instructor ownerUser
          """,
      countQuery =
          """
          select count(distinct c.id)
          from Course c
          """)
  Page<AdminCourseListItemDto> findAllCoursesForAdmin(Pageable pageable);

  @Query(
      value =
          """
          select distinct new com.pm4.istp.admin.dto.AdminCourseListItemDto(
            c.id,
            c.title,
            c.description,
            c.shortDescription,
            c.isPublished,
            c.isPrivate,
            c.createdAt,
            c.updatedAt,
            c.topic,
            c.imageUrl,
            ownerUser.id,
            ownerUser.name,
            ownerUser.username
          )
          from Course c
          left join c.courseInstructors ciOwner
            on ciOwner.instructorRole = com.pm4.istp.course.db.InstructorRoleEnum.OWNER
          left join ciOwner.instructor ownerUser
          where lower(c.title) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.shortDescription, '')) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.description, '')) like lower(concat('%', :query, '%'))
          """,
      countQuery =
          """
          select count(distinct c.id)
          from Course c
          where lower(c.title) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.shortDescription, '')) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.description, '')) like lower(concat('%', :query, '%'))
          """)
  Page<AdminCourseListItemDto> findAllCoursesForAdminByQuery(
      @Param("query") String query, Pageable pageable);

  @Modifying
  @Query(
      "update Course c set c.topic = null, c.updatedAt = CURRENT_TIMESTAMP where c.topic = :topic")
  int clearTopic(@Param("topic") String topic);

  @Query(
      """
      select distinct c from Course c
      join fetch c.courseChallenges cc
      join fetch cc.challenge ch
      join fetch ch.subTasks
      join c.courseEnrollments ce
      where cc.challenge.id = :challengeId and ce.participant.id = :userId
      """)
  List<Course> findCoursesByChallengeIdAndEnrolledUserId(
      @Param("challengeId") UUID challengeId, @Param("userId") UUID userId);
}
