"use server";

import {fetchBackend} from "@/src/lib/api";

import type {ActionResult, CourseResponseDto, CreateCourseDto, ListCourseResponseDto, Page,} from "@/src/types/course";

export async function createCourse(
    dto: Omit<CreateCourseDto, "instructors"> & { collaboratorIds: string[] }
): Promise<ActionResult<CourseResponseDto>> {
    try {
        const payload: CreateCourseDto = {
            title: dto.title,
            description: dto.description,
            isPublished: dto.isPublished,
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
            return {success: false, error: `${res.status}: ${text || res.statusText}`};
        }

        const data = (await res.json()) as CourseResponseDto;
        return {success: true, data};
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
            return {success: false, error: `${res.status}: ${res.statusText}`};
        }

        const data = (await res.json()) as Page<ListCourseResponseDto>;
        return {success: true, data};
    } catch (err) {
        return {
            success: false,
            error: err instanceof Error ? err.message : "Unknown error",
        };
    }
}
