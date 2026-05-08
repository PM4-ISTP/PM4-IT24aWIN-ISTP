import type { components } from "@/src/shared/lib/api/schema";

export type ChallengeStudentDto = components["schemas"]["ChallengeStudentDto"];
export type ChallengeOptionStudentDto = components["schemas"]["ChallengeOptionStudentDto"];
export type LabStudentDto = components["schemas"]["LabStudentDto"];
export type ChallengeSubmissionRequestDto = components["schemas"]["ChallengeSubmissionRequestDto"];
export type ChallengeSubmissionResponseDto =
  components["schemas"]["ChallengeSubmissionResponseDto"];
export type ChoiceSubmissionResponseDto = components["schemas"]["ChoiceSubmissionResponseDto"];

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
  mcAttemptsMode?: string | null;
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
  mcAttemptsMode?: string | null;
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
  email?: string | null;
}

export interface CourseLabResponseDto {
  labId: string;
  labTitle: string;
  difficulty: string;
  orderIndex: number;
  dueAt?: string | null;
  maxScore?: number | null;
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
  mcAttemptsMode?: string | null;
  courseInstructors: CourseInstructorResponseDto[];
  participants: CourseParticipantDto[];
  courseLabs: CourseLabResponseDto[];
  createdAt: string;
  updatedAt: string;
}

export type CourseLabSubmissionStatusEnum = "NOT_SUBMITTED" | "IN_PROGRESS" | "ON_TIME";

export interface CourseChallengeSubmissionEntryDto {
  participantId: string;
  labId: string;
  solvedChallengeCount: number;
  totalChallengeCount: number;
  awardedPoints: number;
  maxPoints: number;
  completedAt: string | null;
  status: CourseLabSubmissionStatusEnum;
}

export interface CourseLabChallengeSubmissionDetailDto {
  challengeId: string;
  title: string;
  type: string;
  maxPoints: number;
  completed: boolean;
  correct: boolean | null;
  awardedPoints: number | null;
  overridePoints: number | null;
  submittedFlag: string | null;
  selectedOptionText: string | null;
}

export interface CourseLabSubmissionDetailDto {
  courseId: string;
  participantId: string;
  labId: string;
  labTitle: string;
  dueAt: string | null;
  completedAt: string | null;
  status: CourseLabSubmissionStatusEnum;
  awardedPoints: number;
  maxPoints: number;
  challenges: CourseLabChallengeSubmissionDetailDto[];
}

export interface CourseLabSubmissionsResponseDto {
  courseId: string;
  participants: CourseParticipantDto[];
  labs: CourseLabResponseDto[];
  submissions: CourseChallengeSubmissionEntryDto[];
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
  courseLabs: LabStudentDto[];
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
