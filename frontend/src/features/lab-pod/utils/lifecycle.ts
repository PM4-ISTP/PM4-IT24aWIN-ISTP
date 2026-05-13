export const DEFAULT_MAX_EXTENSION_COUNT = 2;

export function formatTimeLeft(msLeft: number | null): string | null {
  if (msLeft === null) return null;
  if (msLeft <= 0) return "expired";

  const totalMinutes = Math.ceil(msLeft / 60_000);
  if (totalMinutes < 60) {
    return `${totalMinutes} min`;
  }

  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  return minutes === 0 ? `${hours} h` : `${hours} h ${minutes} min`;
}

export function getExtensionSummary(
  extensionCount?: number | null,
  maxExtensionCount?: number | null
): {
  used: number;
  max: number;
  label: string;
} {
  const used = Math.max(0, extensionCount ?? 0);
  const max = Math.max(0, maxExtensionCount ?? DEFAULT_MAX_EXTENSION_COUNT);
  return {
    used,
    max,
    label: max === 0 ? "No extensions available" : `${used} / ${max} extensions used`,
  };
}

export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (!error) return fallback;
  if (error instanceof Error) return error.message;
  if (typeof error === "string") return error;
  if (typeof error === "object" && "message" in error) {
    const message = (error as { message?: unknown }).message;
    if (typeof message === "string" && message.trim()) return message;
  }
  if (typeof error === "object" && "error" in error) {
    const message = (error as { error?: unknown }).error;
    if (typeof message === "string" && message.trim()) return message;
  }
  return fallback;
}
