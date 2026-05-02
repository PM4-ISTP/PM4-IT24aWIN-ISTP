"use server";

import { withActionResult, withActionResultNoContent } from "@/src/shared/lib/api/actionResult";
import type { ActionResult } from "@/src/shared/lib/api/actionResult";
import { springPageableSerializer } from "@/src/shared/lib/api/querySerializers";
import type { components } from "@/src/shared/lib/api/schema";

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

type CreateCourseRequestDto = components["schemas"]["CreateCourseRequestDto"];
type UpdateCourseRequestDto = components["schemas"]["UpdateCourseRequestDto"];

// The OpenAPI generator marks every response field optional, but the backend
// guarantees them. Consumers (e.g. dashboard pages) rely on the stricter
// handwritten shapes, so we cast at the action boundary.
function castResult<T>(result: ActionResult<unknown>): ActionResult<T> {
  return result as ActionResult<T>;
}

export async function createCourse(
  dto: Omit<CreateCourseDto, "instructors"> & { collaboratorIds: string[] }
): Promise<ActionResult<CourseResponseDto>> {
  const body: CreateCourseRequestDto = {
    title: dto.title,
    description: dto.description,
    shortDescription: dto.shortDescription ?? "",
    isPublished: dto.isPublished,
    isPrivate: dto.isPrivate,
    imageUrl: dto.imageUrl ?? undefined,
    topic: dto.topic ?? undefined,
    instructors: dto.collaboratorIds.map((id) => ({
      instructorId: id,
      instructorRole: "COLLABORATOR",
    })),
  };

  const result = await withActionResult(
    (client) => client.POST("/api/v1/courses", { body }),
    "Failed to create course"
  );
  return castResult<CourseResponseDto>(result);
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
  const result = await withActionResult(
    (client) =>
      client.GET("/api/v1/courses/{id}", {
        params: { path: { id } },
      }),
    "Failed to load course"
  );
  return castResult<OldCourseDetailResponseDto>(result);
}

export async function updateCourse(
  id: string,
  dto: Omit<UpdateCourseDto, "instructors"> & { collaboratorIds: string[] }
): Promise<ActionResult<OldCourseDetailResponseDto>> {
  const body: UpdateCourseRequestDto = {
    title: dto.title,
    description: dto.description,
    shortDescription: dto.shortDescription ?? "",
    isPublished: dto.isPublished,
    isPrivate: dto.isPrivate,
    imageUrl: dto.imageUrl ?? undefined,
    topic: dto.topic ?? undefined,
    instructors: dto.collaboratorIds.map((cid) => ({
      instructorId: cid,
      instructorRole: "COLLABORATOR",
    })),
  };

  const result = await withActionResult(
    (client) =>
      client.PUT("/api/v1/courses/{id}", {
        params: { path: { id } },
        body,
      }),
    "Failed to update course"
  );
  return castResult<OldCourseDetailResponseDto>(result);
}

export async function deleteCourse(id: string): Promise<ActionResult<void>> {
  return await withActionResultNoContent(
    (client) =>
      client.DELETE("/api/v1/courses/{id}", {
        params: { path: { id } },
      }),
    "Failed to delete course"
  );
}

export async function removeCourseParticipant(
  courseId: string,
  participantId: string
): Promise<ActionResult<void>> {
  return await withActionResultNoContent(
    (client) =>
      client.DELETE("/api/v1/courses/{id}/participants/{participantId}", {
        params: { path: { id: courseId, participantId } },
      }),
    "Failed to remove participant"
  );
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

export async function leaveCourse(id: string): Promise<ActionResult<void>> {
  return await withActionResultNoContent(
    (client) =>
      client.DELETE("/api/v1/courses/catalog/{id}/leave", {
        params: { path: { id } },
      }),
    "Failed to leave course"
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
  return await withActionResult(
    (client) =>
      client.GET("/api/v1/courses/catalog", {
        params: {
          query: {
            pageable: { page, size },
            ...(query.trim() ? { query: query.trim() } : {}),
            ...(topic.trim() ? { topic: topic.trim() } : {}),
          },
        },
        querySerializer: springPageableSerializer,
      }),
    "Failed to load published courses"
  );
}

export async function fetchCourseTopics(): Promise<ActionResult<string[]>> {
  return await withActionResult(
    (client) => client.GET("/api/v1/courses/topics"),
    "Failed to load course topics"
  );
}
