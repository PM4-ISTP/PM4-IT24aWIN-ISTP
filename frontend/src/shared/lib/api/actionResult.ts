import { getApiClient } from "@/src/shared/lib/api/server";
import { type components } from "@/src/shared/lib/api/schema";

export type ActionResult<T> = { success: true; data: T } | { success: false; error: string };

type ApiClient = Awaited<ReturnType<typeof getApiClient>>;

type OpenApiResponse<T> = {
  response: Response;
  data?: T;
  error?: components["schemas"]["ErrorDto"];
};

export async function performFetch<T>(
  successResultMapper: (data: T | undefined) => ActionResult<T>,
  action: (client: ApiClient) => Promise<OpenApiResponse<T>>,
  fallbackMessage: string
): Promise<ActionResult<T>> {
  try {
    const client = await getApiClient();
    const { response, data, error } = await action(client);

    if (error !== undefined) {
      return { success: false, error: error?.error ?? fallbackMessage };
    }
    if (!response.ok) {
      return { success: false, error: fallbackMessage };
    }

    return successResultMapper(data);
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}

export function withActionResult<T extends NonNullable<unknown>>(
  action: (client: ApiClient) => Promise<OpenApiResponse<T>>,
  fallbackMessage: string
): Promise<ActionResult<T>> {
  return performFetch(
    (data) => {
      if (data === undefined) {
        return { success: false, error: fallbackMessage };
      } else {
        return { success: true, data };
      }
    },
    action,
    fallbackMessage
  );
}

export function withActionResultNoContent(
  action: (client: ApiClient) => Promise<OpenApiResponse<void>>,
  fallbackMessage: string
): Promise<ActionResult<void>> {
  return performFetch(
    (data) => {
      if (data !== undefined) {
        return { success: false, error: fallbackMessage };
      } else {
        return { success: true, data };
      }
    },
    action,
    fallbackMessage
  );
}
