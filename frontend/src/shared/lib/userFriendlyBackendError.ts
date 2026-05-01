const FIELD_LABELS: Record<string, string> = {
  title: "Title",
  description: "Description",
  shortDescription: "Short description",
  topic: "Topic",
  imageUrl: "Image URL",
  maxScore: "Max score",
  value: "Value",
};

function prettifyFieldName(rawField: string | null): string | null {
  if (!rawField) return null;

  // Examples we might see:
  // - "title" (Spring FieldError)
  // - "addTopic.value" / "request.value"
  // - "createCourseRequestDto.title" (rare, but possible via constraint violations)
  const lastSegment = rawField.split(".").at(-1) ?? rawField;
  const cleaned = lastSegment.replace(/\[\d+]/g, "").trim();
  if (!cleaned) return null;

  return FIELD_LABELS[cleaned] ?? cleaned;
}

/**
 * Turns raw backend errors into messages that are ok to show to end-users.
 *
 * Handles common cases:
 * - "HTTP 400: ..." or "400: ..." prefixes
 * - Spring validation errors like "fieldName: message"
 * - Rewrites common "must be at most X characters" into a clearer message
 */
export function toUserFriendlyBackendError(raw: string | null | undefined): string | null {
  if (!raw) return null;
  let text = raw.trim();
  if (!text) return null;

  // Strip HTTP status prefixes that some callers include (e.g. "401: ..." / "HTTP 401: ...")
  const httpPrefix = text.match(/^HTTP\s+(\d{3})\s*[:\-–—]?\s*(.*)$/i);
  if (httpPrefix) {
    text = (httpPrefix[2] ?? "").trim() || text;
  } else {
    const statusPrefix = text.match(/^(\d{3})\s*[:\-–—]\s*(.*)$/);
    if (statusPrefix) {
      text = (statusPrefix[2] ?? "").trim() || text;
    }
  }

  // Backend validation errors often come as: "fieldName: message"
  let field: string | null = null;
  const fieldPrefixMatch = text.match(/^([A-Za-z0-9_.\[\]]+):\s*(.+)$/);
  if (fieldPrefixMatch?.[1] && fieldPrefixMatch?.[2]) {
    field = fieldPrefixMatch[1];
    text = fieldPrefixMatch[2].trim();
  }

  // Normalize some "generic" backend messages into nicer user text
  if (text === "An unknown error occurred") {
    return "Something went wrong. Please try again.";
  }
  if (text === "Access denied") {
    return "You don’t have permission to do that.";
  }

  const label = prettifyFieldName(field);

  const maxChars = text.match(/must be at most\s+(\d+)\s+characters/i);
  if (maxChars?.[1]) {
    return label
      ? `${label} is too long (max ${maxChars[1]} characters).`
      : `Text is too long (max ${maxChars[1]} characters).`;
  }

  const betweenChars = text.match(/must be between\s+(\d+)\s+and\s+(\d+)\s+characters/i);
  if (betweenChars?.[1] && betweenChars?.[2]) {
    return label
      ? `${label} must be between ${betweenChars[1]} and ${betweenChars[2]} characters.`
      : `Text must be between ${betweenChars[1]} and ${betweenChars[2]} characters.`;
  }

  if (/is required/i.test(text) || /must not be blank/i.test(text)) {
    return label ? `${label} is required.` : "This field is required.";
  }

  return text;
}
