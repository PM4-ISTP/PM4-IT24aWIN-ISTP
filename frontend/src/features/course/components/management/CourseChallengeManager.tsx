"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import sanitizeHtml from "sanitize-html";
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
import { ChallengeSearchSelect } from "@/src/features/course/components/challenges/ChallengeSearchSelect";
import {
  fetchChallenge,
  type ChallengeDetailResponseDto,
  type ListChallengeResponseDto,
} from "@/src/features/course/actions/challenges";
import {
  getDifficultyColor,
  getStatusColor,
} from "@/src/features/course/constants/challengeConstants";

export interface CourseChallengeEntry {
  challengeId: string;
  challengeTitle: string;
  difficulty: string;
  orderIndex: number;
  shortDescription?: string;
  creatorName?: string;
  creatorId?: string;
  status?: string;
}

interface CourseChallengeManagerProps {
  challenges: CourseChallengeEntry[];
  onChange: (challenges: CourseChallengeEntry[]) => void;
}

const RICH_TEXT_SANITIZE_OPTIONS: sanitizeHtml.IOptions = {
  allowedTags: sanitizeHtml.defaults.allowedTags.concat(["img", "h1", "h2"]),
  allowedAttributes: {
    ...sanitizeHtml.defaults.allowedAttributes,
    img: ["src", "alt", "width", "height"],
  },
};

type SubTaskDetail = NonNullable<ChallengeDetailResponseDto["subTasks"]>[number];

function SubTaskListView({ subTasks }: { subTasks: SubTaskDetail[] }) {
  const [expandedKey, setExpandedKey] = useState<string | null>(null);

  const sorted = [...subTasks].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0));

  return (
    <Stack gap="xs">
      <Group gap="sm" align="center">
        <IconListCheck size={16} color="#60a5fa" />
        <Text size="sm" fw={600} style={{ color: "#f1f5f9" }}>
          Sub Tasks ({sorted.length})
        </Text>
      </Group>

      <Stack gap="xs">
        {sorted.map((st, i) => {
          const key = st.id ?? `local-${i}`;
          const isExpanded = expandedKey === key;
          const description = st.description
            ? sanitizeHtml(st.description, RICH_TEXT_SANITIZE_OPTIONS)
            : null;
          const flag = st.flag?.trim();
          const hasFlag = Boolean(flag);
          const title = st.title?.trim() || `Sub Task ${i + 1}`;

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
  const sanitizedDescription = detail.description
    ? sanitizeHtml(detail.description, RICH_TEXT_SANITIZE_OPTIONS)
    : null;
  const subTasks = detail.subTasks ?? [];

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

      {/* Sub Tasks */}
      {subTasks.length > 0 && (
        <>
          <Divider style={{ borderColor: "rgba(255,255,255,0.08)" }} />
          <SubTaskListView subTasks={subTasks} />
        </>
      )}
    </Stack>
  );
}

export function CourseChallengeManager({ challenges, onChange }: CourseChallengeManagerProps) {
  const { data: session } = useSession();
  const currentUserId = (session as { userId?: string } | null)?.userId;

  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [detailsCache, setDetailsCache] = useState<Record<string, ChallengeDetailResponseDto>>({});
  const [loadingDetailId, setLoadingDetailId] = useState<string | null>(null);

  function handleAddChallenge(challenge: ListChallengeResponseDto) {
    if (!challenge.id) return;
    if (challenges.some((c) => c.challengeId === challenge.id)) return;

    const newEntry: CourseChallengeEntry = {
      challengeId: challenge.id,
      challengeTitle: challenge.title ?? "",
      difficulty: challenge.difficulty ?? "MEDIUM",
      orderIndex: challenges.length,
      shortDescription: challenge.shortDescription ?? undefined,
      creatorName: challenge.creatorName ?? undefined,
      status: challenge.status ?? undefined,
    };
    onChange([...challenges, newEntry]);
  }

  function handleRemove(challengeId: string) {
    const updated = challenges
      .filter((c) => c.challengeId !== challengeId)
      .map((c, i) => ({ ...c, orderIndex: i }));
    onChange(updated);
    if (expandedId === challengeId) setExpandedId(null);
  }

  function handleMoveUp(index: number) {
    if (index === 0) return;
    const updated = [...challenges];
    [updated[index - 1], updated[index]] = [updated[index], updated[index - 1]];
    onChange(updated.map((c, i) => ({ ...c, orderIndex: i })));
  }

  function handleMoveDown(index: number) {
    if (index === challenges.length - 1) return;
    const updated = [...challenges];
    [updated[index], updated[index + 1]] = [updated[index + 1], updated[index]];
    onChange(updated.map((c, i) => ({ ...c, orderIndex: i })));
  }

  /** Hydrate a challenge entry with data from the detail response */
  function hydrateEntry(challengeId: string, detail: ChallengeDetailResponseDto) {
    const idx = challenges.findIndex((c) => c.challengeId === challengeId);
    if (idx === -1) return;
    const entry = challenges[idx];
    if (entry.shortDescription && entry.creatorName && entry.status && entry.creatorId) return;

    const updated = [...challenges];
    updated[idx] = {
      ...entry,
      shortDescription: entry.shortDescription ?? detail.shortDescription ?? undefined,
      creatorName: entry.creatorName ?? detail.creator?.name ?? undefined,
      creatorId: entry.creatorId ?? detail.creator?.id ?? undefined,
      status: entry.status ?? detail.status ?? undefined,
    };
    onChange(updated);
  }

  // Preload details for challenges that are missing metadata (e.g. loaded from course API)
  const preloadedRef = useRef<Set<string>>(new Set());
  useEffect(() => {
    const missing = challenges.filter((c) => !c.status && !preloadedRef.current.has(c.challengeId));
    if (missing.length === 0) return;

    missing.forEach((c) => preloadedRef.current.add(c.challengeId));

    void (async () => {
      const results = await Promise.all(
        missing.map(async (c) => {
          const result = await fetchChallenge(c.challengeId);
          return result.success ? { id: c.challengeId, detail: result.data } : null;
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
      const hydrated = challenges.map((entry) => {
        const detail = fetched.get(entry.challengeId);
        if (!detail) return entry;
        if (entry.shortDescription && entry.creatorName && entry.status && entry.creatorId)
          return entry;
        updated = true;
        return {
          ...entry,
          shortDescription: entry.shortDescription ?? detail.shortDescription ?? undefined,
          creatorName: entry.creatorName ?? detail.creator?.name ?? undefined,
          creatorId: entry.creatorId ?? detail.creator?.id ?? undefined,
          status: entry.status ?? detail.status ?? undefined,
        };
      });
      if (updated) onChange(hydrated);
    })();
  }, [challenges, onChange]);

  async function handleToggleExpand(challengeId: string) {
    if (expandedId === challengeId) {
      setExpandedId(null);
      return;
    }

    setExpandedId(challengeId);

    if (!detailsCache[challengeId]) {
      setLoadingDetailId(challengeId);
      const result = await fetchChallenge(challengeId);
      setLoadingDetailId((prev) => (prev === challengeId ? null : prev));

      if (result.success) {
        setDetailsCache((prev) => ({ ...prev, [challengeId]: result.data }));
        hydrateEntry(challengeId, result.data);
      }
    }
  }

  return (
    <Stack gap="md">
      <Group justify="space-between" align="center">
        <Title order={4}>Course Challenges</Title>
        <Button
          variant="light"
          size="xs"
          leftSection={<IconPlus size={14} />}
          component={Link}
          href="/dashboard/instructor/challenges/create"
          target="_blank"
        >
          New Challenge
        </Button>
      </Group>

      <ChallengeSearchSelect
        excludeIds={challenges.map((c) => c.challengeId)}
        onSelect={handleAddChallenge}
      />

      {challenges.length === 0 ? (
        <Text size="sm" c="dimmed">
          No challenges assigned to this course yet.
        </Text>
      ) : (
        <Stack gap="sm">
          {challenges.map((challenge, index) => {
            const isExpanded = expandedId === challenge.challengeId;
            const detail = detailsCache[challenge.challengeId];
            const isLoadingDetail = loadingDetailId === challenge.challengeId;
            const isOwner =
              (challenge.creatorId != null && challenge.creatorId === currentUserId) ||
              (detail?.creator?.id != null && detail.creator.id === currentUserId);

            return (
              <Box
                key={challenge.challengeId}
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
                  onClick={() => void handleToggleExpand(challenge.challengeId)}
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
                            {challenge.challengeTitle}
                          </Text>
                        </Group>

                        {/* Row 2: Short description */}
                        {challenge.shortDescription && (
                          <Text size="xs" c="dimmed" lineClamp={2}>
                            {challenge.shortDescription}
                          </Text>
                        )}

                        {/* Row 3: Difficulty + Creator */}
                        <Group gap="xs">
                          <Text size="xs" c="dimmed">
                            Difficulty:{" "}
                            <Text
                              span
                              size="xs"
                              fw={600}
                              c={getDifficultyColor(challenge.difficulty)}
                            >
                              {challenge.difficulty}
                            </Text>
                          </Text>
                          {challenge.creatorName && (
                            <>
                              <Text size="xs" c="dimmed">
                                |
                              </Text>
                              <Text size="xs" c="dimmed">
                                by {challenge.creatorName}
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
                      {challenge.status && (
                        <Badge size="xs" variant="light" color={getStatusColor(challenge.status)}>
                          {challenge.status}
                        </Badge>
                      )}
                      {isOwner && (
                        <ActionIcon
                          variant="subtle"
                          size="sm"
                          component={Link}
                          href={`/dashboard/instructor/challenges/${challenge.challengeId}`}
                          aria-label="Edit challenge"
                          title="Edit challenge"
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
                        disabled={index === challenges.length - 1}
                        onClick={() => handleMoveDown(index)}
                        aria-label="Move down"
                      >
                        <IconArrowDown size={14} />
                      </ActionIcon>
                      <ActionIcon
                        variant="subtle"
                        size="sm"
                        color="red"
                        onClick={() => handleRemove(challenge.challengeId)}
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
