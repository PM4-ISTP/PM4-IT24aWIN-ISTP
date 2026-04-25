import { getApiClient } from "@/src/shared/lib/api/server";
import { ActionResult } from "@/src/shared/lib/api/actionResult";

type ApiClient = Awaited<ReturnType<typeof getApiClient>>;

type OpenApiResponse<T> = {
  data?: T;
  error?: { error?: string };
};

export async function withApiResult<T>(
  action: (client: ApiClient) => Promise<OpenApiResponse<T>>,
  fallbackMessage: string
): Promise<ActionResult<T>> {
  try {
    const client = await getApiClient();
    const { data, error } = await action(client);

    if (!data || error) {
      // This also handles edge cases, where data and error are both undefined. It happens, when the user is not authenticated.
      return { success: false, error: error?.error ?? fallbackMessage };
    }

    return { success: true, data: data };
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : "Unknown error",
    };
  }
}
