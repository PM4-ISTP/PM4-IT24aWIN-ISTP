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
import {
  IconAlertTriangle,
  IconCheck,
  IconCircleDashed,
  IconClock,
  IconListCheck,
  IconX,
} from "@tabler/icons-react";
import { getDifficultyColor } from "@/src/features/course/constants/challengeConstants";
import PlayLabButton from "@/src/features/course/components/labs/PlayLabButton";
import { getSanitizedHtml } from "@/src/shared/lib/utils";
import type { LabStudentDto } from "@/src/shared/types/course";

function formatText(value?: string | number | null): string {
  if (value === undefined || value === null || value === "") return "n/a";
  return String(value);
}

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

function ChallengeRow({
  title,
  solved,
  wrong,
  index,
}: {
  title: string;
  solved: boolean;
  wrong: boolean;
  index: number;
}) {
  const color = wrong ? "red" : solved ? "teal" : "gray";
  const icon = wrong ? <IconX size={12} /> : solved ? <IconCheck size={12} /> : <IconCircleDashed size={12} />;
  const ariaLabel = wrong ? "Completed (wrong answer)" : solved ? "Completed" : "Not completed";
  return (
    <Group gap="xs" align="center" wrap="nowrap">
      <ThemeIcon
        variant="light"
        radius="xl"
        size="sm"
        color={color}
        aria-label={ariaLabel}
      >
        {icon}
      </ThemeIcon>
      <Text
        size="sm"
        c={wrong ? "red.4" : solved ? "teal.3" : undefined}
        td={solved ? "line-through" : undefined}
      >
        {index + 1}. {title}
      </Text>
    </Group>
  );
}

export function CourseLabDetailsList({
  labs,
  title,
  showIndex,
  courseId,
}: {
  labs: LabStudentDto[];
  title: string;
  showIndex: boolean;
  /** If provided, "Play" buttons link into the play view for this course. */
  courseId?: string;
}) {
  if (labs.length === 0) return null;

  const totalSolved = labs.filter((c) => c.isSolved).length;

  return (
    <Stack gap="md">
      <Group justify="space-between" align="center">
        {title ? <Title order={3}>{title}</Title> : <span />}
        <Group gap="xs">
          <IconListCheck size={16} color="rgba(255,255,255,0.5)" />
          <Text size="sm" c="dimmed">
            {totalSolved}/{labs.length} completed
          </Text>
        </Group>
      </Group>

      <Accordion variant="separated" radius="md" multiple>
        {labs.map((lab, index) => {
          if (lab.id === undefined) return null;
          const labTitle = showIndex
            ? `#${index + 1} ${formatText(lab.title)}`
            : formatText(lab.title);
          const sanitizedDescription = lab.description ? getSanitizedHtml(lab.description) : "";
          const challenges = lab.challenges ?? [];
          const solvedCount = lab.solvedChallengeCount ?? 0;
          const totalCount = lab.totalChallengeCount ?? challenges.length;
          const percent = totalCount === 0 ? 0 : Math.round((solvedCount / totalCount) * 100);
          const playHref = courseId
            ? `/dashboard/courses/${courseId}/labs/${lab.id}/play`
            : undefined;

          const now = new Date();
          const dueDate = lab.dueAt ? new Date(lab.dueAt) : null;
          const deadlinePassed = dueDate ? now > dueDate : false;
          const deadlineExpiredUnsolved = deadlinePassed && !lab.isSolved;
          const deadlineExpiredSolved = deadlinePassed && lab.isSolved;

          return (
            <Accordion.Item
              key={lab.id}
              value={lab.id}
              style={{
                background: deadlineExpiredUnsolved
                  ? "rgba(239,68,68,0.06)"
                  : "rgba(255,255,255,0.02)",
                borderLeft: deadlineExpiredUnsolved
                  ? "3px solid rgba(239,68,68,0.7)"
                  : deadlineExpiredSolved
                    ? "3px solid rgba(20,184,166,0.5)"
                    : undefined,
              }}
            >
              <Accordion.Control>
                <Group justify="space-between" align="center" wrap="nowrap" pr="md">
                  <Group gap="sm" align="center" wrap="nowrap" style={{ minWidth: 0 }}>
                    {lab.isSolved ? (
                      <ThemeIcon
                        color="teal"
                        variant="light"
                        radius="xl"
                        size="md"
                        aria-label="Lab solved"
                      >
                        <IconCheck size={16} />
                      </ThemeIcon>
                    ) : (
                      <ThemeIcon
                        color="gray"
                        variant="light"
                        radius="xl"
                        size="md"
                        aria-label="Lab not solved"
                      >
                        <IconCircleDashed size={16} />
                      </ThemeIcon>
                    )}
                    <Text fw={600} truncate>
                      {labTitle}
                    </Text>
                  </Group>
                  <Group gap="xs" wrap="nowrap" align="center" style={{ flexShrink: 0 }}>
                    {lab.dueAt ? (
                      deadlineExpiredUnsolved ? (
                        <Group
                          gap={5}
                          wrap="nowrap"
                          style={{
                            background: "rgba(239,68,68,0.15)",
                            border: "1px solid rgba(239,68,68,0.4)",
                            borderRadius: 8,
                            padding: "4px 10px",
                          }}
                        >
                          <IconAlertTriangle size={15} color="#f87171" style={{ flexShrink: 0 }} />
                          <Text
                            size="sm"
                            fw={700}
                            style={{ color: "#f87171", whiteSpace: "nowrap" }}
                          >
                            Expired · {formatDue(lab.dueAt)}
                          </Text>
                        </Group>
                      ) : deadlineExpiredSolved ? (
                        <Group
                          gap={5}
                          wrap="nowrap"
                          style={{
                            background: "rgba(20,184,166,0.1)",
                            border: "1px solid rgba(20,184,166,0.3)",
                            borderRadius: 8,
                            padding: "4px 10px",
                          }}
                        >
                          <IconClock
                            size={15}
                            color="rgba(20,184,166,0.9)"
                            style={{ flexShrink: 0 }}
                          />
                          <Text
                            size="sm"
                            fw={600}
                            style={{ color: "rgba(20,184,166,0.9)", whiteSpace: "nowrap" }}
                          >
                            Due: {formatDue(lab.dueAt)}
                          </Text>
                        </Group>
                      ) : (
                        <Group
                          gap={5}
                          wrap="nowrap"
                          style={{
                            background: "rgba(255,255,255,0.05)",
                            border: "1px solid rgba(255,255,255,0.12)",
                            borderRadius: 8,
                            padding: "4px 10px",
                          }}
                        >
                          <IconClock
                            size={15}
                            color="rgba(255,255,255,0.6)"
                            style={{ flexShrink: 0 }}
                          />
                          <Text
                            size="sm"
                            fw={600}
                            style={{ color: "rgba(255,255,255,0.7)", whiteSpace: "nowrap" }}
                          >
                            Due: {formatDue(lab.dueAt)}
                          </Text>
                        </Group>
                      )
                    ) : null}
                    <Badge variant="light" color={getDifficultyColor(lab.difficulty ?? "")}>
                      {formatText(lab.difficulty)}
                    </Badge>
                    <Badge
                      variant="light"
                      color={lab.isSolved ? "teal" : "blue"}
                      aria-label={`${solvedCount} of ${totalCount} challenges solved`}
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
                      color={lab.isSolved ? "teal" : "blue"}
                      radius="xl"
                      size="sm"
                    />
                  </Stack>

                  {sanitizedDescription && (
                    <Box
                      className="course-description"
                      style={{ fontSize: "var(--mantine-font-size-sm)" }}
                      dangerouslySetInnerHTML={{ __html: sanitizedDescription }}
                    />
                  )}

                  {challenges.length > 0 && (
                    <>
                      <Divider my={4} />
                      <Stack gap={6}>
                        <Text size="xs" c="dimmed" tt="uppercase" fw={700}>
                          Challenges
                        </Text>
                        {challenges.map((st, idx) => (
                          <ChallengeRow
                            key={st.id ?? idx}
                            title={st.title ?? ""}
                            solved={st.isSolved ?? false}
                            wrong={Boolean(st.isSolved && st.correctOptionId)}
                            index={idx}
                          />
                        ))}
                      </Stack>
                    </>
                  )}

                  {playHref && (
                    <Group justify="flex-end">
                      <PlayLabButton
                        href={playHref}
                        solved={lab.isSolved ?? false}
                        inProgress={solvedCount > 0 && !lab.isSolved}
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
