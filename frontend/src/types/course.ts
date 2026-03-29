export type InstructorRoleEnum = "OWNER" | "COLLABORATOR";

export interface InstructorAssignment {
    instructorId: string;
    instructorRole: InstructorRoleEnum;
}

export interface CreateCourseDto {
    title: string;
    description: string;
    isPublished: boolean;
    instructors: InstructorAssignment[];
}

export interface CourseResponseDto {
    id: string;
    title: string;
    description: string;
    isPublished: boolean;
    instructors: InstructorAssignment[];
    createdAt: string;
    updatedAt: string;
}

export interface ListCourseResponseDto {
    id: string;
    title: string;
    description: string;
    published: boolean;
    instructorCount: number;
    updatedAt: string;
}

export interface Page<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
}

export type ActionResult<T> =
    | { success: true; data: T }
    | { success: false; error: string };
