import type { ChallengeStatusEnum, ChallengeDifficultyEnum } from "@/src/lib/actions/challenges";

export const CHALLENGE_SHORT_DESCRIPTION_MAX_CHARS = 200;

export const STATUS_OPTIONS: { value: ChallengeStatusEnum; label: string }[] = [
  { value: "DRAFT", label: "Draft" },
  { value: "PRIVATE", label: "Private" },
  { value: "PUBLIC", label: "Public" },
];

export const DIFFICULTY_OPTIONS: { value: ChallengeDifficultyEnum; label: string }[] = [
  { value: "BEGINNER", label: "Beginner" },
  { value: "EASY", label: "Easy" },
  { value: "MEDIUM", label: "Medium" },
  { value: "HARD", label: "Hard" },
  { value: "EXPERT", label: "Expert" },
];

export const STATUS_COLORS: Record<ChallengeStatusEnum, string> = {
  DRAFT: "gray",
  PRIVATE: "yellow",
  PUBLIC: "teal",
};

export const DIFFICULTY_COLORS: Record<ChallengeDifficultyEnum, string> = {
  BEGINNER: "green",
  EASY: "blue",
  MEDIUM: "yellow",
  HARD: "orange",
  EXPERT: "red",
};

export function getStatusColor(status: string): string {
  return STATUS_COLORS[status as ChallengeStatusEnum] ?? "gray";
}

export function getDifficultyColor(difficulty: string): string {
  return DIFFICULTY_COLORS[difficulty as ChallengeDifficultyEnum] ?? "gray";
}
