export type InstructorRoleEnum = "OWNER" | "COLLABORATOR";
export type PlatformRole = "ROLE_ADMINISTRATOR" | "ROLE_INSTRUCTOR" | "ROLE_STUDENT";
export type CourseDifficulty = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";

export interface CourseUserSummary {
  id: string;
  name: string;
  email: string;
  username?: string | null;
  picture?: string | null;
  roles: PlatformRole[];
}

export type CollaboratorUserResponseDto = CourseUserSummary;

export interface InstructorAssignment {
  instructorId: string;
  instructorRole: InstructorRoleEnum;
}

export interface CreateCourseDto {
  title: string;
  description: string;
  isPublished: boolean;
  imageUrl?: string | null;
  topic?: string | null;
  difficulty?: CourseDifficulty | null;
  instructors: InstructorAssignment[];
}

export interface CourseResponseDto {
  id: string;
  title: string;
  description: string | null;
  isPublished: boolean;
  imageUrl?: string | null;
  topic?: string | null;
  difficulty?: CourseDifficulty | null;
  instructors: InstructorAssignment[];
  createdAt: string;
  updatedAt: string;
}

export interface ListCourseResponseDto {
  id: string;
  title: string;
  description: string | null;
  isPublished: boolean;
  instructorCount: number;
  imageUrl?: string | null;
  topic?: string | null;
  difficulty?: CourseDifficulty | null;
  ownerName?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateCourseDto {
  title: string;
  description: string;
  isPublished: boolean;
  imageUrl?: string | null;
  topic?: string | null;
  difficulty?: CourseDifficulty | null;
  instructors: InstructorAssignment[];
}

export interface CourseInstructorResponseDto {
  id: string;
  instructorRole: InstructorRoleEnum;
  isAccepted: boolean;
  instructor: CourseUserSummary;
  invitedAt: string;
  acceptedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CourseDetailResponseDto {
  id: string;
  title: string;
  description: string | null;
  isPublished: boolean;
  imageUrl?: string | null;
  topic?: string | null;
  difficulty?: CourseDifficulty | null;
  courseInstructors: CourseInstructorResponseDto[];
  createdAt: string;
  updatedAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export type ActionResult<T> = { success: true; data: T } | { success: false; error: string };
