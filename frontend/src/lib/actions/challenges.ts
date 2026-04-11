"use server";

import { getApiClient } from "@/src/lib/api/server";
import type { components } from "@/src/lib/api/schema";
import type { ActionResult } from "@/src/types/course";

/**
 * Spring Boot resolves Pageable from flat query params (page, size, sort),
 * but openapi-typescript models it as a nested object. This serializer
 * flattens the pageable params so Spring can read them.
 */
function springPageableSerializer(params: Record<string, unknown>): string {
  const parts: string[] = [];
  for (const [key, val] of Object.entries(params)) {
    if (key === "pageable" && typeof val === "object" && val !== null) {
      for (const [pk, pv] of Object.entries(val as Record<string, unknown>)) {
        if (pv != null) parts.push(`${pk}=${encodeURIComponent(String(pv as string | number))}`);
      }
    } else if (val != null) {
      parts.push(`${key}=${encodeURIComponent(String(val as string | number))}`);
    }
  }
  return parts.join("&");
}

// Re-export generated types with convenient aliases
export type ChallengeStatusEnum = NonNullable<
  components["schemas"]["ChallengeDetailResponseDto"]["status"]
>;
export type ChallengeDifficultyEnum = NonNullable<
  components["schemas"]["ChallengeDetailResponseDto"]["difficulty"]
>;

export type CreateChallengeRequestDto = components["schemas"]["CreateChallengeRequestDto"];
export type UpdateChallengeRequestDto = components["schemas"]["UpdateChallengeRequestDto"];
export type CreateChallengeResponseDto = components["schemas"]["CreateChallengeResponseDto"];
export type ChallengeDetailResponseDto = components["schemas"]["ChallengeDetailResponseDto"];
export type ListChallengeResponseDto = components["schemas"]["ListChallengeResponseDto"];
export type PageListChallengeResponseDto = components["schemas"]["PageListChallengeResponseDto"];
export type CourseChallengeItemDto = components["schemas"]["CourseChallengeItemDto"];
export type UpdateCourseChallengesRequestDto =
  components["schemas"]["UpdateCourseChallengesRequestDto"];

export async function createChallenge(
  dto: CreateChallengeRequestDto
): Promise<ActionResult<CreateChallengeResponseDto>> {
  try {
    const client = await getApiClient();
    const { data, error } = await client.POST("/api/v1/challenges", {
      body: dto,
    });

    if (error) {
      return { success: false, error: error.error ?? "Failed to create challenge" };
    }

    return { success: true, data };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}

export async function fetchChallenge(
  id: string
): Promise<ActionResult<ChallengeDetailResponseDto>> {
  try {
    const client = await getApiClient();
    const { data, error } = await client.GET("/api/v1/challenges/{id}", {
      params: { path: { id } },
    });

    if (error) {
      return { success: false, error: error.error ?? "Failed to load challenge" };
    }

    return { success: true, data };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}

export async function updateChallenge(
  id: string,
  dto: UpdateChallengeRequestDto
): Promise<ActionResult<ChallengeDetailResponseDto>> {
  try {
    const client = await getApiClient();
    const { data, error } = await client.PUT("/api/v1/challenges/{id}", {
      params: { path: { id } },
      body: dto,
    });

    if (error) {
      return { success: false, error: error.error ?? "Failed to update challenge" };
    }

    return { success: true, data };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}

export async function deleteChallenge(id: string): Promise<ActionResult<void>> {
  try {
    const client = await getApiClient();
    const { error } = await client.DELETE("/api/v1/challenges/{id}", {
      params: { path: { id } },
    });

    if (error) {
      return { success: false, error: error.error ?? "Failed to delete challenge" };
    }

    return { success: true, data: undefined };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}

export async function fetchInstructorChallenges(
  page = 0,
  size = 20
): Promise<ActionResult<PageListChallengeResponseDto>> {
  try {
    const client = await getApiClient();
    const { data, error } = await client.GET("/api/v1/challenges", {
      params: { query: { pageable: { page, size } } },
      querySerializer: springPageableSerializer,
    });

    if (error) {
      return { success: false, error: "Failed to load challenges" };
    }

    return { success: true, data };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}

export async function searchChallenges(
  query: string,
  page = 0,
  size = 20
): Promise<ActionResult<PageListChallengeResponseDto>> {
  try {
    const client = await getApiClient();
    const { data, error } = await client.GET("/api/v1/challenges/search", {
      params: { query: { q: query, pageable: { page, size } } },
      querySerializer: springPageableSerializer,
    });

    if (error) {
      return { success: false, error: "Failed to search challenges" };
    }

    return { success: true, data };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}

export async function updateCourseChallenges(
  courseId: string,
  challenges: CourseChallengeItemDto[]
): Promise<ActionResult<unknown>> {
  try {
    const client = await getApiClient();
    const { data, error } = await client.PUT("/api/v1/courses/{id}/challenges", {
      params: { path: { id: courseId } },
      body: { challenges },
    });

    if (error) {
      return { success: false, error: error.error ?? "Failed to update course challenges" };
    }

    return { success: true, data };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}
