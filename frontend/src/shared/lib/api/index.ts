import createClient from "openapi-fetch";
import type { paths } from "./schema";

/**
 * Extracts a human-readable message from an openapi-fetch error payload.
 * The backend returns errors as `{ error: string }` (ErrorDto).
 */
export function apiErrorText(error: unknown): string | undefined {
  if (error && typeof error === "object" && "error" in error) {
    const value = (error as { error?: unknown }).error;
    if (typeof value === "string") return value;
  }
  return undefined;
}

export function createApiClient(baseUrl: string, accessToken?: string) {
  const headers = new Headers();
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  return createClient<paths>({
    baseUrl,
    headers,
  });
}
