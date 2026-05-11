"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useSession } from "next-auth/react";
import {
  ActionIcon,
  Badge,
  Box,
  Button,
  Code,
  Collapse,
  Divider,
  Group,
  Loader,
  Stack,
  Text,
  TextInput,
  Title,
} from "@mantine/core";
import {
  IconArrowDown,
  IconArrowUp,
  IconBook2,
  IconChevronDown,
  IconChevronRight,
  IconListCheck,
  IconPencil,
  IconPlus,
  IconTrash,
} from "@tabler/icons-react";
import { LabSearchSelect } from "@/src/features/course/components/labs/LabSearchSelect";
import {
  fetchChallenge,
  type ChallengeDetailResponseDto,
  type ListLabResponseDto,
} from "@/src/features/course/actions/labs";
import {
  getDifficultyColor,
  getStatusColor,
} from "@/src/features/course/constants/challengeConstants";
import { getSanitizedHtml } from "@/src/shared/lib/utils";

export interface CourseChallengeEntry {
  labId: string;
  labTitle: string;
  difficulty: string;
  orderIndex: number;
  /** ISO local datetime string (e.g. 2026-05-01T12:00:00) or null when no deadline is set. */
  dueAt?: string | null;
  creatorName?: string;
  creatorId?: string;
  status?: string;
}

interface CourseChallengeManagerProps {
  labs: CourseChallengeEntry[];
  onChange: (labs: CourseChallengeEntry[]) => void;
}

type ChallengeDetail = NonNullable<ChallengeDetailResponseDto["challenges"]>[number];

function ChallengeListView({ challenges }: { challenges: ChallengeDetail[] }) {
  const [expandedKey, setExpandedKey] = useState<string | null>(null);

  const sorted = [...challenges].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0));

  return (
    <Stack gap="xs">
      <Group gap="sm" align="center">
        <IconListCheck size={16} color="#60a5fa" />
        <Text size="sm" fw={600} style={{ color: "#f1f5f9" }}>
          Challenges ({sorted.length})
        </Text>
      </Group>

      <Stack gap="xs">
        {sorted.map((st, i) => {
          const key = st.id ?? `local-${i}`;
          const isExpanded = expandedKey === key;
          const description = st.description ? getSanitizedHtml(st.description) : null;
          const flag = st.flag?.trim();
          const hasFlag = Boolean(flag);
          const title = st.title?.trim() || `Lab ${i + 1}`;

          return (
            <Box
              key={key}
              style={{
                border: "1px solid var(--mantine-color-default-border)",
                borderRadius: "var(--mantine-radius-sm)",
                overflow: "hidden",
              }}
            >
              <Box
                p="xs"
                style={{ cursor: "pointer" }}
                onClick={() => setExpandedKey(isExpanded ? null : key)}
              >
                <Group justify="space-between" align="center" wrap="nowrap">
                  <Group gap="xs" wrap="nowrap" align="center" style={{ flex: 1, minWidth: 0 }}>
                    <ActionIcon variant="transparent" size="sm" tabIndex={-1}>
                      {isExpanded ? <IconChevronDown size={14} /> : <IconChevronRight size={14} />}
                    </ActionIcon>
                    <Text size="xs" c="dimmed" fw={600} style={{ flexShrink: 0 }}>
                      #{i + 1}
                    </Text>
                    <Text size="sm" fw={500} truncate style={{ flex: 1, minWidth: 0 }}>
                      {title}
                    </Text>
                  </Group>
                  {hasFlag && (
                    <Badge size="xs" variant="light" color="grape">
                      Flag
                    </Badge>
                  )}
                </Group>
              </Box>
              <Collapse expanded={isExpanded}>
                <Box
                  p="sm"
                  style={{
                    borderTop: "1px solid rgba(255,255,255,0.08)",
                    background: "rgba(255,255,255,0.02)",
                  }}
                >
                  <Stack gap="md">
                    <Stack gap="xs">
                      <Group gap="xs" align="center">
                        <IconBook2 size={14} color="#60a5fa" />
                        <Text size="xs" fw={600} style={{ color: "#f1f5f9" }}>
                          Description
                        </Text>
                      </Group>
                      {description ? (
                        <Box
                          className="course-description"
                          style={{
                            fontSize: "var(--mantine-font-size-sm)",
                            color: "#cbd5e1",
                          }}
                          dangerouslySetInnerHTML={{ __html: description }}
                        />
                      ) : (
                        <Text size="sm" c="dimmed">
                          No description.
                        </Text>
                      )}
                    </Stack>

                    {hasFlag && (
                      <Stack gap={4}>
                        <Text size="xs" fw={600} style={{ color: "#f1f5f9" }}>
                          Flag:
                        </Text>
                        <Code>{flag}</Code>
                      </Stack>
                    )}
                  </Stack>
                </Box>
              </Collapse>
            </Box>
          );
        })}
      </Stack>
    </Stack>
  );
}

function ChallengeDetailView({ detail }: { detail: ChallengeDetailResponseDto }) {
  const sanitizedDescription = detail.description ? getSanitizedHtml(detail.description) : null;
  const challenges = detail.challenges ?? [];

  return (
    <Stack gap="md">
      {/* Metadata */}
      <Group gap="lg">
        <Text size="xs" c="dimmed">
          Max Score: {detail.maxScore ?? 0}
        </Text>
        <Text size="xs" c="dimmed">
          Updated:{" "}
          {detail.updatedAt
            ? new Date(detail.updatedAt).toLocaleString("de-CH", {
                day: "2-digit",
                month: "2-digit",
                year: "numeric",
                hour: "2-digit",
                minute: "2-digit",
              })
            : "–"}
        </Text>
      </Group>

      {/* Description */}
      {sanitizedDescription && (
        <>
          <Divider style={{ borderColor: "rgba(255,255,255,0.08)" }} />
          <Stack gap="xs">
            <Group gap="sm" align="center">
              <IconBook2 size={16} color="#60a5fa" />
              <Text size="sm" fw={600} style={{ color: "#f1f5f9" }}>
                Description
              </Text>
            </Group>
            <Box
              className="course-description"
              style={{ fontSize: "var(--mantine-font-size-sm)", color: "#cbd5e1" }}
              dangerouslySetInnerHTML={{ __html: sanitizedDescription }}
            />
          </Stack>
        </>
      )}

      {/* Challenges */}
      {challenges.length > 0 && (
        <>
          <Divider style={{ borderColor: "rgba(255,255,255,0.08)" }} />
          <ChallengeListView challenges={challenges} />
        </>
      )}
    </Stack>
  );
}

function toDateTimeLocalValue(value?: string | null): string {
  if (!value) return "";
  // backend uses LocalDateTime -> ISO_LOCAL_DATE_TIME (no timezone), typically with seconds
  // datetime-local expects YYYY-MM-DDTHH:mm
  if (value.length >= 16) return value.slice(0, 16);
  return value;
}

function fromDateTimeLocalValue(value: string): string | null {
  const trimmed = value.trim();
  if (!trimmed) return null;
  // normalize to include seconds
  return trimmed.length === 16 ? `${trimmed}:00` : trimmed;
}

export function CourseLabManager({ labs, onChange }: CourseChallengeManagerProps) {
  const { data: session } = useSession();
  const currentUserId = (session as { userId?: string } | null)?.userId;

  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [detailsCache, setDetailsCache] = useState<Record<string, ChallengeDetailResponseDto>>({});
  const [loadingDetailId, setLoadingDetailId] = useState<string | null>(null);

  function handleAddChallenge(lab: ListLabResponseDto) {
    if (!lab.id) return;
    if (labs.some((c) => c.labId === lab.id)) return;

    const newEntry: CourseChallengeEntry = {
      labId: lab.id,
      labTitle: lab.title ?? "",
      difficulty: lab.difficulty ?? "MEDIUM",
      orderIndex: labs.length,
      dueAt: null,
      creatorName: lab.creatorName ?? undefined,
      status: lab.status ?? undefined,
    };
    onChange([...labs, newEntry]);
  }

  function handleRemove(labId: string) {
    const updated = labs.filter((c) => c.labId !== labId).map((c, i) => ({ ...c, orderIndex: i }));
    onChange(updated);
    if (expandedId === labId) setExpandedId(null);
  }

  function handleMoveUp(index: number) {
    if (index === 0) return;
    const updated = [...labs];
    [updated[index - 1], updated[index]] = [updated[index], updated[index - 1]];
    onChange(updated.map((c, i) => ({ ...c, orderIndex: i })));
  }

  function handleMoveDown(index: number) {
    if (index === labs.length - 1) return;
    const updated = [...labs];
    [updated[index], updated[index + 1]] = [updated[index + 1], updated[index]];
    onChange(updated.map((c, i) => ({ ...c, orderIndex: i })));
  }

  /** Hydrate a lab entry with data from the detail response */
  function hydrateEntry(labId: string, detail: ChallengeDetailResponseDto) {
    const idx = labs.findIndex((c) => c.labId === labId);
    if (idx === -1) return;
    const entry = labs[idx];
    if (entry.creatorName && entry.status && entry.creatorId) return;

    const updated = [...labs];
    updated[idx] = {
      ...entry,
      creatorName: entry.creatorName ?? detail.creator?.name ?? undefined,
      creatorId: entry.creatorId ?? detail.creator?.id ?? undefined,
      status: entry.status ?? detail.status ?? undefined,
    };
    onChange(updated);
  }

  // Preload details for labs that are missing metadata (e.g. loaded from course API)
  const preloadedRef = useRef<Set<string>>(new Set());
  useEffect(() => {
    const missing = labs.filter((c) => !c.status && !preloadedRef.current.has(c.labId));
    if (missing.length === 0) return;

    missing.forEach((c) => preloadedRef.current.add(c.labId));

    void (async () => {
      const results = await Promise.all(
        missing.map(async (c) => {
          const result = await fetchChallenge(c.labId);
          return result.success ? { id: c.labId, detail: result.data } : null;
        })
      );

      // Build a lookup of fetched details
      const fetched = new Map<string, ChallengeDetailResponseDto>();
      for (const r of results) {
        if (r) fetched.set(r.id, r.detail);
      }
      if (fetched.size === 0) return;

      // Update cache
      setDetailsCache((prev) => {
        const next = { ...prev };
        for (const [id, detail] of fetched) next[id] = detail;
        return next;
      });

      // Batch-hydrate all entries in a single onChange call
      let updated = false;
      const hydrated = labs.map((entry) => {
        const detail = fetched.get(entry.labId);
        if (!detail) return entry;
        if (entry.creatorName && entry.status && entry.creatorId) return entry;
        updated = true;
        return {
          ...entry,
          creatorName: entry.creatorName ?? detail.creator?.name ?? undefined,
          creatorId: entry.creatorId ?? detail.creator?.id ?? undefined,
          status: entry.status ?? detail.status ?? undefined,
        };
      });
      if (updated) onChange(hydrated);
    })();
  }, [labs, onChange]);

  async function handleToggleExpand(labId: string) {
    if (expandedId === labId) {
      setExpandedId(null);
      return;
    }

    setExpandedId(labId);

    if (!detailsCache[labId]) {
      setLoadingDetailId(labId);
      const result = await fetchChallenge(labId);
      setLoadingDetailId((prev) => (prev === labId ? null : prev));

      if (result.success) {
        setDetailsCache((prev) => ({ ...prev, [labId]: result.data }));
        hydrateEntry(labId, result.data);
      }
    }
  }

  return (
    <Stack gap="md">
      <Group justify="space-between" align="center">
        <Title order={4}>Course Labs</Title>
        <Button
          variant="light"
          size="xs"
          leftSection={<IconPlus size={14} />}
          component={Link}
          href="/dashboard/instructor/labs/create"
          target="_blank"
          rel="noopener noreferrer"
        >
          New Lab
        </Button>
      </Group>

      <LabSearchSelect excludeIds={labs.map((c) => c.labId)} onSelect={handleAddChallenge} />

      {labs.length === 0 ? (
        <Text size="sm" c="dimmed">
          No labs assigned to this course yet.
        </Text>
      ) : (
        <Stack gap="sm">
          {labs.map((lab, index) => {
            const isExpanded = expandedId === lab.labId;
            const detail = detailsCache[lab.labId];
            const isLoadingDetail = loadingDetailId === lab.labId;
            const isOwner =
              (lab.creatorId != null && lab.creatorId === currentUserId) ||
              (detail?.creator?.id != null && detail.creator.id === currentUserId);

            return (
              <Box
                key={lab.labId}
                style={{
                  border: "1px solid var(--mantine-color-default-border)",
                  borderRadius: "var(--mantine-radius-md)",
                  overflow: "hidden",
                }}
              >
                {/* Collapsed card — always visible */}
                <Box
                  p="md"
                  style={{ cursor: "pointer" }}
                  onClick={() => void handleToggleExpand(lab.labId)}
                >
                  <Group justify="space-between" align="flex-start" wrap="nowrap">
                    {/* Left: content */}
                    <Group
                      gap="sm"
                      wrap="nowrap"
                      align="flex-start"
                      style={{ flex: 1, minWidth: 0 }}
                    >
                      <ActionIcon variant="transparent" size="sm" tabIndex={-1} mt={2}>
                        {isExpanded ? (
                          <IconChevronDown size={16} />
                        ) : (
                          <IconChevronRight size={16} />
                        )}
                      </ActionIcon>

                      <Stack gap={6} style={{ flex: 1, minWidth: 0 }}>
                        {/* Row 1: Number + Title */}
                        <Group gap="xs" wrap="nowrap">
                          <Text size="sm" c="dimmed" fw={600} style={{ flexShrink: 0 }}>
                            #{index + 1}
                          </Text>
                          <Text size="sm" fw={600} truncate>
                            {lab.labTitle}
                          </Text>
                        </Group>

                        {/* Row 2: Difficulty + Creator */}
                        <Group gap="xs">
                          <Text size="xs" c="dimmed">
                            Difficulty:{" "}
                            <Text span size="xs" fw={600} c={getDifficultyColor(lab.difficulty)}>
                              {lab.difficulty}
                            </Text>
                          </Text>
                          {lab.creatorName && (
                            <>
                              <Text size="xs" c="dimmed">
                                |
                              </Text>
                              <Text size="xs" c="dimmed">
                                by {lab.creatorName}
                              </Text>
                            </>
                          )}
                        </Group>
                      </Stack>
                    </Group>

                    {/* Right: status + action buttons */}
                    <Group
                      gap="xs"
                      wrap="nowrap"
                      align="center"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <TextInput
                        label="Due date & time"
                        type="datetime-local"
                        size="xs"
                        w={210}
                        value={toDateTimeLocalValue(lab.dueAt)}
                        onChange={(e) => {
                          const next = fromDateTimeLocalValue(e.currentTarget.value);
                          const updated = [...labs];
                          updated[index] = { ...updated[index], dueAt: next };
                          onChange(updated);
                        }}
                        placeholder="Optional"
                      />
                      {lab.status && (
                        <Badge size="xs" variant="light" color={getStatusColor(lab.status)}>
                          {lab.status}
                        </Badge>
                      )}
                      {isOwner && (
                        <ActionIcon
                          variant="subtle"
                          size="sm"
                          component={Link}
                          href={`/dashboard/instructor/labs/${lab.labId}`}
                          aria-label="Edit lab"
                          title="Edit lab"
                        >
                          <IconPencil size={14} />
                        </ActionIcon>
                      )}
                      <ActionIcon
                        variant="subtle"
                        size="sm"
                        disabled={index === 0}
                        onClick={() => handleMoveUp(index)}
                        aria-label="Move up"
                      >
                        <IconArrowUp size={14} />
                      </ActionIcon>
                      <ActionIcon
                        variant="subtle"
                        size="sm"
                        disabled={index === labs.length - 1}
                        onClick={() => handleMoveDown(index)}
                        aria-label="Move down"
                      >
                        <IconArrowDown size={14} />
                      </ActionIcon>
                      <ActionIcon
                        variant="subtle"
                        size="sm"
                        color="red"
                        onClick={() => handleRemove(lab.labId)}
                        aria-label="Remove"
                      >
                        <IconTrash size={14} />
                      </ActionIcon>
                    </Group>
                  </Group>
                </Box>

                {/* Expanded detail */}
                <Collapse expanded={isExpanded}>
                  <Box
                    p="md"
                    style={{
                      borderTop: "1px solid rgba(255,255,255,0.08)",
                      background: "rgba(255,255,255,0.02)",
                    }}
                  >
                    {isLoadingDetail && !detail ? (
                      <Group justify="center" p="xs">
                        <Loader size="sm" />
                      </Group>
                    ) : detail ? (
                      <ChallengeDetailView detail={detail} />
                    ) : (
                      <Text size="sm" c="dimmed">
                        Failed to load details.
                      </Text>
                    )}
                  </Box>
                </Collapse>
              </Box>
            );
          })}
        </Stack>
      )}
    </Stack>
  );
}
