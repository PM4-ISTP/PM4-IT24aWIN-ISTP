"use server";

import { fetchBackend } from "@/src/shared/lib/api";
import { withActionResult } from "@/src/shared/lib/api/actionResult";
import type { ActionResult } from "@/src/shared/lib/api/actionResult";
import { springPageableSerializer } from "@/src/shared/lib/api/querySerializers";
import type { components } from "@/src/shared/lib/api/schema";
import { extractErrorMessage } from "@/src/shared/lib/utils";

import type {
  CourseDetailResponseDto as OldCourseDetailResponseDto,
  CourseResponseDto,
  CreateCourseDto,
  UpdateCourseDto,
} from "@/src/shared/types/course";

export type PublicCourseDetailResponseDto = components["schemas"]["PublicCourseDetailResponseDto"];
export type CourseDetailResponseDto = components["schemas"]["CourseDetailResponseDto"];
export type CourseDetailInstructorResponseDto =
  components["schemas"]["CourseDetailInstructorResponseDto"];
export type PageListCourseResponseDto = components["schemas"]["PageListCourseResponseDto"];
export type ListCourseResponseDto = components["schemas"]["ListCourseResponseDto"];

export async function createCourse(
  dto: Omit<CreateCourseDto, "instructors"> & { collaboratorIds: string[] }
): Promise<ActionResult<CourseResponseDto>> {
  try {
    const payload: CreateCourseDto = {
      title: dto.title,
      description: dto.description,
      shortDescription: dto.shortDescription,
      isPublished: dto.isPublished,
      isPrivate: dto.isPrivate,
      imageUrl: dto.imageUrl,
      topic: dto.topic,
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
      return { success: false, error: message };
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
): Promise<ActionResult<PublicCourseDetailResponseDto>> {
  return await withActionResult(
    (client) =>
      client.GET("/api/v1/courses/catalog/{id}", {
        params: { path: { id } },
      }),
    "Failed to load course"
  );
}

export async function enrollInCourse(
  id: string
): Promise<ActionResult<PublicCourseDetailResponseDto>> {
  return await withActionResult(
    (client) =>
      client.POST("/api/v1/courses/catalog/{id}/enroll", {
        params: { path: { id } },
      }),
    "Failed to enroll in course"
  );
}

export async function fetchCourse(id: string): Promise<ActionResult<OldCourseDetailResponseDto>> {
  try {
    const res = await fetchBackend(`/api/v1/courses/${id}`, {
      cache: "no-store",
    });

    if (!res.ok) {
      const text = await res.text();
      const message = extractErrorMessage(text, res.statusText);
      return { success: false, error: message };
    }

    const data = (await res.json()) as OldCourseDetailResponseDto;
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
): Promise<ActionResult<OldCourseDetailResponseDto>> {
  try {
    const payload: UpdateCourseDto = {
      title: dto.title,
      description: dto.description,
      shortDescription: dto.shortDescription,
      isPublished: dto.isPublished,
      isPrivate: dto.isPrivate,
      imageUrl: dto.imageUrl,
      topic: dto.topic,
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
      return { success: false, error: message };
    }

    const data = (await res.json()) as OldCourseDetailResponseDto;
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
      return { success: false, error: message };
    }

    return { success: true, data: undefined };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}

export async function removeCourseParticipant(
  courseId: string,
  participantId: string
): Promise<ActionResult<void>> {
  try {
    const res = await fetchBackend(`/api/v1/courses/${courseId}/participants/${participantId}`, {
      method: "DELETE",
    });

    if (!res.ok) {
      const text = await res.text();
      const message = extractErrorMessage(text, res.statusText);
      return { success: false, error: message };
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
): Promise<ActionResult<PageListCourseResponseDto>> {
  return await withActionResult(
    (client) =>
      client.GET("/api/v1/courses", {
        params: { query: { pageable: { page, size } } },
        querySerializer: springPageableSerializer,
      }),
    "Failed to load the courses for which the current user is owner or collaborator"
  );
}

export async function fetchEnrolledCoursesOfLoggedInUser(
  page = 0,
  size = 20
): Promise<ActionResult<PageListCourseResponseDto>> {
  return await withActionResult(
    (client) =>
      client.GET("/api/v1/courses/my-enrollments", {
        params: { query: { pageable: { page, size } } },
        querySerializer: springPageableSerializer,
      }),
    "Failed to load enrollments"
  );
}

export async function joinCourseByCode(
  code: string
): Promise<ActionResult<PublicCourseDetailResponseDto>> {
  return await withActionResult(
    (client) =>
      client.POST("/api/v1/courses/catalog/join", {
        body: { code },
      }),
    "Failed to join course"
  );
}

export async function regenerateInviteCode(
  id: string
): Promise<ActionResult<CourseDetailResponseDto>> {
  return await withActionResult(
    (client) =>
      client.POST("/api/v1/courses/{id}/invite-code/regenerate", {
        params: { path: { id } },
      }),
    "Failed to regenerate invite code"
  );
}

export async function fetchPublishedCourses(
  query = "",
  page = 0,
  size = 12,
  topic = ""
): Promise<ActionResult<PageListCourseResponseDto>> {
  try {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      ...(query.trim() ? { query: query.trim() } : {}),
      ...(topic.trim() ? { topic: topic.trim() } : {}),
    });

    const res = await fetchBackend(`/api/v1/courses/catalog?${params}`, {
      cache: "no-store",
    });

    if (!res.ok) {
      const text = await res.text();
      const message = extractErrorMessage(text, res.statusText);
      return { success: false, error: message };
    }

    const data = (await res.json()) as PageListCourseResponseDto;
    return { success: true, data };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}

export async function fetchCourseTopics(): Promise<ActionResult<string[]>> {
  try {
    const res = await fetchBackend("/api/v1/courses/topics", { cache: "no-store" });
    if (!res.ok) {
      const text = await res.text();
      const message = extractErrorMessage(text, res.statusText);
      return { success: false, error: message };
    }
    const data = (await res.json()) as string[];
    return { success: true, data: Array.isArray(data) ? data : [] };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}
