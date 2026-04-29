"use server";

import type { components } from "@/src/shared/lib/api/schema";
import { withActionResult, withActionResultNoContent } from "@/src/shared/lib/api/actionResult";
import type { ActionResult } from "@/src/shared/lib/api/actionResult";
import { springPageableSerializer } from "@/src/shared/lib/api/querySerializers";

// Re-export generated types with convenient aliases
export type ChallengeStatusEnum = NonNullable<
  components["schemas"]["ChallengeDetailResponseDto"]["status"]
>;
export type ChallengeDifficultyEnum = NonNullable<
  components["schemas"]["ChallengeDetailResponseDto"]["difficulty"]
>;

export type ChallengeCreatorResponseDto = components["schemas"]["ChallengeCreatorResponseDto"];
export type CreateChallengeRequestDto = components["schemas"]["CreateChallengeRequestDto"];
export type UpdateChallengeRequestDto = components["schemas"]["UpdateChallengeRequestDto"];
export type CreateChallengeResponseDto = components["schemas"]["CreateChallengeResponseDto"];
export type ChallengeDetailResponseDto = components["schemas"]["ChallengeDetailResponseDto"];
export type ChallengeStudentDto = components["schemas"]["ChallengeStudentDto"];
export type SubTaskSubmissionRequestDto = components["schemas"]["SubTaskSubmissionRequestDto"];
export type SubTaskSubmissionResponseDto = components["schemas"]["SubTaskSubmissionResponseDto"];
export type ListChallengeResponseDto = components["schemas"]["ListChallengeResponseDto"];
export type PageListChallengeResponseDto = components["schemas"]["PageListChallengeResponseDto"];
export type CourseChallengeItemDto = components["schemas"]["CourseChallengeItemDto"];
export type UpdateCourseChallengesRequestDto =
  components["schemas"]["UpdateCourseChallengesRequestDto"];
export type VisibilityImpactResponseDto = components["schemas"]["VisibilityImpactResponseDto"];
export type CourseDetailResponseDto = components["schemas"]["CourseDetailResponseDto"];

export async function createChallenge(
  dto: CreateChallengeRequestDto
): Promise<ActionResult<CreateChallengeResponseDto>> {
  return await withActionResult(
    (client) =>
      client.POST("/api/v1/challenges", {
        body: dto,
      }),
    "Failed to create challenge"
  );
}

export async function fetchChallenge(
  id: string
): Promise<ActionResult<ChallengeDetailResponseDto>> {
  return await withActionResult(
    (client) =>
      client.GET("/api/v1/challenges/{id}", {
        params: { path: { id } },
      }),
    "Failed to load challenge"
  );
}

export async function updateChallenge(
  id: string,
  dto: UpdateChallengeRequestDto
): Promise<ActionResult<ChallengeDetailResponseDto>> {
  return await withActionResult(
    (client) =>
      client.PUT("/api/v1/challenges/{id}", {
        params: { path: { id } },
        body: dto,
      }),
    "Failed to update challenge"
  );
}

export async function deleteChallenge(id: string): Promise<ActionResult<void>> {
  return await withActionResultNoContent(
    (client) =>
      client.DELETE("/api/v1/challenges/{id}", {
        params: { path: { id } },
      }),
    "Failed to delete challenge"
  );
}

export async function fetchInstructorChallenges(
  page = 0,
  size = 20
): Promise<ActionResult<PageListChallengeResponseDto>> {
  return await withActionResult(
    (client) =>
      client.GET("/api/v1/challenges", {
        params: { query: { pageable: { page, size } } },
        querySerializer: springPageableSerializer,
      }),
    "Failed to load challenges"
  );
}

export async function searchChallenges(
  query: string,
  page = 0,
  size = 20
): Promise<ActionResult<PageListChallengeResponseDto>> {
  return await withActionResult(
    (client) =>
      client.GET("/api/v1/challenges/search", {
        params: { query: { q: query, pageable: { page, size } } },
        querySerializer: springPageableSerializer,
      }),
    "Failed to search challenges"
  );
}

export async function previewVisibilityImpact(
  id: string,
  newStatus: ChallengeStatusEnum
): Promise<ActionResult<VisibilityImpactResponseDto>> {
  return await withActionResult(
    (client) =>
      client.GET("/api/v1/challenges/{id}/visibility-impact", {
        params: { path: { id }, query: { status: newStatus } },
      }),
    "Failed to preview visibility impact"
  );
}

export async function fetchChallengeForPlay(
  challengeId: string,
  courseId: string
): Promise<ActionResult<ChallengeStudentDto>> {
  return await withActionResult(
    (client) =>
      client.GET("/api/v1/challenges/{id}/play", {
        params: { path: { id: challengeId }, query: { courseId } },
      }),
    "Failed to load challenge"
  );
}

export async function submitSubTaskFlag(
  challengeId: string,
  subTaskId: string,
  flag: string
): Promise<ActionResult<SubTaskSubmissionResponseDto>> {
  return await withActionResult(
    (client) =>
      client.POST("/api/v1/challenges/{challengeId}/subtasks/{subTaskId}/submit", {
        params: { path: { challengeId, subTaskId } },
        body: { flag },
      }),
    "Failed to submit flag"
  );
}

export async function updateCourseChallenges(
  courseId: string,
  challenges: CourseChallengeItemDto[]
): Promise<ActionResult<CourseDetailResponseDto>> {
  return await withActionResult(
    (client) =>
      client.PUT("/api/v1/courses/{id}/challenges", {
        params: { path: { id: courseId } },

        body: { challenges },
      }),
    "Failed to update course challenges"
  );
}
