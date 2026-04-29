"use client";

import {
  ActionIcon,
  Badge,
  Box,
  Button,
  Divider,
  Group,
  Paper,
  Progress,
  Stack,
  Stepper,
  Text,
  TextInput,
  Title,
  Tooltip,
} from "@mantine/core";
import { notifications } from "@mantine/notifications";
import {
  IconArrowLeft,
  IconArrowRight,
  IconCheck,
  IconExternalLink,
  IconLock,
  IconMaximize,
  IconMinimize,
  IconTrophy,
} from "@tabler/icons-react";
import Link from "next/link";
import { useMemo, useState } from "react";
import { submitSubTaskFlag } from "@/src/features/course/actions/challenges";
import {
  getDifficultyColor,
  getStatusColor,
} from "@/src/features/course/constants/challengeConstants";
import { getSanitizedHtml } from "@/src/shared/lib/utils";
import type { ChallengeStudentDto, SubTaskStudentDto } from "@/src/shared/types/course";

const FLAG_PATTERN = /^ISTP\{[A-Za-z0-9_]+\}$/;
const YOUTUBE_EMBED_URL = "https://www.youtube.com/embed/QDqPXFgLirM?list=RDQDqPXFgLirM";
const YOUTUBE_WATCH_URL = "https://www.youtube.com/watch?v=QDqPXFgLirM&list=RDQDqPXFgLirM";

function pickInitialStep(subTasks: SubTaskStudentDto[]): number {
  const firstUnsolved = subTasks.findIndex((st) => !st.isSolved);
  return firstUnsolved === -1 ? Math.max(subTasks.length - 1, 0) : firstUnsolved;
}

export function ChallengePlayView({
  courseId,
  initialChallenge,
}: {
  courseId: string;
  initialChallenge: ChallengeStudentDto;
}) {
  const [challenge, setChallenge] = useState<ChallengeStudentDto>(initialChallenge);
  const [activeStep, setActiveStep] = useState<number>(() =>
    pickInitialStep(initialChallenge.subTasks ?? [])
  );
  const [flagInput, setFlagInput] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [labFullscreen, setLabFullscreen] = useState(false);

  const subTasks = challenge.subTasks ?? [];
  const total = subTasks.length;
  const solvedCount = challenge.solvedSubTaskCount ?? 0;
  const percent = total === 0 ? 0 : Math.round((solvedCount / total) * 100);
  const current = subTasks[activeStep] ?? null;
  const allSolved = challenge.isSolved ?? false;
  const sanitizedChallengeDescription = useMemo(
    () => (challenge.description ? getSanitizedHtml(challenge.description) : ""),
    [challenge.description]
  );
  const sanitizedSubTaskDescription = useMemo(
    () => (current?.description ? getSanitizedHtml(current.description) : ""),
    [current]
  );

  function updateSubTaskSolved(subTaskId: string, submittedFlag: string) {
    setChallenge((prev) => {
      const updatedSubTasks = (prev.subTasks ?? []).map((st) =>
        st.id === subTaskId ? { ...st, isSolved: true, solvedFlag: submittedFlag } : st
      );
      const newSolved = updatedSubTasks.filter((st) => st.isSolved).length;
      return {
        ...prev,
        subTasks: updatedSubTasks,
        solvedSubTaskCount: newSolved,
        isSolved: newSolved === updatedSubTasks.length && updatedSubTasks.length > 0,
      };
    });
  }

  async function handleSubmit() {
    if (!current || !current.id || !challenge.id) return;
    if (!FLAG_PATTERN.test(flagInput)) {
      notifications.show({
        color: "red",
        title: "Invalid flag format",
        message: "Flags must match ISTP{...} (letters, digits, underscores).",
      });
      return;
    }

    setSubmitting(true);
    const result = await submitSubTaskFlag(challenge.id, current.id, flagInput);
    setSubmitting(false);

    if (!result.success) {
      notifications.show({
        color: "red",
        title: "Submission failed",
        message: result.error,
      });
      return;
    }

    if (result.data.isCorrect && current.id) {
      updateSubTaskSolved(current.id, flagInput);
      notifications.show({
        color: "teal",
        title: "Correct flag!",
        message: result.data.isChallengeSolved
          ? "Challenge completed. Nice work!"
          : "Sub-task solved. Continue to the next one.",
      });
    } else {
      notifications.show({
        color: "red",
        title: "Incorrect flag",
        message: "Not quite — double-check and try again.",
      });
    }
  }

  function goToStep(step: number) {
    if (step < 0 || step >= total) return;
    // Allow navigating only to solved steps or the first unsolved one.
    const firstUnsolved = subTasks.findIndex((st) => !st.isSolved);
    const maxReachable = firstUnsolved === -1 ? total - 1 : firstUnsolved;
    if (step > maxReachable) return;
    setActiveStep(step);
    setFlagInput("");
  }

  return (
    <Box style={{ display: "flex", flexDirection: "column", minHeight: "calc(100vh - 60px)" }}>
      {/* Header */}
      <Group justify="space-between" align="center" px="lg" py="md">
        <Link href={`/dashboard/courses/${courseId}`} style={{ textDecoration: "none" }}>
          <Group gap={6} style={{ color: "rgba(255,255,255,0.6)" }}>
            <IconArrowLeft size={16} />
            <Text size="sm">Back to course</Text>
          </Group>
        </Link>
        <Group gap="xs">
          <Badge variant="light" color={getStatusColor(challenge.status ?? "")}>
            {challenge.status}
          </Badge>
          <Badge variant="light" color={getDifficultyColor(challenge.difficulty ?? "")}>
            {challenge.difficulty}
          </Badge>
          {allSolved && (
            <Badge variant="light" color="teal" leftSection={<IconTrophy size={12} />}>
              Completed
            </Badge>
          )}
        </Group>
      </Group>

      <Box
        style={{
          display: "grid",
          gridTemplateColumns: labFullscreen ? "1fr" : "minmax(0, 1fr) minmax(0, 1fr)",
          gap: "1rem",
          padding: "0 1rem 1rem",
          flex: 1,
          minHeight: 0,
        }}
      >
        {/* LEFT: Challenge description + sub-task stepper */}
        <Paper
          withBorder
          radius="md"
          p="lg"
          style={{ background: "rgba(255,255,255,0.02)", overflow: "auto" }}
        >
          <Stack gap="lg">
            {/* ── Challenge block ── */}
            <Stack gap="md">
              <Stack gap={6}>
                <Text
                  size="xs"
                  tt="uppercase"
                  c="dimmed"
                  fw={700}
                  style={{ letterSpacing: "0.08em" }}
                >
                  Challenge
                </Text>
                <Title order={2} style={{ lineHeight: 1.2 }}>
                  {challenge.title}
                </Title>
                {challenge.shortDescription && (
                  <Text c="dimmed" size="sm">
                    {challenge.shortDescription}
                  </Text>
                )}
              </Stack>

              <Stack gap={6}>
                <Group justify="space-between" align="center">
                  <Text size="xs" tt="uppercase" c="dimmed" fw={700}>
                    Progress
                  </Text>
                  <Text size="xs" fw={600} c={allSolved ? "teal.3" : "blue.3"}>
                    {solvedCount} / {total} sub-tasks
                  </Text>
                </Group>
                <Progress
                  value={percent}
                  color={allSolved ? "teal" : "blue"}
                  radius="xl"
                  size="sm"
                />
              </Stack>

              {sanitizedChallengeDescription && (
                <Box
                  className="course-description"
                  style={{ fontSize: "var(--mantine-font-size-sm)" }}
                  dangerouslySetInnerHTML={{ __html: sanitizedChallengeDescription }}
                />
              )}
            </Stack>

            <Divider />

            {/* ── Sub-task navigation: compact, just numbers ── */}
            {total > 0 && (
              <Stepper
                active={activeStep}
                onStepClick={goToStep}
                allowNextStepsSelect={false}
                size="xs"
                iconSize={28}
              >
                {subTasks.map((st, idx) => (
                  <Stepper.Step
                    key={st.id}
                    completedIcon={<IconCheck size={14} />}
                    icon={
                      st.isSolved ? (
                        <IconCheck size={14} />
                      ) : idx > 0 && !subTasks[idx - 1]?.isSolved ? (
                        <IconLock size={12} />
                      ) : undefined
                    }
                  />
                ))}
              </Stepper>
            )}

            {/* ── Sub-task working area ── */}
            {current && (
              <Paper
                withBorder
                radius="md"
                p="md"
                style={{ background: "rgba(255,255,255,0.03)" }}
              >
                <Stack gap="md">
                  <Stack gap={4}>
                    <Group gap="xs" align="center">
                      <Text size="xs" tt="uppercase" c="dimmed" fw={700}>
                        Sub-task {activeStep + 1} of {total}
                      </Text>
                      {current.isSolved && (
                        <Badge variant="light" color="teal" size="xs">
                          Solved
                        </Badge>
                      )}
                    </Group>
                    <Title order={4} style={{ lineHeight: 1.3 }}>
                      {current.title}
                    </Title>
                  </Stack>

                  {sanitizedSubTaskDescription && (
                    <Box
                      className="course-description"
                      style={{ fontSize: "var(--mantine-font-size-sm)" }}
                      dangerouslySetInnerHTML={{ __html: sanitizedSubTaskDescription }}
                    />
                  )}

                  <Stack gap="xs">
                    <Text size="xs" tt="uppercase" c="dimmed" fw={700}>
                      Submit Flag
                    </Text>
                    <Group gap="xs" align="flex-end">
                      <TextInput
                        value={current.isSolved ? (current.solvedFlag ?? "") : flagInput}
                        onChange={(e) => {
                          if (!current.isSolved) setFlagInput(e.currentTarget.value);
                        }}
                        placeholder="ISTP{...}"
                        readOnly={current.isSolved}
                        disabled={submitting}
                        style={{ flex: 1 }}
                        styles={{ input: { fontFamily: "var(--font-geist-mono), monospace" } }}
                        onKeyDown={(e) => {
                          if (e.key === "Enter" && !current.isSolved && !submitting) {
                            e.preventDefault();
                            void handleSubmit();
                          }
                        }}
                        aria-label="Flag input"
                      />
                      <Button
                        onClick={() => void handleSubmit()}
                        disabled={current.isSolved}
                        loading={submitting}
                        color={current.isSolved ? "teal" : "blue"}
                        leftSection={current.isSolved ? <IconCheck size={16} /> : undefined}
                      >
                        {current.isSolved ? "Solved" : "Submit"}
                      </Button>
                    </Group>
                  </Stack>

                  <Group justify="space-between">
                    <Button
                      variant="subtle"
                      leftSection={<IconArrowLeft size={16} />}
                      onClick={() => goToStep(activeStep - 1)}
                      disabled={activeStep === 0}
                    >
                      Previous
                    </Button>
                    <Button
                      rightSection={<IconArrowRight size={16} />}
                      onClick={() => goToStep(activeStep + 1)}
                      disabled={!current.isSolved || activeStep >= total - 1}
                    >
                      Next
                    </Button>
                  </Group>
                </Stack>
              </Paper>
            )}

            {allSolved && (
              <Paper
                withBorder
                radius="md"
                p="md"
                style={{ background: "rgba(20, 184, 166, 0.08)" }}
              >
                <Group gap="sm">
                  <IconTrophy size={20} color="var(--mantine-color-teal-4)" />
                  <Stack gap={2}>
                    <Text fw={600}>Challenge completed</Text>
                    <Text size="sm" c="dimmed">
                      You solved every sub-task. Well done!
                    </Text>
                  </Stack>
                </Group>
              </Paper>
            )}
          </Stack>
        </Paper>

        {/* RIGHT: Lab environment (placeholder iframe) */}
        {!labFullscreen && (
          <Paper
            withBorder
            radius="md"
            p={0}
            style={{
              background: "rgba(255,255,255,0.02)",
              display: "flex",
              flexDirection: "column",
              minHeight: 420,
              overflow: "hidden",
            }}
          >
            <Group
              justify="space-between"
              align="center"
              px="md"
              py="xs"
              style={{ borderBottom: "1px solid rgba(255,255,255,0.08)" }}
            >
              <Group gap="xs">
                <Text size="sm" fw={600}>
                  Lab Environment
                </Text>
                <Badge size="xs" variant="light" color="yellow">
                  Coming soon
                </Badge>
              </Group>
              <Group gap={4}>
                <Tooltip label="Open lab in new tab">
                  <ActionIcon
                    component="a"
                    href={YOUTUBE_WATCH_URL}
                    target="_blank"
                    rel="noreferrer"
                    variant="subtle"
                    aria-label="Open lab in new tab"
                  >
                    <IconExternalLink size={16} />
                  </ActionIcon>
                </Tooltip>
                <Tooltip label="Fullscreen description">
                  <ActionIcon
                    variant="subtle"
                    onClick={() => setLabFullscreen(true)}
                    aria-label="Toggle fullscreen"
                  >
                    <IconMaximize size={16} />
                  </ActionIcon>
                </Tooltip>
              </Group>
            </Group>
            <Box style={{ position: "relative", flex: 1 }}>
              <iframe
                src={YOUTUBE_EMBED_URL}
                title="Lab environment placeholder"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                style={{
                  position: "absolute",
                  inset: 0,
                  width: "100%",
                  height: "100%",
                  border: 0,
                }}
              />
            </Box>
          </Paper>
        )}
      </Box>

      {labFullscreen && (
        <Group justify="flex-end" px="lg" pb="md">
          <Button
            variant="subtle"
            leftSection={<IconExternalLink size={16} />}
            component="a"
            href={YOUTUBE_WATCH_URL}
            target="_blank"
            rel="noreferrer"
          >
            Open lab in new tab
          </Button>
          <Button
            variant="light"
            leftSection={<IconMinimize size={16} />}
            onClick={() => setLabFullscreen(false)}
          >
            Show lab pane
          </Button>
        </Group>
      )}
    </Box>
  );
}
