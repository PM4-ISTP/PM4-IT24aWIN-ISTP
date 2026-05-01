"use client";

import {
  Accordion,
  Badge,
  Box,
  Divider,
  Group,
  Progress,
  Stack,
  Text,
  ThemeIcon,
  Title,
} from "@mantine/core";
import { IconCheck, IconCircleDashed, IconListCheck } from "@tabler/icons-react";
import {
  getDifficultyColor,
  getStatusColor,
} from "@/src/features/course/constants/challengeConstants";
import PlayChallengeButton from "@/src/features/course/components/challenges/PlayChallengeButton";
import { getSanitizedHtml } from "@/src/shared/lib/utils";
import type { ChallengeStudentDto } from "@/src/shared/types/course";

function formatText(value?: string | number | null): string {
  if (value === undefined || value === null || value === "") return "n/a";
  return String(value);
}

function SubTaskRow({ title, solved, index }: { title: string; solved: boolean; index: number }) {
  return (
    <Group gap="xs" align="center" wrap="nowrap">
      <ThemeIcon
        variant="light"
        radius="xl"
        size="sm"
        color={solved ? "teal" : "gray"}
        aria-label={solved ? "Solved" : "Not solved"}
      >
        {solved ? <IconCheck size={12} /> : <IconCircleDashed size={12} />}
      </ThemeIcon>
      <Text size="sm" c={solved ? "teal.3" : undefined} td={solved ? "line-through" : undefined}>
        {index + 1}. {title}
      </Text>
    </Group>
  );
}

export function CourseChallengeDetailsList({
  challenges,
  title,
  showIndex,
  courseId,
}: {
  challenges: ChallengeStudentDto[];
  title: string;
  showIndex: boolean;
  /** If provided, "Play" buttons link into the play view for this course. */
  courseId?: string;
}) {
  if (challenges.length === 0) return null;

  const totalSolved = challenges.filter((c) => c.isSolved).length;

  return (
    <Stack gap="md">
      <Group justify="space-between" align="center">
        {title ? <Title order={3}>{title}</Title> : <span />}
        <Group gap="xs">
          <IconListCheck size={16} color="rgba(255,255,255,0.5)" />
          <Text size="sm" c="dimmed">
            {totalSolved}/{challenges.length} completed
          </Text>
        </Group>
      </Group>

      <Accordion variant="separated" radius="md" multiple>
        {challenges.map((challenge, index) => {
          if (challenge.id === undefined) return null;
          const challengeTitle = showIndex
            ? `#${index + 1} ${formatText(challenge.title)}`
            : formatText(challenge.title);
          const sanitizedDescription = challenge.description
            ? getSanitizedHtml(challenge.description)
            : "";
          const subTasks = challenge.subTasks ?? [];
          const solvedCount = challenge.solvedSubTaskCount ?? 0;
          const totalCount = challenge.totalSubTaskCount ?? subTasks.length;
          const percent = totalCount === 0 ? 0 : Math.round((solvedCount / totalCount) * 100);
          const playHref = courseId
            ? `/dashboard/courses/${courseId}/challenges/${challenge.id}/play`
            : undefined;

          return (
            <Accordion.Item
              key={challenge.id}
              value={challenge.id}
              style={{ background: "rgba(255,255,255,0.02)" }}
            >
              <Accordion.Control>
                <Group justify="space-between" align="center" wrap="nowrap" pr="md">
                  <Group gap="sm" align="center" wrap="nowrap" style={{ minWidth: 0 }}>
                    {challenge.isSolved ? (
                      <ThemeIcon
                        color="teal"
                        variant="light"
                        radius="xl"
                        size="md"
                        aria-label="Challenge solved"
                      >
                        <IconCheck size={16} />
                      </ThemeIcon>
                    ) : (
                      <ThemeIcon
                        color="gray"
                        variant="light"
                        radius="xl"
                        size="md"
                        aria-label="Challenge not solved"
                      >
                        <IconCircleDashed size={16} />
                      </ThemeIcon>
                    )}
                    <Text fw={600} truncate>
                      {challengeTitle}
                    </Text>
                  </Group>
                  <Group gap="xs" wrap="nowrap">
                    <Badge variant="light" color={getStatusColor(challenge.status ?? "")}>
                      {formatText(challenge.status)}
                    </Badge>
                    <Badge variant="light" color={getDifficultyColor(challenge.difficulty ?? "")}>
                      {formatText(challenge.difficulty)}
                    </Badge>
                    <Badge
                      variant="light"
                      color={challenge.isSolved ? "teal" : "blue"}
                      aria-label={`${solvedCount} of ${totalCount} sub-tasks solved`}
                    >
                      {solvedCount}/{totalCount}
                    </Badge>
                  </Group>
                </Group>
              </Accordion.Control>
              <Accordion.Panel>
                <Stack gap="md">
                  <Stack gap={4}>
                    <Text size="xs" c="dimmed" tt="uppercase" fw={700}>
                      Progress
                    </Text>
                    <Progress
                      value={percent}
                      color={challenge.isSolved ? "teal" : "blue"}
                      radius="xl"
                      size="sm"
                    />
                  </Stack>

                  {challenge.shortDescription && (
                    <Text size="sm" c="dimmed">
                      {challenge.shortDescription}
                    </Text>
                  )}

                  {sanitizedDescription && (
                    <Box
                      className="course-description"
                      style={{ fontSize: "var(--mantine-font-size-sm)" }}
                      dangerouslySetInnerHTML={{ __html: sanitizedDescription }}
                    />
                  )}

                  {subTasks.length > 0 && (
                    <>
                      <Divider my={4} />
                      <Stack gap={6}>
                        <Text size="xs" c="dimmed" tt="uppercase" fw={700}>
                          Sub-tasks
                        </Text>
                        {subTasks.map((st, idx) => (
                          <SubTaskRow
                            key={st.id ?? idx}
                            title={st.title ?? ""}
                            solved={st.isSolved ?? false}
                            index={idx}
                          />
                        ))}
                      </Stack>
                    </>
                  )}

                  {playHref && (
                    <Group justify="flex-end">
                      <PlayChallengeButton
                        href={playHref}
                        solved={challenge.isSolved ?? false}
                        inProgress={solvedCount > 0 && !challenge.isSolved}
                      />
                    </Group>
                  )}
                </Stack>
              </Accordion.Panel>
            </Accordion.Item>
          );
        })}
      </Accordion>
    </Stack>
  );
}
