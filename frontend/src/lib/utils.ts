import sanitizeHtml from "sanitize-html";

export function isStringArray(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((item) => typeof item === "string");
}

/**
 * Returns up to two uppercase initials from a display name.
 * Used for avatar fallbacks across the app.
 */
export function getInitials(name: string): string {
  return name
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? "")
    .join("");
}

/**
 * Extracts a human-readable error message from a raw response body string.
 * Tries to parse JSON and read an `error` field; falls back to the raw text
 * or a provided fallback string.
 */
export function extractErrorMessage(text: string, fallback: string): string {
  if (!text) {
    return fallback;
  }

  try {
    const parsed: unknown = JSON.parse(text);
    if (typeof parsed === "object" && parsed !== null && "error" in parsed) {
      const errorValue = (parsed as { error?: unknown }).error;
      if (typeof errorValue === "string" && errorValue.trim()) {
        return errorValue;
      }
    }
    // Parsed successfully but no usable error field – fall back to raw text
    return text || fallback;
  } catch {
    // Not valid JSON – use raw text
    return text || fallback;
  }
}

/**
 * Sanitizes a HTML string, so that it can be used as input for `dangerouslySetInnerHTML`.
 *
 * @param html The HTML string that should get sanitized.
 * @returns The sanitized string.
 */
export function getSanitizedHtml(html: string) {
  return sanitizeHtml(html, {
    allowedTags: sanitizeHtml.defaults.allowedTags.concat(["img", "h1", "h2"]),
    allowedAttributes: {
      ...sanitizeHtml.defaults.allowedAttributes,
      img: ["src", "alt", "width", "height"],
    },
  });
}
