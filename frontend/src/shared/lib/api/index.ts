import createClient from "openapi-fetch";
import type { paths } from "./schema";

export function createApiClient(baseUrl: string, accessToken: string) {
  return createClient<paths>({
    baseUrl,
    headers: { Authorization: `Bearer ${accessToken}` },
  });
}
