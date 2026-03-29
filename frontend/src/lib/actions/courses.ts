"use server";

import {fetchBackend} from "@/src/lib/api";

import type {
    ActionResult,
    CourseDetailResponseDto,
    CourseResponseDto,
    CreateCourseDto,
    ListCourseResponseDto,
    Page,
    UpdateCourseDto,
} from "@/src/types/course";

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
            let message = res.statusText;
            try {
                const json = JSON.parse(text);
                if (json.error) message = json.error;
            } catch {
                message = text || res.statusText;
            }
            return {success: false, error: `${res.status}: ${message}`};
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

export async function fetchCourse(
    id: string
): Promise<ActionResult<CourseDetailResponseDto>> {
    try {
        const res = await fetchBackend(`/api/v1/courses/${id}`, {
            cache: "no-store",
        });

        if (!res.ok) {
            const text = await res.text();
            let message = res.statusText;
            try {
                const json = JSON.parse(text);
                if (json.error) message = json.error;
            } catch {
                message = text || res.statusText;
            }
            return {success: false, error: `${res.status}: ${message}`};
        }

        const data = (await res.json()) as CourseDetailResponseDto;
        return {success: true, data};
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
            isPublished: dto.isPublished,
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
            let message = res.statusText;
            try {
                const json = JSON.parse(text);
                if (json.error) message = json.error;
            } catch {
                message = text || res.statusText;
            }
            return {success: false, error: `${res.status}: ${message}`};
        }

        const data = (await res.json()) as CourseDetailResponseDto;
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
