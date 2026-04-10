"use server";

import { fetchBackend } from "@/src/lib/api";

import type {
  ActionResult,
  CourseDetailResponseDto,
  CourseResponseDto,
  CreateCourseDto,
  ListCourseResponseDto,
  Page,
  UpdateCourseDto,
} from "@/src/types/course";

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

export async function createCourse(
  dto: Omit<CreateCourseDto, "instructors"> & { collaboratorIds: string[] }
): Promise<ActionResult<CourseResponseDto>> {
  try {
    const payload: CreateCourseDto = {
      title: dto.title,
      description: dto.description,
      shortDescription: dto.shortDescription,
      isPublished: dto.isPublished,
      imageUrl: dto.imageUrl,
      topic: dto.topic,
      difficulty: dto.difficulty,
      instructors: dto.collaboratorIds.map((id) => ({
        instructorId: id,
        instructorRole: "COLLABORATOR" as const,
      })),
    };

    const res = await fetchBackend("/api/v1/courses", {
      method: "POST",
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      const text = await res.text();
      const message = extractErrorMessage(text, res.statusText);
      return { success: false, error: `${res.status}: ${message}` };
    }

    const data = (await res.json()) as CourseResponseDto;
    return { success: true, data };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}

export async function fetchPublicCourse(
  id: string
): Promise<ActionResult<CourseDetailResponseDto>> {
  try {
    const res = await fetchBackend(`/api/v1/courses/catalog/${id}`, {
      cache: "no-store",
    });

    if (!res.ok) {
      const text = await res.text();
      const message = extractErrorMessage(text, res.statusText);
      return { success: false, error: `${res.status}: ${message}` };
    }

    const data = (await res.json()) as CourseDetailResponseDto;
    return { success: true, data };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}

export async function enrollInCourse(
  id: string
): Promise<ActionResult<CourseDetailResponseDto>> {
  try {
    const res = await fetchBackend(`/api/v1/courses/catalog/${id}/enroll`, {
      method: "POST",
    });

    if (!res.ok) {
      const text = await res.text();
      const message = extractErrorMessage(text, res.statusText);
      return { success: false, error: `${res.status}: ${message}` };
    }

    const data = (await res.json()) as CourseDetailResponseDto;
    return { success: true, data };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}

export async function fetchCourse(id: string): Promise<ActionResult<CourseDetailResponseDto>> {
  try {
    const res = await fetchBackend(`/api/v1/courses/${id}`, {
      cache: "no-store",
    });

    if (!res.ok) {
      const text = await res.text();
      const message = extractErrorMessage(text, res.statusText);
      return { success: false, error: `${res.status}: ${message}` };
    }

    const data = (await res.json()) as CourseDetailResponseDto;
    return { success: true, data };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}

export async function updateCourse(
  id: string,
  dto: Omit<UpdateCourseDto, "instructors"> & { collaboratorIds: string[] }
): Promise<ActionResult<CourseDetailResponseDto>> {
  try {
    const payload: UpdateCourseDto = {
      title: dto.title,
      description: dto.description,
      shortDescription: dto.shortDescription,
      isPublished: dto.isPublished,
      imageUrl: dto.imageUrl,
      topic: dto.topic,
      difficulty: dto.difficulty,
      instructors: dto.collaboratorIds.map((cid) => ({
        instructorId: cid,
        instructorRole: "COLLABORATOR" as const,
      })),
    };

    const res = await fetchBackend(`/api/v1/courses/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      const text = await res.text();
      const message = extractErrorMessage(text, res.statusText);
      return { success: false, error: `${res.status}: ${message}` };
    }

    const data = (await res.json()) as CourseDetailResponseDto;
    return { success: true, data };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}

export async function deleteCourse(id: string): Promise<ActionResult<void>> {
  try {
    const res = await fetchBackend(`/api/v1/courses/${id}`, {
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

export async function fetchInstructorCourses(
  page = 0,
  size = 20
): Promise<ActionResult<Page<ListCourseResponseDto>>> {
  try {
    const res = await fetchBackend(`/api/v1/courses?page=${page}&size=${size}`, {
      cache: "no-store",
    });

    if (!res.ok) {
      return { success: false, error: `${res.status}: ${res.statusText}` };
    }

    const data = (await res.json()) as Page<ListCourseResponseDto>;
    return { success: true, data };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}

export async function fetchPublishedCourses(
  query = "",
  page = 0,
  size = 12
): Promise<ActionResult<Page<ListCourseResponseDto>>> {
  try {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      ...(query.trim() ? { query: query.trim() } : {}),
    });

    const res = await fetchBackend(`/api/v1/courses/catalog?${params}`, {
      cache: "no-store",
    });

    if (!res.ok) {
      const text = await res.text();
      const message = extractErrorMessage(text, res.statusText);
      return { success: false, error: `${res.status}: ${message}` };
    }

    const data = (await res.json()) as Page<ListCourseResponseDto>;
    return { success: true, data };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}
