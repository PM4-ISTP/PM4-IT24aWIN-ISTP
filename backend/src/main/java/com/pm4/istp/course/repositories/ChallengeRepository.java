package com.pm4.istp.course.repositories;

import com.pm4.istp.course.db.entities.Challenge;
import com.pm4.istp.admin.dto.AdminChallengeListItemDto;
import com.pm4.istp.course.dto.ListChallengeResponseDto;
import com.pm4.istp.course.db.entities.ChallengeDifficultyEnum;
import com.pm4.istp.course.db.entities.ChallengeStatusEnum;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, UUID> {

  Page<Challenge> findByCreatorId(UUID creatorId, Pageable pageable);

  @Query(
      value =
          """
          select new com.pm4.istp.course.dto.ListChallengeResponseDto(
            c.id,
            c.title,
            c.shortDescription,
            c.status,
            c.difficulty,
            c.maxScore,
            c.creator.name,
            (select count(cc) from CourseChallenge cc where cc.challenge = c),
            c.updatedAt
          )
          from Challenge c
          where c.creator.id = :creatorId
          """,
      countQuery =
          """
          select count(c)
          from Challenge c
          where c.creator.id = :creatorId
          """)
  Page<ListChallengeResponseDto> findListChallengesForCreator(
      @Param("creatorId") UUID creatorId, Pageable pageable);

  @Query(
      value =
          """
          select new com.pm4.istp.course.dto.ListChallengeResponseDto(
            c.id,
            c.title,
            c.shortDescription,
            c.status,
            c.difficulty,
            c.maxScore,
            c.creator.name,
            (select count(cc) from CourseChallenge cc where cc.challenge = c),
            c.updatedAt
          )
          from Challenge c
          where (
            (c.creator.id = :userId and c.status = com.pm4.istp.course.db.entities.ChallengeStatusEnum.PRIVATE)
            or c.status = com.pm4.istp.course.db.entities.ChallengeStatusEnum.PUBLIC
          )
          and lower(c.title) like lower(concat('%', :search, '%'))
          """,
      countQuery =
          """
          select count(c)
          from Challenge c
          where (
            (c.creator.id = :userId and c.status = com.pm4.istp.course.db.entities.ChallengeStatusEnum.PRIVATE)
            or c.status = com.pm4.istp.course.db.entities.ChallengeStatusEnum.PUBLIC
          )
          and lower(c.title) like lower(concat('%', :search, '%'))
          """)
  Page<ListChallengeResponseDto> searchAvailableChallenges(
      @Param("userId") UUID userId, @Param("search") String search, Pageable pageable);

  @Query(
      value =
          """
          select new com.pm4.istp.admin.dto.AdminChallengeListItemDto(
            c.id,
            c.title,
            c.shortDescription,
            c.description,
            c.status,
            c.difficulty,
            c.maxScore,
            (select count(cc) from CourseChallenge cc where cc.challenge = c),
            c.createdAt,
            c.updatedAt,
            c.creator.id,
            c.creator.name,
            c.creator.username
          )
          from Challenge c
          where (:query is null
              or lower(c.title) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.shortDescription, '')) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.description, '')) like lower(concat('%', :query, '%')))
            and (:owner is null
              or lower(coalesce(c.creator.name, '')) like lower(concat('%', :owner, '%'))
              or lower(coalesce(c.creator.username, '')) like lower(concat('%', :owner, '%')))
            and (:status is null or c.status = :status)
            and (:difficulty is null or c.difficulty = :difficulty)
          """,
      countQuery =
          """
          select count(c)
          from Challenge c
          where (:query is null
              or lower(c.title) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.shortDescription, '')) like lower(concat('%', :query, '%'))
              or lower(coalesce(c.description, '')) like lower(concat('%', :query, '%')))
            and (:owner is null
              or lower(coalesce(c.creator.name, '')) like lower(concat('%', :owner, '%'))
              or lower(coalesce(c.creator.username, '')) like lower(concat('%', :owner, '%')))
            and (:status is null or c.status = :status)
            and (:difficulty is null or c.difficulty = :difficulty)
          """)
  Page<AdminChallengeListItemDto> findAllChallengesForAdmin(
      @Param("query") String query,
      @Param("owner") String owner,
      @Param("status") ChallengeStatusEnum status,
      @Param("difficulty") ChallengeDifficultyEnum difficulty,
      Pageable pageable);
}
