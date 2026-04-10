export const COURSE_SHORT_DESCRIPTION_MAX_WORDS = 30;

export function countWords(value: string): number {
  const normalized = value.trim();
  return normalized ? normalized.split(/\s+/).length : 0;
}

export function normalizeShortDescription(value: string): string {
  return value.trim().replace(/\s+/g, " ");
}

export function getPlainTextFromHtml(value: string | null | undefined): string {
  if (!value) {
    return "";
  }

  return value
    .replace(/<\/(p|h[1-6]|li|br|div)>/gi, " ")
    .replace(/<[^>]*>/g, "")
    .replace(/\s+/g, " ")
    .trim();
}

export function getCoursePreviewText(
  shortDescription: string | null | undefined,
  description: string | null | undefined
): string {
  const normalizedShortDescription = normalizeShortDescription(shortDescription ?? "");
  if (normalizedShortDescription) {
    return normalizedShortDescription;
  }

  return getPlainTextFromHtml(description);
}
