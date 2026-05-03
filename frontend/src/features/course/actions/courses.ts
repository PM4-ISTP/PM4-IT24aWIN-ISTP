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
export type ListChallengeStatisticOfCourseDto = {
  statistics: {
    challenge: components["schemas"]["ChallengeDetailResponseDto"];
    solvedRatio: number;
  }[];
};

type ChallengeProgressDto = {
  user: components["schemas"]["UserDto"];
  isCompleted: boolean;
  subTasks: {
    subTask: components["schemas"]["SubTaskResponseDto"];
    isCompleted: boolean;
  }[];
};

export type PageListChallengeProgressForCourseDto = {
  totalPages?: number;
  totalElements?: number;
  content?: ChallengeProgressDto[];
};

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
    mcAttemptsMode: dto.mcAttemptsMode ?? "UNLIMITED",
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
    mcAttemptsMode: dto.mcAttemptsMode ?? "UNLIMITED",
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

export async function fetchChallengeStatisticsOfCourse(
  courseId: string
): Promise<ActionResult<ListChallengeStatisticOfCourseDto>> {
  const statistics: ListChallengeStatisticOfCourseDto["statistics"] = [
    {
      challenge: {
        id: "00000000-0000-000-0000-000000000000",
        title: "Introduction Challenge",
        shortDescription: "A gentle warm-up challenge for new participants.",
        description: "Solve the basics and get familiar with the course workflow.",
        status: "PUBLIC",
        difficulty: "BEGINNER",
        maxScore: 100,
        creator: {
          id: "00000000-0000-000-0000-000000000000",
          name: "Creator 1",
        },
        courseCount: 1,
        createdAt: "2026-04-01T08:00:00Z",
        updatedAt: "2026-04-02T08:00:00Z",
      },
      solvedRatio: 0.92,
    },
    {
      challenge: {
        id: "00000000-0000-000-0000-000000000001",
        title: "Input Validation Drill",
        shortDescription: "Validate and sanitize incoming user data.",
        description: "Focus on robust checks and defensive programming techniques.",
        status: "PUBLIC",
        difficulty: "EASY",
        maxScore: 150,
        creator: {
          id: "00000000-0000-000-0000-000000000001",
          name: "Creator 2",
        },
        courseCount: 2,
        createdAt: "2026-04-03T08:00:00Z",
        updatedAt: "2026-04-04T08:00:00Z",
      },
      solvedRatio: 0.76,
    },
    {
      challenge: {
        id: "00000000-0000-000-0000-000000000002",
        title: "Data Model Puzzle",
        shortDescription: "Work with a slightly more complex domain model.",
        description: "Apply relationships and map the domain into a correct solution.",
        status: "PRIVATE",
        difficulty: "MEDIUM",
        maxScore: 200,
        creator: {
          id: "00000000-0000-000-0000-000000000002",
          name: "Creator 3",
        },
        courseCount: 3,
        createdAt: "2026-04-05T08:00:00Z",
        updatedAt: "2026-04-06T08:00:00Z",
      },
      solvedRatio: 0.49,
    },
    {
      challenge: {
        id: "00000000-0000-000-0000-000000000003",
        title: "Integration Bug Hunt",
        shortDescription: "Find and fix issues across multiple components.",
        description: "Trace the full flow and solve bugs at integration boundaries.",
        status: "PUBLIC",
        difficulty: "HARD",
        maxScore: 250,
        creator: {
          id: "00000000-0000-000-0000-000000000003",
          name: "Creator 4",
        },
        courseCount: 4,
        createdAt: "2026-04-07T08:00:00Z",
        updatedAt: "2026-04-08T08:00:00Z",
      },
      solvedRatio: 0.31,
    },
    {
      challenge: {
        id: "00000000-0000-000-0000-000000000004",
        title: "Capstone Challenge",
        shortDescription: "The final challenge combining everything learned.",
        description: "A comprehensive exercise that ties together the complete course content.",
        status: "PUBLIC",
        difficulty: "EXPERT",
        maxScore: 300,
        creator: {
          id: "00000000-0000-000-0000-000000000003",
          name: "Creator 5",
        },
        courseCount: 5,
        createdAt: "2026-04-09T08:00:00Z",
        updatedAt: "2026-04-10T08:00:00Z",
      },
      solvedRatio: 0.14,
    },
  ];

  await Promise.resolve(); // fake async function needed for ESLint
  console.log(courseId); // console.log needed to suppress ESLint error

  return {
    success: true,
    data: {
      statistics,
    },
  };
}

export async function fetchChallengeProgressesForCourse(
  challengeId: string,
  courseId: string,
  query = "",
  page = 0,
  size = 20
): Promise<ActionResult<PageListChallengeProgressForCourseDto>> {
  const content: ChallengeProgressDto[] = Array.from({ length: 20 }, (_, index) => {
    const userNumber = page * size + index + 1;
    const baseCompletion = index % 4 !== 3;

    return {
      user: {
        id: `${courseId}-user-${userNumber}`,
        name: `Student ${userNumber}`,
        email: `student${userNumber}@example.com`,
        username: `student${userNumber}`,
        title: index % 2 === 0 ? "BSc" : "MSc",
      },
      isCompleted: baseCompletion,
      subTasks: [
        {
          subTask: {
            id: `${challengeId}-subtask-1`,
            title: "Reconnaissance",
            description: "Collect the required information for the challenge.",
            flag: "ISTP{reconnaissance}",
            orderIndex: 1,
          },
          isCompleted: true,
        },
        {
          subTask: {
            id: `${challengeId}-subtask-2`,
            title: "Exploit",
            description: "Use the discovered weakness to solve the task.",
            flag: "ISTP{exploit}",
            orderIndex: 2,
          },
          isCompleted: index % 3 !== 0,
        },
        {
          subTask: {
            id: `${challengeId}-subtask-3`,
            title: "Validation",
            description: "Verify the solution and document the result.",
            flag: "ISTP{validation}",
            orderIndex: 3,
          },
          isCompleted: index % 5 === 0,
        },
      ],
    };
  });

  await Promise.resolve(); // fake async function needed for ESLint
  console.log(query); // console.log needed to suppress ESLint error

  return {
    success: true,
    data: {
      totalPages: 5,
      totalElements: 100,
      content,
    },
  };
}
