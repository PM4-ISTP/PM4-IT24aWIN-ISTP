import { getApiClient } from "@/src/shared/lib/api/server";
import { type components } from "@/src/shared/lib/api/schema";

export type ActionResult<T> = { success: true; data: T } | { success: false; error: string };

type ApiClient = Awaited<ReturnType<typeof getApiClient>>;

type OpenApiResponse<T> = {
  data?: T;
  error?: components["schemas"]["ErrorDto"];
};

export async function withActionResult<T>(
  action: (client: ApiClient) => Promise<OpenApiResponse<T>>,
  fallbackMessage: string
): Promise<ActionResult<T>> {
  try {
    const client = await getApiClient();
    const { data, error } = await action(client);

    if (error !== undefined) {
      return { success: false, error: error?.error ?? fallbackMessage };
    }

    /*
      data can be either T or undefined. If error is not undefined, data must
      be in a valid state. So we can cast it to T. If we use
      withActionResultNoContent, then T is equal to void.
    */
    return { success: true, data: data as T };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}

export async function withActionResultNoContent(
  action: (client: ApiClient) => Promise<OpenApiResponse<void>>,
  fallbackMessage: string
): Promise<ActionResult<void>> {
  return await withActionResult(action, fallbackMessage);
}
