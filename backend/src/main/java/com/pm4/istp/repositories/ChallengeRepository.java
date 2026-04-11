package com.pm4.istp.repositories;

import com.pm4.istp.domain.entites.Challenge;
import com.pm4.istp.dto.ListChallengeResponseDto;
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
          select new com.pm4.istp.dto.ListChallengeResponseDto(
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
          select new com.pm4.istp.dto.ListChallengeResponseDto(
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
          where (c.creator.id = :userId or c.status = com.pm4.istp.domain.entites.ChallengeStatusEnum.PUBLIC)
          and lower(c.title) like lower(concat('%', :search, '%'))
          """,
      countQuery =
          """
          select count(c)
          from Challenge c
          where (c.creator.id = :userId or c.status = com.pm4.istp.domain.entites.ChallengeStatusEnum.PUBLIC)
          and lower(c.title) like lower(concat('%', :search, '%'))
          """)
  Page<ListChallengeResponseDto> searchAvailableChallenges(
      @Param("userId") UUID userId, @Param("search") String search, Pageable pageable);
}
