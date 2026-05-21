export function formatDate(date?: string | null | number): string {
  if (!date) {
    return "No date specified";
  }

  return new Date(typeof date === "number" ? date : date).toLocaleDateString("de-CH", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

export function formatDateTime(dueAt: string | number): string {
  return new Date(dueAt).toLocaleString("de-CH", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}
