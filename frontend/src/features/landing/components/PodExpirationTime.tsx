"use client";

import { Text } from "@mantine/core";

function formatDateTime(value?: string | null): string | null {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return date.toLocaleString("de-CH", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function PodExpirationTime({ expiresAt }: { expiresAt: string }) {
  return (
    <Text size="sm" c="dimmed">
      Expires {formatDateTime(expiresAt)}
    </Text>
  );
}
