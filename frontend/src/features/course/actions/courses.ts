"use server";

import { fetchBackend } from "@/src/shared/lib/api";
import { springPageableSerializer } from "@/src/shared/lib/api/querySerializers";
import type { components } from "@/src/shared/lib/api/schema";
import { getApiClient } from "@/src/shared/lib/api/server";
import { extractErrorMessage } from "@/src/shared/lib/utils";

import type {
  ActionResult,
  CourseDetailResponseDto,
  CourseResponseDto,
  CreateCourseDto,
  UpdateCourseDto,
} from "@/src/shared/types/course";

export type PublicCourseDetailResponseDto = components["schemas"]["PublicCourseDetailResponseDto"];
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
): Promise<ActionResult<PublicCourseDetailResponseDto>> {
  try {
    const client = await getApiClient();
    const { data, error } = await client.GET("/api/v1/courses/catalog/{id}", {
      params: { path: { id } },
    });

    if (error) {
      return { success: false, error: error.error ?? "Failed to load course" };
    }

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
): Promise<ActionResult<PublicCourseDetailResponseDto>> {
  try {
    const client = await getApiClient();
    const { data, error } = await client.POST("/api/v1/courses/catalog/{id}/enroll", {
      params: { path: { id } },
    });

    if (error) {
      return { success: false, error: error.error ?? "Failed to load enroll in course" };
    }

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
): Promise<ActionResult<PageListCourseResponseDto>> {
  try {
    const client = await getApiClient();
    const { data, error } = await client.GET("/api/v1/courses", {
      params: { query: { pageable: { page: page, size: size } } },
      querySerializer: springPageableSerializer,
    });

    if (error) {
      return { success: false, error: error.error ?? "Failed to load enrollments" };
    }

    return { success: true, data };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}

export async function fetchEnrolledCoursesOfLoggedInUser(
  page = 0,
  size = 20
): Promise<ActionResult<PageListCourseResponseDto>> {
  try {
    const client = await getApiClient();
    const { data, error } = await client.GET("/api/v1/courses/my-enrollments", {
      params: { query: { pageable: { page: page, size: size } } },
      querySerializer: springPageableSerializer,
    });

    if (error) {
      return { success: false, error: error.error ?? "Failed to load enrollments" };
    }

    return { success: true, data };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}

export async function joinCourseByCode(
  code: string
): Promise<ActionResult<PublicCourseDetailResponseDto>> {
  try {
    const client = await getApiClient();
    const { data, error } = await client.POST("/api/v1/courses/catalog/join", {
      body: { code },
    });

    if (error) {
      return { success: false, error: error.error ?? "Failed to join course" };
    }

    return { success: true, data };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}

export async function regenerateInviteCode(
  id: string
): Promise<ActionResult<CourseDetailResponseDto>> {
  try {
    const res = await fetchBackend(`/api/v1/courses/${id}/invite-code/regenerate`, {
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

export async function fetchPublishedCourses(
  query = "",
  page = 0,
  size = 12
): Promise<ActionResult<PageListCourseResponseDto>> {
  try {
    const client = await getApiClient();
    const { data, error } = await client.GET("/api/v1/courses/catalog", {
      params: { query: { query: query, pageable: { page: page, size: size } } },
      querySerializer: springPageableSerializer,
    });

    if (error) {
      return { success: false, error: error.error ?? "Failed to load published courses" };
    }

    return { success: true, data };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}
