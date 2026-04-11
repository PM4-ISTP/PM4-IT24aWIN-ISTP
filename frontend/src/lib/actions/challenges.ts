"use server";

import { fetchBackend } from "@/src/lib/api";

import type { ActionResult, Page } from "@/src/types/course";

// Temporary manual types — replace with generated types after `npm run generate:api`

export type ChallengeStatusEnum = "DRAFT" | "PRIVATE" | "PUBLIC";
export type ChallengeDifficultyEnum = "BEGINNER" | "EASY" | "MEDIUM" | "HARD" | "EXPERT";

export interface CreateChallengeDto {
  title: string;
  shortDescription: string | null;
  description: string | null;
  status: ChallengeStatusEnum;
  difficulty: ChallengeDifficultyEnum;
}

export interface UpdateChallengeDto {
  title: string;
  shortDescription: string | null;
  description: string | null;
  status: ChallengeStatusEnum;
  difficulty: ChallengeDifficultyEnum;
}

export interface ChallengeResponseDto {
  id: string;
  title: string;
  shortDescription: string | null;
  description: string | null;
  status: ChallengeStatusEnum;
  difficulty: ChallengeDifficultyEnum;
  maxScore: number;
  creatorId: string;
  createdAt: string;
  updatedAt: string;
}

export interface ChallengeCreatorDto {
  id: string;
  name: string;
}

export interface ChallengeDetailResponseDto {
  id: string;
  title: string;
  shortDescription: string | null;
  description: string | null;
  status: ChallengeStatusEnum;
  difficulty: ChallengeDifficultyEnum;
  maxScore: number;
  creator: ChallengeCreatorDto;
  createdAt: string;
  updatedAt: string;
}

export interface ListChallengeResponseDto {
  id: string;
  title: string;
  shortDescription: string | null;
  status: ChallengeStatusEnum;
  difficulty: ChallengeDifficultyEnum;
  maxScore: number;
  creatorName: string;
  courseCount: number;
  updatedAt: string;
}

export interface CourseChallengeItem {
  challengeId: string;
  orderIndex: number;
}

function extractErrorMessage(text: string, fallback: string): string {
  if (!text) {
    return fallback;
  }

  try {
    const parsed: unknown = JSON.parse(text);
    if (typeof parsed === "object" && parsed !== null && "error" in parsed) {
      const errorValue = (parsed as { error?: unknown }).error;
      if (typeof errorValue === "string" && errorValue.trim()) {
        return errorValue;
      }
    }
  } catch {
    return text || fallback;
  }

  return text || fallback;
}

export async function createChallenge(
  dto: CreateChallengeDto
): Promise<ActionResult<ChallengeResponseDto>> {
  try {
    const res = await fetchBackend("/api/v1/challenges", {
      method: "POST",
      body: JSON.stringify(dto),
    });

    if (!res.ok) {
      const text = await res.text();
      const message = extractErrorMessage(text, res.statusText);
      return { success: false, error: `${res.status}: ${message}` };
    }

    const data = (await res.json()) as ChallengeResponseDto;
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
    const res = await fetchBackend(`/api/v1/challenges/${id}`, {
      cache: "no-store",
    });

    if (!res.ok) {
      const text = await res.text();
      const message = extractErrorMessage(text, res.statusText);
      return { success: false, error: `${res.status}: ${message}` };
    }

    const data = (await res.json()) as ChallengeDetailResponseDto;
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
  dto: UpdateChallengeDto
): Promise<ActionResult<ChallengeDetailResponseDto>> {
  try {
    const res = await fetchBackend(`/api/v1/challenges/${id}`, {
      method: "PUT",
      body: JSON.stringify(dto),
    });

    if (!res.ok) {
      const text = await res.text();
      const message = extractErrorMessage(text, res.statusText);
      return { success: false, error: `${res.status}: ${message}` };
    }

    const data = (await res.json()) as ChallengeDetailResponseDto;
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
    const res = await fetchBackend(`/api/v1/challenges/${id}`, {
      method: "DELETE",
    });

    if (!res.ok) {
      const text = await res.text();
      const message = extractErrorMessage(text, res.statusText);
      return { success: false, error: `${res.status}: ${message}` };
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
): Promise<ActionResult<Page<ListChallengeResponseDto>>> {
  try {
    const res = await fetchBackend(`/api/v1/challenges?page=${page}&size=${size}`, {
      cache: "no-store",
    });

    if (!res.ok) {
      return { success: false, error: `${res.status}: ${res.statusText}` };
    }

    const data = (await res.json()) as Page<ListChallengeResponseDto>;
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
): Promise<ActionResult<Page<ListChallengeResponseDto>>> {
  try {
    const res = await fetchBackend(
      `/api/v1/challenges/search?q=${encodeURIComponent(query)}&page=${page}&size=${size}`,
      { cache: "no-store" }
    );

    if (!res.ok) {
      const text = await res.text();
      const message = extractErrorMessage(text, res.statusText);
      return { success: false, error: `${res.status}: ${message}` };
    }

    const data = (await res.json()) as Page<ListChallengeResponseDto>;
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
  challenges: CourseChallengeItem[]
): Promise<ActionResult<unknown>> {
  try {
    const res = await fetchBackend(`/api/v1/courses/${courseId}/challenges`, {
      method: "PUT",
      body: JSON.stringify({ challenges }),
    });

    if (!res.ok) {
      const text = await res.text();
      const message = extractErrorMessage(text, res.statusText);
      return { success: false, error: `${res.status}: ${message}` };
    }

    const data = (await res.json()) as unknown;
    return { success: true, data };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}
