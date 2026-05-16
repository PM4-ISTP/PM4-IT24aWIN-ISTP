package com.pm4.istp.course.repositories;

import com.pm4.istp.admin.dto.AdminLabListItemDto;
import com.pm4.istp.course.db.entities.Lab;
import com.pm4.istp.course.dto.ListLabResponseDto;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LabRepository extends JpaRepository<Lab, UUID> {
  Optional<Lab> findByIdAndDeletedAtIsNull(UUID id);

  Page<Lab> findByCreatorId(UUID creatorId, Pageable pageable);

  @Query(
      value =
          """
          select new com.pm4.istp.course.dto.ListLabResponseDto(
            c.id,
            c.title,
            c.status,
            c.difficulty,
            c.maxScore,
            c.dockerImage,
            c.containerPort,
            c.podTtlSeconds,
            c.creator.name,
            (select count(cc) from CourseLab cc where cc.lab = c),
            c.updatedAt
           )
           from Lab c
           where c.creator.id = :creatorId and c.deletedAt is null
           """,
      countQuery =
          """
           select count(c)
           from Lab c
           where c.creator.id = :creatorId and c.deletedAt is null
           """)
  Page<ListLabResponseDto> findListChallengesForCreator(
      @Param("creatorId") UUID creatorId, Pageable pageable);

  @Query(
      value =
          """
          select new com.pm4.istp.course.dto.ListLabResponseDto(
            c.id,
            c.title,
            c.status,
            c.difficulty,
            c.maxScore,
            c.dockerImage,
            c.containerPort,
            c.podTtlSeconds,
            c.creator.name,
            (select count(cc) from CourseLab cc where cc.lab = c),
            c.updatedAt
          )
          from Lab c
          where c.deletedAt is null
          and (
            (c.creator.id = :userId and c.status = com.pm4.istp.course.db.entities.LabStatusEnum.PRIVATE)
            or c.status = com.pm4.istp.course.db.entities.LabStatusEnum.PUBLIC
          )
          and lower(c.title) like lower(concat('%', :search, '%'))
          """,
      countQuery =
          """
          select count(c)
          from Lab c
          where c.deletedAt is null
          and (
            (c.creator.id = :userId and c.status = com.pm4.istp.course.db.entities.LabStatusEnum.PRIVATE)
            or c.status = com.pm4.istp.course.db.entities.LabStatusEnum.PUBLIC
          )
          and lower(c.title) like lower(concat('%', :search, '%'))
          """)
  Page<ListLabResponseDto> searchAvailableLabs(
      @Param("userId") UUID userId, @Param("search") String search, Pageable pageable);

  @Query(
      value =
          """
          select new com.pm4.istp.admin.dto.AdminLabListItemDto(
            c.id,
            c.title,
            c.description,
            c.status,
            c.difficulty,
            c.deletedAt is not null,
            c.dockerImage,
            c.containerPort,
            c.maxScore,
            (select count(cc) from CourseLab cc where cc.lab = c),
            c.createdAt,
            c.updatedAt,
            c.creator.id,
            c.creator.name,
            c.creator.username
          )
          from Lab c
          """,
      countQuery =
          """
          select count(c)
          from Lab c
          """)
  Page<AdminLabListItemDto> findAllChallengesForAdmin(Pageable pageable);

  @Query(
      value =
          """
          select new com.pm4.istp.admin.dto.AdminLabListItemDto(
            c.id,
            c.title,
            c.description,
            c.status,
            c.difficulty,
            c.deletedAt is not null,
            c.dockerImage,
            c.containerPort,
            c.maxScore,
            (select count(cc) from CourseLab cc where cc.lab = c),
            c.createdAt,
            c.updatedAt,
            c.creator.id,
            c.creator.name,
            c.creator.username
          )
          from Lab c
          where lower(c.title) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.description, '')) like lower(concat('%', :query, '%'))
          """,
      countQuery =
          """
          select count(c)
          from Lab c
          where lower(c.title) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.description, '')) like lower(concat('%', :query, '%'))
          """)
  Page<AdminLabListItemDto> findAllChallengesForAdminByQuery(
      @Param("query") String query, Pageable pageable);

  @Query(
      value =
          """
          select new com.pm4.istp.admin.dto.AdminLabListItemDto(
            c.id,
            c.title,
            c.description,
            c.status,
            c.difficulty,
            c.deletedAt is not null,
            c.dockerImage,
            c.containerPort,
            c.maxScore,
            (select count(cc) from CourseLab cc where cc.lab = c),
            c.createdAt,
            c.updatedAt,
            c.creator.id,
            c.creator.name,
            c.creator.username
          )
          from Lab c
          where c.deletedAt is not null
          """,
      countQuery =
          """
          select count(c)
          from Lab c
          where c.deletedAt is not null
          """)
  Page<AdminLabListItemDto> findRemovedChallengesForAdmin(Pageable pageable);

  @Query(
      value =
          """
          select new com.pm4.istp.admin.dto.AdminLabListItemDto(
            c.id,
            c.title,
            c.description,
            c.status,
            c.difficulty,
            c.deletedAt is not null,
            c.dockerImage,
            c.containerPort,
            c.maxScore,
            (select count(cc) from CourseLab cc where cc.lab = c),
            c.createdAt,
            c.updatedAt,
            c.creator.id,
            c.creator.name,
            c.creator.username
          )
          from Lab c
          where c.deletedAt is not null
            and (
              lower(c.title) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.description, '')) like lower(concat('%', :query, '%'))
            )
          """,
      countQuery =
          """
          select count(c)
          from Lab c
          where c.deletedAt is not null
            and (
              lower(c.title) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.description, '')) like lower(concat('%', :query, '%'))
            )
          """)
  Page<AdminLabListItemDto> findRemovedChallengesForAdminByQuery(
      @Param("query") String query, Pageable pageable);
}
