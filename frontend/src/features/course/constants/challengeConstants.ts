import type { LabStatusEnum, LabDifficultyEnum } from "@/src/features/course/actions/labs";

export const CHALLENGE_SHORT_DESCRIPTION_MAX_CHARS = 200;

export const DOCKER_IMAGE_PATTERN =
  /^ghcr\.io\/[\w.-]+\/[\w./-]+((:[\w.-]+)|(@sha256:[A-Fa-f0-9]{64}))?$/;
export const DOCKER_IMAGE_ERROR =
  "Docker image must be a public GHCR reference (e.g. ghcr.io/school-org/lab:1.0.0)";

export const STATUS_OPTIONS: { value: LabStatusEnum; label: string }[] = [
  { value: "DRAFT", label: "Draft" },
  { value: "PRIVATE", label: "Private" },
  { value: "PUBLIC", label: "Public" },
];

export const DIFFICULTY_OPTIONS: { value: LabDifficultyEnum; label: string }[] = [
  { value: "BEGINNER", label: "Beginner" },
  { value: "EASY", label: "Easy" },
  { value: "MEDIUM", label: "Medium" },
  { value: "HARD", label: "Hard" },
  { value: "EXPERT", label: "Expert" },
];

export const STATUS_COLORS: Record<LabStatusEnum, string> = {
  DRAFT: "gray",
  PRIVATE: "yellow",
  PUBLIC: "teal",
};

export const DIFFICULTY_COLORS: Record<LabDifficultyEnum, string> = {
  BEGINNER: "green",
  EASY: "blue",
  MEDIUM: "yellow",
  HARD: "orange",
  EXPERT: "red",
};

export function getStatusColor(status: string): string {
  return STATUS_COLORS[status as LabStatusEnum] ?? "gray";
}

export function getDifficultyColor(difficulty: string): string {
  return DIFFICULTY_COLORS[difficulty as LabDifficultyEnum] ?? "gray";
}
