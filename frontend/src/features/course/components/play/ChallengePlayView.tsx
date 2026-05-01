"use client";

import {
  ActionIcon,
  Badge,
  Box,
  Button,
  CopyButton,
  Divider,
  Group,
  Paper,
  Progress,
  Stack,
  Stepper,
  Text,
  TextInput,
  ThemeIcon,
  Title,
  Tooltip,
} from "@mantine/core";
import { notifications } from "@mantine/notifications";
import {
  IconArrowLeft,
  IconArrowRight,
  IconCheck,
  IconCopy,
  IconExternalLink,
  IconLock,
  IconPlayerPlay,
  IconPlayerStop,
  IconRefresh,
  IconTerminal2,
  IconTrophy,
  IconWorld,
} from "@tabler/icons-react";
import Link from "next/link";
import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { ChallengePodStatusBadge } from "@/src/features/challenge-pod/components/ChallengePodStatusBadge";
import { useChallengePodStatus } from "@/src/features/challenge-pod/hooks/useChallengePodStatus";
import { submitSubTaskFlag } from "@/src/features/course/actions/challenges";
import {
  DOCKER_IMAGE_ERROR,
  getDifficultyColor,
  getStatusColor,
} from "@/src/features/course/constants/challengeConstants";
import { useDockerImageCheck } from "@/src/features/course/hooks/useDockerImageCheck";
import { useApiClient } from "@/src/shared/lib/api/client";
import { getSanitizedHtml } from "@/src/shared/lib/utils";
import type { ChallengeStudentDto, SubTaskStudentDto } from "@/src/shared/types/course";

const FLAG_WRAPPED_PATTERN = /^ISTP\{[A-Za-z0-9_]+\}$/;
const FLAG_INNER_PATTERN = /^[A-Za-z0-9_]+$/;

function pickInitialStep(subTasks: SubTaskStudentDto[]): number {
  const firstUnsolved = subTasks.findIndex((st) => !st.isSolved);
  return firstUnsolved === -1 ? Math.max(subTasks.length - 1, 0) : firstUnsolved;
}

function formatExpiry(expiresAt?: string | null): string {
  if (!expiresAt) return "";
  const date = new Date(expiresAt);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleString("de-CH", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function CopyIconButton({ value, label }: { value: string; label: string }) {
  return (
    <CopyButton value={value}>
      {({ copied, copy }) => (
        <Tooltip label={copied ? "Copied" : label}>
          <ActionIcon size="xs" variant="subtle" onClick={copy} aria-label={label}>
            {copied ? <IconCheck size={12} /> : <IconCopy size={12} />}
          </ActionIcon>
        </Tooltip>
      )}
    </CopyButton>
  );
}

function ConsoleCredentials({
  password,
  expiresAt,
}: {
  password?: string | null;
  expiresAt: string;
}) {
  if (!password && !expiresAt) return null;

  return (
    <Group justify="space-between" gap="xs">
      {password ? (
        <Group gap={4} wrap="nowrap">
          <Text size="xs" c="dimmed">
            Console login:
          </Text>
          <Text component="span" ff="monospace" size="xs">
            student
          </Text>
          <CopyIconButton value="student" label="Copy console username" />
          <Text size="xs" c="dimmed">
            /
          </Text>
          <Text component="span" ff="monospace" size="xs">
            {password}
          </Text>
          <CopyIconButton value={password} label="Copy console password" />
        </Group>
      ) : (
        <span />
      )}
      {expiresAt && (
        <Text size="xs" c="dimmed">
          Expires: {expiresAt}
        </Text>
      )}
    </Group>
  );
}

function LabLaunchCard({
  title,
  description,
  icon,
  url,
  buttonLabel,
  disabledLabel,
}: {
  title: string;
  description: string;
  icon: ReactNode;
  url?: string | null;
  buttonLabel: string;
  disabledLabel: string;
}) {
  return (
    <Paper
      withBorder
      radius="md"
      p="md"
      style={{
        background: url
          ? "linear-gradient(135deg, rgba(59,130,246,0.14), rgba(20,184,166,0.08))"
          : "rgba(255,255,255,0.03)",
      }}
    >
      <Stack gap="md">
        <Group justify="space-between" align="flex-start" gap="md" wrap="nowrap">
          <Group gap="sm" align="flex-start" wrap="nowrap">
            <ThemeIcon variant="light" radius="md" size={42}>
              {icon}
            </ThemeIcon>
            <Stack gap={3}>
              <Text fw={700}>{title}</Text>
              <Text size="sm" c="dimmed">
                {description}
              </Text>
            </Stack>
          </Group>
          {url ? (
            <Badge variant="light" color="teal">
              Ready
            </Badge>
          ) : (
            <Badge variant="light" color="gray">
              Pending
            </Badge>
          )}
        </Group>
        <Button
          component="a"
          href={url ?? undefined}
          target="_blank"
          rel="noreferrer"
          disabled={!url}
          rightSection={<IconExternalLink size={16} />}
          fullWidth
        >
          {url ? buttonLabel : disabledLabel}
        </Button>
      </Stack>
    </Paper>
  );
}

export function ChallengePlayView({
  courseId,
  challengeId,
  initialChallenge,
}: {
  courseId: string;
  challengeId: string;
  initialChallenge: ChallengeStudentDto;
}) {
  const apiClient = useApiClient();
  const [challenge, setChallenge] = useState<ChallengeStudentDto>(initialChallenge);
  const [activeStep, setActiveStep] = useState<number>(() =>
    pickInitialStep(initialChallenge.subTasks ?? [])
  );
  const [flagInput, setFlagInput] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [podActionLoading, setPodActionLoading] = useState(false);
  const [podActionError, setPodActionError] = useState<string | null>(null);
  const autoStartAttempted = useRef(false);
  const {
    data: pod,
    error: podStatusError,
    loading: podStatusLoading,
    refetch: refetchPodStatus,
  } = useChallengePodStatus(challengeId);
  const dockerImage = challenge.dockerImage ?? "";
  const dockerImageCheck = useDockerImageCheck(dockerImage);

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
  const podStatus = pod?.status ?? "NOT_FOUND";
  const terminalExpiry = formatExpiry(pod?.expiresAt);
  const canStartPod =
    dockerImageCheck.status === "success" ||
    (dockerImageCheck.status === "idle" && !dockerImage.trim());
  const startDisabled = podActionLoading || dockerImageCheck.status === "checking" || !canStartPod;
  let startDisabledReason: string | null = null;
  if (dockerImageCheck.status === "checking") {
    startDisabledReason = "Checking Docker image...";
  } else if (dockerImageCheck.status === "error") {
    startDisabledReason = dockerImageCheck.message ?? "Docker image is not reachable";
  } else if (dockerImage.trim() && dockerImageCheck.status === "idle") {
    startDisabledReason = DOCKER_IMAGE_ERROR;
  }

  const handleStartPod = useCallback(async () => {
    if (!canStartPod) {
      setPodActionError(startDisabledReason ?? "Lab cannot be started with this Docker image.");
      return;
    }
    setPodActionLoading(true);
    setPodActionError(null);
    try {
      await apiClient.POST("/api/v1/challenge-pods/{challengeId}", {
        params: { path: { challengeId } },
      });
      await refetchPodStatus();
    } catch (e) {
      setPodActionError(e instanceof Error ? e.message : "Failed to start lab.");
    } finally {
      setPodActionLoading(false);
    }
  }, [apiClient, canStartPod, challengeId, refetchPodStatus, startDisabledReason]);

  const handleStopPod = useCallback(async () => {
    setPodActionLoading(true);
    setPodActionError(null);
    try {
      await apiClient.DELETE("/api/v1/challenge-pods/{challengeId}", {
        params: { path: { challengeId } },
      });
      await refetchPodStatus();
    } catch (e) {
      setPodActionError(e instanceof Error ? e.message : "Failed to stop lab.");
    } finally {
      setPodActionLoading(false);
    }
  }, [apiClient, challengeId, refetchPodStatus]);

  useEffect(() => {
    if (podStatusLoading || autoStartAttempted.current) return;
    if (podStatus !== "NOT_FOUND" && podStatus !== "FAILED") return;
    if (dockerImageCheck.status === "checking") return;
    autoStartAttempted.current = true;
    void handleStartPod();
  }, [dockerImageCheck.status, handleStartPod, podStatus, podStatusLoading]);

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
    const trimmedFlag = flagInput.trim();
    const normalizedFlag = FLAG_WRAPPED_PATTERN.test(trimmedFlag)
      ? trimmedFlag
      : FLAG_INNER_PATTERN.test(trimmedFlag)
        ? `ISTP{${trimmedFlag}}`
        : null;

    if (!normalizedFlag) {
      notifications.show({
        color: "red",
        title: "Invalid flag format",
        message: "Flags must match ISTP{...} (letters, digits, underscores).",
      });
      return;
    }

    setSubmitting(true);
    const result = await submitSubTaskFlag(challenge.id, current.id, normalizedFlag);
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
      updateSubTaskSolved(current.id, normalizedFlag);
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
    <Box
      style={{
        display: "flex",
        flexDirection: "column",
        height: "calc(100vh - 60px - var(--app-shell-padding) * 2)",
        minHeight: 0,
        overflow: "hidden",
      }}
    >
      <Group justify="space-between" align="center" px="lg" pb="md" style={{ flexShrink: 0 }}>
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
          gridTemplateColumns: "minmax(0, 1.12fr) minmax(360px, 0.88fr)",
          gap: "1rem",
          padding: "0 1rem 1rem",
          flex: 1,
          minHeight: 0,
          overflow: "hidden",
        }}
      >
        <Paper
          withBorder
          radius="md"
          p="lg"
          style={{
            background: "rgba(255,255,255,0.02)",
            overflow: "auto",
            minHeight: 0,
          }}
        >
          <Stack gap="lg">
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

            {current && (
              <Paper withBorder radius="md" p="md" style={{ background: "rgba(255,255,255,0.03)" }}>
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

        <Paper
          withBorder
          radius="md"
          p={0}
          style={{
            background: "rgba(255,255,255,0.02)",
            display: "flex",
            flexDirection: "column",
            minHeight: 0,
            overflow: "hidden",
          }}
        >
          <Group
            justify="space-between"
            align="center"
            px="md"
            py="xs"
            style={{ borderBottom: "1px solid rgba(255,255,255,0.08)", flexShrink: 0 }}
          >
            <Group gap="xs">
              <Text size="sm" fw={600}>
                Lab Environment
              </Text>
              <ChallengePodStatusBadge status={podStatus} />
            </Group>
            <Group gap="xs" wrap="nowrap">
              {podStatus === "RUNNING" || podStatus === "PROVISIONING" ? (
                <Tooltip label="Stop lab">
                  <ActionIcon
                    variant="subtle"
                    color="red"
                    loading={podActionLoading}
                    onClick={() => void handleStopPod()}
                    aria-label="Stop lab"
                  >
                    <IconPlayerStop size={16} />
                  </ActionIcon>
                </Tooltip>
              ) : (
                <Tooltip
                  label={
                    startDisabledReason ?? (podStatus === "FAILED" ? "Retry lab" : "Start lab")
                  }
                >
                  <ActionIcon
                    variant="subtle"
                    color="blue"
                    loading={podActionLoading}
                    disabled={startDisabled}
                    onClick={() => void handleStartPod()}
                    aria-label={podStatus === "FAILED" ? "Retry lab" : "Start lab"}
                  >
                    {podStatus === "FAILED" ? (
                      <IconRefresh size={16} />
                    ) : (
                      <IconPlayerPlay size={16} />
                    )}
                  </ActionIcon>
                </Tooltip>
              )}
            </Group>
          </Group>

          <Stack gap="md" p="lg" style={{ flex: 1, overflow: "auto" }}>
            {startDisabledReason && (
              <Paper
                withBorder
                radius="md"
                p="md"
                style={{ background: "rgba(248,113,113,0.08)" }}
              >
                <Text size="sm" c="red.3">
                  {startDisabledReason}
                </Text>
              </Paper>
            )}

            <LabLaunchCard
              title="Challenge app"
              description={
                podStatus === "RUNNING"
                  ? "Open the running web service in its own browser tab."
                  : "The app link appears once the lab is running."
              }
              icon={<IconWorld size={22} />}
              url={podStatus === "RUNNING" ? pod?.appUrl : null}
              buttonLabel="Open app"
              disabledLabel="App not ready"
            />

            <LabLaunchCard
              title="Console"
              description={
                podStatus === "RUNNING"
                  ? "Open terminal access in a separate browser tab."
                  : "The console link appears once the lab is running."
              }
              icon={<IconTerminal2 size={22} />}
              url={podStatus === "RUNNING" ? pod?.terminalUrl : null}
              buttonLabel="Open console"
              disabledLabel="Console not ready"
            />

            {(podActionError || podStatusError) && (
              <Paper withBorder radius="md" p="md" style={{ background: "rgba(248,113,113,0.08)" }}>
                <Text size="sm" c="red.3">
                  {podActionError ?? podStatusError}
                </Text>
              </Paper>
            )}

            <ConsoleCredentials password={pod?.terminalPassword} expiresAt={terminalExpiry} />
          </Stack>
        </Paper>
      </Box>
    </Box>
  );
}
