"use client";

import { useState } from "react";
import {
  Stack,
  Group,
  Text,
  Badge,
  Box,
  Divider,
  ActionIcon,
} from "@mantine/core";
import { IconX } from "@tabler/icons-react";
import Link from "next/link";

export type DeadlineItem = {
  courseId: string;
  courseTitle: string;
  challengeId: string;
  challengeTitle: string;
  dueAt: string;
};

type Props = {
  deadlines: DeadlineItem[];
  userId?: string;
};

function formatDue(value?: string | null): string {
  if (!value) return "–";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("de-CH", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function getStorageKey(userId?: string) {
  return userId ? `dismissed_deadlines_${userId}` : "dismissed_deadlines";
}

export function DeadlineWidget({ deadlines, userId }: Props) {
  const [dismissed, setDismissed] = useState<Set<string>>(() => {
    // Lazy initializer: runs only on client (server returns empty set)
    if (typeof window === "undefined") return new Set<string>();
    try {
      const raw = localStorage.getItem(getStorageKey(userId));
      return raw ? new Set<string>(JSON.parse(raw) as string[]) : new Set<string>();
    } catch {
      return new Set<string>();
    }
  });

  function dismiss(courseId: string, challengeId: string) {
    const key = `${courseId}:${challengeId}`;
    setDismissed((prev) => {
      const next = new Set(prev);
      next.add(key);
      try {
        localStorage.setItem(getStorageKey(userId), JSON.stringify([...next]));
      } catch {
        // ignore
      }
      return next;
    });
  }

  const now = new Date();

  const visible = deadlines.filter((it) => {
    const key = `${it.courseId}:${it.challengeId}`;
    return !dismissed.has(key);
  });

  // Sort: overdue first (ascending = oldest overdue first), then upcoming (ascending = soonest first)
  const overdue = visible
    .filter((it) => new Date(it.dueAt).getTime() < now.getTime())
    .sort((a, b) => new Date(a.dueAt).getTime() - new Date(b.dueAt).getTime());

  const upcoming = visible
    .filter((it) => new Date(it.dueAt).getTime() >= now.getTime())
    .sort((a, b) => new Date(a.dueAt).getTime() - new Date(b.dueAt).getTime());

  const sorted = [...overdue, ...upcoming];

  if (sorted.length === 0) {
    return (
      <Text size="sm" c="dimmed">
        No deadlines set for your enrolled courses.
      </Text>
    );
  }

  return (
    <Stack gap="xs">
      {sorted.map((it, idx) => {
        const isOverdue = new Date(it.dueAt).getTime() < now.getTime();
        const key = `${it.courseId}:${it.challengeId}`;

        return (
          <Box key={`${key}:${idx}`}>
            {idx > 0 ? <Divider my={8} style={{ opacity: 0.35 }} /> : null}
            <Group justify="space-between" align="flex-start" wrap="nowrap" gap="xs">
              {/* Left: course + lab info */}
              <Stack gap={2} style={{ minWidth: 0, flex: 1 }}>
                <Text
                  size="xs"
                  c="dimmed"
                  truncate
                  title={it.courseTitle}
                  style={{ fontFamily: "var(--font-space-grotesk), sans-serif" }}
                >
                  {it.courseTitle || "–"}
                </Text>
                <Link
                  href={`/dashboard/courses/${encodeURIComponent(
                    it.courseId
                  )}/challenges/${encodeURIComponent(it.challengeId)}/play`}
                  style={{
                    color: "#e2e8f0",
                    fontFamily: "var(--font-space-grotesk), sans-serif",
                    fontSize: "0.9rem",
                    fontWeight: 600,
                    textDecoration: "none",
                    overflow: "hidden",
                    textOverflow: "ellipsis",
                    whiteSpace: "nowrap",
                    display: "block",
                  }}
                  title={it.challengeTitle}
                >
                  {it.challengeTitle}
                </Link>
              </Stack>

              {/* Right: badge + date + optional dismiss */}
              <Stack gap={4} align="flex-end" style={{ flexShrink: 0 }}>
                <Group gap={4} align="center" wrap="nowrap">
                  <Badge variant="light" color={isOverdue ? "red" : "blue"} size="sm">
                    {isOverdue ? "OVERDUE" : "DUE"}
                  </Badge>
                  {isOverdue && (
                    <ActionIcon
                      size="xs"
                      variant="subtle"
                      color="red"
                      title="Aus Kalender entfernen"
                      onClick={() => dismiss(it.courseId, it.challengeId)}
                      style={{ opacity: 0.7 }}
                    >
                      <IconX size={12} />
                    </ActionIcon>
                  )}
                </Group>
                <Text size="xs" c={isOverdue ? "red.3" : "dimmed"}>
                  {formatDue(it.dueAt)}
                </Text>
              </Stack>
            </Group>
          </Box>
        );
      })}
    </Stack>
  );
}
