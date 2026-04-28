export async function readBackendError(res: Response): Promise<string | null> {
  try {
    const body = await res.text();
    if (!body) return null;

    try {
      const json = JSON.parse(body) as unknown;
      if (
        json &&
        typeof json === "object" &&
        "error" in json &&
        typeof (json as { error?: unknown }).error === "string"
      ) {
        return (json as { error: string }).error;
      }
    } catch {
      // ignore JSON parse errors
    }

    return body;
  } catch {
    return null;
  }
}
