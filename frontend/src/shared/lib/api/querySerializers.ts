/**
 * Spring Boot resolves Pageable from flat query params (page, size, sort),
 * but openapi-typescript models it as a nested object. This serializer
 * flattens the pageable params so Spring can read them.
 */
export function springPageableSerializer(params: Record<string, unknown>): string {
  const parts: string[] = [];
  for (const [key, val] of Object.entries(params)) {
    if (key === "pageable" && typeof val === "object" && val !== null) {
      for (const [pk, pv] of Object.entries(val as Record<string, unknown>)) {
        if (pv == null) continue;
        if (Array.isArray(pv)) {
          for (const item of pv) {
            if (item != null) parts.push(`${pk}=${encodeURIComponent(String(item))}`);
          }
        } else {
          parts.push(`${pk}=${encodeURIComponent(String(pv as string | number))}`);
        }
      }
    } else if (val != null) {
      parts.push(`${key}=${encodeURIComponent(String(val as string | number))}`);
    }
  }
  return parts.join("&");
}
