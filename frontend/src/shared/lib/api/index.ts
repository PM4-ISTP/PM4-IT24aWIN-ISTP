import createClient from "openapi-fetch";
import type { paths } from "./schema";

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
