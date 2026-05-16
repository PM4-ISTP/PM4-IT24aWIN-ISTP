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

  Optional<Course> findByIdAndDeletedAtIsNull(UUID id);

  @Query("select c from Course c where c.inviteCode = :inviteCode and c.deletedAt is null")
  Optional<Course> findByInviteCode(@Param("inviteCode") String inviteCode);

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
          and c.deletedAt is null
          group by c.id, c.title, c.description, c.shortDescription, c.isPublished, c.isPrivate, c.createdAt, c.updatedAt, c.imageUrl, c.topic, ownerUser.name, ownerUser.picture, ownerUser.title
          """,
      countQuery =
          """
          select count(c)
          from Course c
          where c.deletedAt is null
            and exists (
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
          where c.deletedAt is null
            and ((c.isPublished = true or c.isPrivate = true) and exists (
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
          where c.deletedAt is null
            and (c.isPublished = true or c.isPrivate = true)
            and exists (
              select 1
              from CourseEnrollment eFilter
              where eFilter.course = c and eFilter.participant.id = :userId
            )
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
          where c.isPublished = true and c.isPrivate = false and c.deletedAt is null
          group by c.id, c.title, c.description, c.shortDescription, c.isPublished, c.isPrivate, c.createdAt, c.updatedAt, c.imageUrl, c.topic, ownerUser.name, ownerUser.picture, ownerUser.title
          """,
      countQuery =
          """
          select count(c)
          from Course c
          where c.isPublished = true and c.isPrivate = false and c.deletedAt is null
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
          where c.isPublished = true and c.isPrivate = false and c.topic = :topic and c.deletedAt is null
          group by c.id, c.title, c.description, c.shortDescription, c.isPublished, c.isPrivate, c.createdAt, c.updatedAt, c.imageUrl, c.topic, ownerUser.name, ownerUser.picture, ownerUser.title
          """,
      countQuery =
          """
          select count(c)
          from Course c
          where c.isPublished = true and c.isPrivate = false and c.topic = :topic and c.deletedAt is null
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
          where c.isPublished = true and c.isPrivate = false and c.topic = :topic and c.deletedAt is null
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
          where c.isPublished = true and c.isPrivate = false and c.topic = :topic and c.deletedAt is null
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
          where c.isPublished = true and c.isPrivate = false and c.deletedAt is null
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
          where c.isPublished = true and c.isPrivate = false and c.deletedAt is null
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
            c.deletedAt is not null,
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
          where c.deletedAt is null
          """,
      countQuery =
          """
          select count(distinct c.id)
          from Course c
          where c.deletedAt is null
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
            c.deletedAt is not null,
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
          where c.deletedAt is null
            and (
              lower(c.title) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.shortDescription, '')) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.description, '')) like lower(concat('%', :query, '%'))
            )
          """,
      countQuery =
          """
          select count(distinct c.id)
          from Course c
          where c.deletedAt is null
            and (
              lower(c.title) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.shortDescription, '')) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.description, '')) like lower(concat('%', :query, '%'))
            )
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
      join fetch c.courseLabs cc
      join fetch cc.lab ch
      join c.courseEnrollments ce
      where cc.lab.id = :labId and ce.participant.id = :userId and c.deletedAt is null
      """)
  List<Course> findCoursesByChallengeIdAndEnrolledUserId(
      @Param("labId") UUID labId, @Param("userId") UUID userId);
}
