import type { CourseDifficulty } from "@/src/shared/types/course";

export function difficultyColor(d: CourseDifficulty | null | undefined): string {
  switch (d) {
    case "BEGINNER":
      return "green";
    case "INTERMEDIATE":
      return "blue";
    case "ADVANCED":
      return "red";
    default:
      return "gray";
  }
}

export function difficultyLabel(d: CourseDifficulty | null | undefined): string {
  switch (d) {
    case "BEGINNER":
      return "Beginner";
    case "INTERMEDIATE":
      return "Intermediate";
    case "ADVANCED":
      return "Advanced";
    default:
      return "";
  }
}
