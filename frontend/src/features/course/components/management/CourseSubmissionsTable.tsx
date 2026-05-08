"use client";

import { useEffect, useMemo, useState } from "react";
import { Alert, Badge, Box, Group, Loader, ScrollArea, Stack, Table, Text } from "@mantine/core";
import type { CourseLabSubmissionsResponseDto } from "@/src/shared/types/course";

function badgeColor(status: string): string {
  switch (status) {
    case "ON_TIME":
      return "teal";
    case "IN_PROGRESS":
      return "yellow";
    case "NOT_SUBMITTED":
    default:
      return "gray";
  }
}

function formatDue(value?: string | null): string {
  if (!value) return "â€”";
  try {
    const d = new Date(value);
    return d.toLocaleString("de-CH", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return value;
  }
}

export function CourseSubmissionsTable({ courseId }: { courseId: string }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [data, setData] = useState<CourseLabSubmissionsResponseDto | null>(null);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        setLoading(true);
        setError(null);
        const res = await fetch(
          `/api/backend/api/v1/courses/${encodeURIComponent(courseId)}/submissions`,
          {
            cache: "no-store",
          }
        );
        if (!res.ok) {
          const msg = await res.text();
          throw new Error(msg || res.statusText);
        }
        const json = (await res.json()) as CourseLabSubmissionsResponseDto;
        if (!cancelled) setData(json);
      } catch (e) {
        if (!cancelled) setError((e as Error).message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [courseId]);

  const matrix = useMemo(() => {
    if (!data) return null;
    const byKey = new Map<string, (typeof data.submissions)[number]>();
    for (const s of data.submissions ?? []) {
      byKey.set(`${s.participantId}:${s.labId}`, s);
    }
    const labs = [...(data.labs ?? [])].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0));
    const participants = data.participants ?? [];
    return { byKey, labs, participants };
  }, [data]);

  return (
    <Box
      style={{
        background: "rgba(255,255,255,0.04)",
        border: "1px solid rgba(255,255,255,0.08)",
        borderRadius: 14,
        padding: "1.5rem",
        boxShadow: "0 4px 24px rgba(0,0,0,0.25)",
      }}
    >
      <Stack gap="sm">
        <Group justify="space-between" align="center">
          <Text size="sm" fw={700} style={{ color: "#f1f5f9" }}>
            Submissions
          </Text>
          {loading ? <Loader size="sm" /> : null}
        </Group>

        {error ? (
          <Alert color="red" title="Failed to load submissions">
            {error}
          </Alert>
        ) : null}

        {!loading && matrix && matrix.labs.length === 0 ? (
          <Text size="sm" c="dimmed">
            No labs assigned.
          </Text>
        ) : null}

        {!loading && matrix && matrix.participants.length === 0 ? (
          <Text size="sm" c="dimmed">
            No participants enrolled.
          </Text>
        ) : null}

        {!loading && matrix && matrix.labs.length > 0 && matrix.participants.length > 0 ? (
          <ScrollArea>
            <Table withTableBorder highlightOnHover>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Student</Table.Th>
                  {matrix.labs.map((c) => (
                    <Table.Th key={c.labId}>
                      <Stack gap={2}>
                        <Text size="xs" fw={600}>
                          {c.labTitle}
                        </Text>
                        <Text size="xs" c="dimmed">
                          Due: {formatDue((c as { dueAt?: string | null }).dueAt ?? null)}
                        </Text>
                      </Stack>
                    </Table.Th>
                  ))}
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {matrix.participants.map((p) => (
                  <Table.Tr key={p.id}>
                    <Table.Td>
                      <Text size="sm">{p.name}</Text>
                    </Table.Td>
                    {matrix.labs.map((c) => {
                      const s = matrix.byKey.get(`${p.id}:${c.labId}`);
                      const status = s?.status ?? "NOT_SUBMITTED";
                      const progress =
                        s && s.totalChallengeCount > 0
                          ? `${s.solvedChallengeCount}/${s.totalChallengeCount}`
                          : "â€”";
                      return (
                        <Table.Td key={`${p.id}:${c.labId}`}>
                          <Stack gap={2}>
                            <Group gap="xs" wrap="nowrap">
                              <Badge variant="light" color={badgeColor(status)}>
                                {status}
                              </Badge>
                              <Text size="xs" c="dimmed">
                                {progress}
                              </Text>
                            </Group>
                          </Stack>
                        </Table.Td>
                      );
                    })}
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </ScrollArea>
        ) : null}
      </Stack>
    </Box>
  );
}
