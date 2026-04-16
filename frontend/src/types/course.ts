import type { ChallengeDetailResponseDto } from "@/src/lib/actions/challenges";

export type InstructorRoleEnum = "OWNER" | "COLLABORATOR";
export type PlatformRole = "ROLE_ADMINISTRATOR" | "ROLE_INSTRUCTOR" | "ROLE_STUDENT";
export type CourseVisibility = "DRAFT" | "PUBLIC" | "PRIVATE";

export interface CourseUserSummary {
  id: string | null;
  name: string;
  email: string;
  username?: string | null;
  picture?: string | null;
  title?: string | null;
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
  shortDescription: string | null;
  isPublished: boolean;
  isPrivate: boolean;
  imageUrl?: string | null;
  topic?: string | null;
  instructors: InstructorAssignment[];
}

export interface CourseResponseDto {
  id: string;
  title: string;
  description: string | null;
  shortDescription: string | null;
  isPublished: boolean;
  isPrivate: boolean;
  imageUrl?: string | null;
  topic?: string | null;
  instructors: InstructorAssignment[];
  createdAt: string;
  updatedAt: string;
}

export interface ListCourseResponseDto {
  id: string;
  title: string;
  description: string | null;
  shortDescription: string | null;
  isPublished: boolean;
  isPrivate: boolean;
  instructorCount: number;
  imageUrl?: string | null;
  topic?: string | null;
  ownerName?: string | null;
  ownerPicture?: string | null;
  ownerTitle?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateCourseDto {
  title: string;
  description: string;
  shortDescription: string | null;
  isPublished: boolean;
  isPrivate: boolean;
  imageUrl?: string | null;
  topic?: string | null;
  instructors: InstructorAssignment[];
}

export interface CourseInstructorResponseDto {
  id: string | null;
  instructorRole: InstructorRoleEnum;
  isAccepted: boolean;
  instructor: CourseUserSummary;
  invitedAt: string;
  acceptedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CourseParticipantDto {
  id: string;
  name: string;
  picture: string | null;
}

export interface CourseChallengeResponseDto {
  challengeId: string;
  challengeTitle: string;
  difficulty: string;
  orderIndex: number;
}

export interface CourseDetailResponseDto {
  id: string;
  title: string;
  description: string | null;
  shortDescription: string | null;
  participantCount: number;
  isEnrolled: boolean;
  isPublished: boolean;
  isPrivate: boolean;
  inviteCode?: string | null;
  imageUrl?: string | null;
  topic?: string | null;
  courseInstructors: CourseInstructorResponseDto[];
  courseChallenges: CourseChallengeResponseDto[];
  createdAt: string;
  updatedAt: string;
}

export interface PublicCourseDetailResponseDto {
  id: string;
  title: string;
  description: string | null;
  shortDescription: string | null;
  participantCount: number;
  isEnrolled: boolean;
  isPublished: boolean;
  imageUrl?: string | null;
  topic?: string | null;
  courseInstructors: CourseInstructorResponseDto[];
  participants: null;
  courseChallenges: ChallengeDetailResponseDto[];
  createdAt: string;
  updatedAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export type ActionResult<T> = { success: true; data: T } | { success: false; error: string };
