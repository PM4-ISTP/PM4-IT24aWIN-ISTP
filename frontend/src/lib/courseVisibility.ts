import type { CourseVisibility } from "@/src/types/course";

export function visibilityFromFlags(isPublished: boolean, isPrivate: boolean): CourseVisibility {
  // Defensive fallback for legacy data: PRIVATE takes precedence over PUBLIC.
  if (isPublished && isPrivate) {
    return "PRIVATE";
  }
  if (isPrivate) {
    return "PRIVATE";
  }
  if (isPublished) {
    return "PUBLIC";
  }
  return "DRAFT";
}

export function visibilityToFlags(visibility: CourseVisibility): {
  isPublished: boolean;
  isPrivate: boolean;
} {
  switch (visibility) {
    case "PUBLIC":
      return { isPublished: true, isPrivate: false };
    case "PRIVATE":
      return { isPublished: false, isPrivate: true };
    case "DRAFT":
    default:
      return { isPublished: false, isPrivate: false };
  }
}

