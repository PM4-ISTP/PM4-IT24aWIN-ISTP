"use server";

import type { components } from "@/src/shared/lib/api/schema";
import { withActionResult, withActionResultNoContent } from "@/src/shared/lib/api/actionResult";
import type { ActionResult } from "@/src/shared/lib/api/actionResult";
import { springPageableSerializer } from "@/src/shared/lib/api/querySerializers";

// Re-export generated types with convenient aliases
export type LabStatusEnum = NonNullable<
  components["schemas"]["ChallengeDetailResponseDto"]["status"]
>;
export type LabDifficultyEnum = NonNullable<
  components["schemas"]["ChallengeDetailResponseDto"]["difficulty"]
>;

export type ChallengeCreatorResponseDto = components["schemas"]["ChallengeCreatorResponseDto"];
export type CreateChallengeRequestDto = components["schemas"]["CreateChallengeRequestDto"];
export type UpdateChallengeRequestDto = components["schemas"]["UpdateChallengeRequestDto"];
export type CreateChallengeResponseDto = components["schemas"]["CreateChallengeResponseDto"];
export type ChallengeDetailResponseDto = components["schemas"]["ChallengeDetailResponseDto"];
export type LabStudentDto = components["schemas"]["LabStudentDto"];
export type ChallengeStudentDto = components["schemas"]["ChallengeStudentDto"];
export type ChallengeOptionStudentDto = components["schemas"]["ChallengeOptionStudentDto"];
export type ChallengeSubmissionRequestDto = components["schemas"]["ChallengeSubmissionRequestDto"];
export type ChallengeSubmissionResponseDto =
  components["schemas"]["ChallengeSubmissionResponseDto"];
export type ChoiceSubmissionResponseDto = components["schemas"]["ChoiceSubmissionResponseDto"];
export type ListLabResponseDto = components["schemas"]["ListLabResponseDto"];
export type PageListLabResponseDto = components["schemas"]["PageListLabResponseDto"];
export type CourseLabItemDto = components["schemas"]["CourseLabItemDto"];
export type UpdateCourseLabsRequestDto = components["schemas"]["UpdateCourseLabsRequestDto"];
export type VisibilityImpactResponseDto = components["schemas"]["VisibilityImpactResponseDto"];
export type CourseDetailResponseDto = components["schemas"]["CourseDetailResponseDto"];

export async function createChallenge(
  dto: CreateChallengeRequestDto
): Promise<ActionResult<CreateChallengeResponseDto>> {
  return await withActionResult(
    (client) =>
      client.POST("/api/v1/labs", {
        body: dto,
      }),
    "Failed to create lab"
  );
}

export async function fetchChallenge(
  id: string
): Promise<ActionResult<ChallengeDetailResponseDto>> {
  return await withActionResult(
    (client) =>
      client.GET("/api/v1/labs/{id}", {
        params: { path: { id } },
      }),
    "Failed to load lab"
  );
}

export async function updateChallenge(
  id: string,
  dto: UpdateChallengeRequestDto
): Promise<ActionResult<ChallengeDetailResponseDto>> {
  return await withActionResult(
    (client) =>
      client.PUT("/api/v1/labs/{id}", {
        params: { path: { id } },
        body: dto,
      }),
    "Failed to update lab"
  );
}

export async function deleteChallenge(id: string): Promise<ActionResult<void>> {
  return await withActionResultNoContent(
    (client) =>
      client.DELETE("/api/v1/labs/{id}", {
        params: { path: { id } },
      }),
    "Failed to delete lab"
  );
}

export async function fetchInstructorChallenges(
  page = 0,
  size = 20
): Promise<ActionResult<PageListLabResponseDto>> {
  return await withActionResult(
    (client) =>
      client.GET("/api/v1/labs", {
        params: { query: { pageable: { page, size } } },
        querySerializer: springPageableSerializer,
      }),
    "Failed to load labs"
  );
}

export async function searchChallenges(
  query: string,
  page = 0,
  size = 20
): Promise<ActionResult<PageListLabResponseDto>> {
  return await withActionResult(
    (client) =>
      client.GET("/api/v1/labs/search", {
        params: { query: { q: query, pageable: { page, size } } },
        querySerializer: springPageableSerializer,
      }),
    "Failed to search labs"
  );
}

export async function previewVisibilityImpact(
  id: string,
  newStatus: LabStatusEnum
): Promise<ActionResult<VisibilityImpactResponseDto>> {
  return await withActionResult(
    (client) =>
      client.GET("/api/v1/labs/{id}/visibility-impact", {
        params: { path: { id }, query: { status: newStatus } },
      }),
    "Failed to preview visibility impact"
  );
}

export async function fetchChallengeForPlay(
  labId: string,
  courseId: string
): Promise<ActionResult<LabStudentDto>> {
  return await withActionResult(
    (client) =>
      client.GET("/api/v1/labs/{id}/play", {
        params: { path: { id: labId }, query: { courseId } },
      }),
    "Failed to load lab"
  );
}

export async function submitChallengeFlag(
  labId: string,
  challengeId: string,
  flag: string,
  courseId: string
): Promise<ActionResult<ChallengeSubmissionResponseDto>> {
  return await withActionResult(
    (client) =>
      client.POST("/api/v1/labs/{labId}/challenges/{challengeId}/submit", {
        params: { path: { labId, challengeId } },
        body: { flag, courseId },
      }),
    "Failed to submit flag"
  );
}

export async function submitChallengeChoice(
  labId: string,
  challengeId: string,
  selectedOptionId: string,
  courseId: string
): Promise<ActionResult<ChoiceSubmissionResponseDto>> {
  return await withActionResult(
    (client) =>
      client.POST("/api/v1/labs/{labId}/challenges/{challengeId}/submit-choice", {
        params: { path: { labId, challengeId } },
        body: { selectedOptionId, courseId },
      }),
    "Failed to submit answer"
  );
}

export async function completeTheoryChallenge(
  labId: string,
  challengeId: string,
  courseId: string
): Promise<ActionResult<ChallengeSubmissionResponseDto>> {
  return await withActionResult(
    (client) =>
      client.POST("/api/v1/labs/{labId}/challenges/{challengeId}/complete", {
        params: { path: { labId, challengeId }, query: { courseId } },
      }),
    "Failed to complete task"
  );
}

export async function updateCourseChallenges(
  courseId: string,
  labs: CourseLabItemDto[]
): Promise<ActionResult<CourseDetailResponseDto>> {
  return await withActionResult(
    (client) =>
      client.PUT("/api/v1/courses/{id}/labs", {
        params: { path: { id: courseId } },
        body: { labs },
      }),
    "Failed to update course labs"
  );
}
