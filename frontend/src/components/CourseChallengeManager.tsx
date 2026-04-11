"use client";

import { useState } from "react";
import Link from "next/link";
import sanitizeHtml from "sanitize-html";
import {
  ActionIcon,
  Badge,
  Box,
  Button,
  Collapse,
  Group,
  Loader,
  Stack,
  Text,
  Title,
} from "@mantine/core";
import {
  IconArrowDown,
  IconArrowUp,
  IconChevronDown,
  IconChevronRight,
  IconPlus,
  IconTrash,
} from "@tabler/icons-react";
import { ChallengeSearchSelect } from "@/src/components/ChallengeSearchSelect";
import {
  fetchChallenge,
  type ChallengeDetailResponseDto,
  type ListChallengeResponseDto,
} from "@/src/lib/actions/challenges";
import { getDifficultyColor, getStatusColor } from "@/src/lib/challengeConstants";

export interface CourseChallengeEntry {
  challengeId: string;
  challengeTitle: string;
  difficulty: string;
  orderIndex: number;
}

interface CourseChallengeManagerProps {
  challenges: CourseChallengeEntry[];
  onChange: (challenges: CourseChallengeEntry[]) => void;
}

export function CourseChallengeManager({ challenges, onChange }: CourseChallengeManagerProps) {
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [detailsCache, setDetailsCache] = useState<Record<string, ChallengeDetailResponseDto>>({});
  const [loadingDetailId, setLoadingDetailId] = useState<string | null>(null);

  function handleAddChallenge(challenge: ListChallengeResponseDto) {
    if (challenges.some((c) => c.challengeId === challenge.id)) return;

    const newEntry: CourseChallengeEntry = {
      challengeId: challenge.id,
      challengeTitle: challenge.title,
      difficulty: challenge.difficulty,
      orderIndex: challenges.length,
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
        <Stack gap="xs">
          {challenges.map((challenge, index) => {
            const isExpanded = expandedId === challenge.challengeId;
            const detail = detailsCache[challenge.challengeId];
            const isLoadingDetail = loadingDetailId === challenge.challengeId;

            return (
              <Box
                key={challenge.challengeId}
                style={{
                  border: "1px solid var(--mantine-color-default-border)",
                  borderRadius: "var(--mantine-radius-sm)",
                  overflow: "hidden",
                }}
              >
                <Group
                  p="xs"
                  justify="space-between"
                  wrap="nowrap"
                  style={{ cursor: "pointer" }}
                  onClick={() => void handleToggleExpand(challenge.challengeId)}
                >
                  <Group gap="sm" wrap="nowrap" style={{ flex: 1, minWidth: 0 }}>
                    <ActionIcon variant="transparent" size="xs" tabIndex={-1}>
                      {isExpanded ? <IconChevronDown size={14} /> : <IconChevronRight size={14} />}
                    </ActionIcon>
                    <Text size="sm" c="dimmed" w={24} ta="center">
                      {index + 1}
                    </Text>
                    <Text size="sm" truncate style={{ flex: 1 }}>
                      {challenge.challengeTitle}
                    </Text>
                    <Badge
                      size="xs"
                      variant="light"
                      color={getDifficultyColor(challenge.difficulty)}
                    >
                      {challenge.difficulty}
                    </Badge>
                  </Group>
                  <Group gap={4} wrap="nowrap" onClick={(e) => e.stopPropagation()}>
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

                <Collapse expanded={isExpanded}>
                  <Box
                    p="sm"
                    style={{
                      borderTop: "1px solid var(--mantine-color-default-border)",
                      background: "rgba(255,255,255,0.02)",
                    }}
                  >
                    {isLoadingDetail && !detail ? (
                      <Group justify="center" p="xs">
                        <Loader size="sm" />
                      </Group>
                    ) : detail ? (
                      <Stack gap="xs">
                        <Group gap="xs">
                          <Badge size="xs" variant="light" color={getStatusColor(detail.status)}>
                            {detail.status}
                          </Badge>
                          <Badge
                            size="xs"
                            variant="light"
                            color={getDifficultyColor(detail.difficulty)}
                          >
                            {detail.difficulty}
                          </Badge>
                          <Text size="xs" c="dimmed">
                            by {detail.creator.name}
                          </Text>
                        </Group>
                        <Text size="xs" c="dimmed">
                          Max Score: {detail.maxScore} | Updated:{" "}
                          {new Date(detail.updatedAt).toLocaleDateString()}
                        </Text>
                        {detail.description && (
                          <Box
                            style={{ fontSize: "var(--mantine-font-size-sm)" }}
                            dangerouslySetInnerHTML={{
                              __html: sanitizeHtml(detail.description),
                            }}
                          />
                        )}
                      </Stack>
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
