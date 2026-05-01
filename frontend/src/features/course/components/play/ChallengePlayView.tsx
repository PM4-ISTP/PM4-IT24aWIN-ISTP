"use client";

import {
  ActionIcon,
  Badge,
  Box,
  Button,
  CopyButton,
  Divider,
  Group,
  Loader,
  Paper,
  Progress,
  SegmentedControl,
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
  IconCopy,
  IconExternalLink,
  IconLock,
  IconMaximize,
  IconMinimize,
  IconPlayerPlay,
  IconPlayerStop,
  IconRefresh,
  IconTerminal2,
  IconTrophy,
  IconWorld,
} from "@tabler/icons-react";
import Link from "next/link";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
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
const SPLIT_STORAGE_KEY = "istp.challengePlay.splitPercent";
const DEFAULT_SPLIT_PERCENT = 48;
const MIN_TASK_PANEL_PX = 360;
const MIN_LAB_PANEL_PX = 420;
type LabViewMode = "app" | "console";

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

function resolveLabUrl(input: string, baseUrl?: string | null): string | null {
  if (!baseUrl) return null;
  const trimmed = input.trim();
  if (!trimmed) return baseUrl;

  try {
    if (/^https?:\/\//i.test(trimmed)) {
      return new URL(trimmed).toString();
    }

    const base = new URL(baseUrl);
    const path = trimmed.startsWith("/") ? trimmed : `/${trimmed}`;
    return new URL(path, base.origin).toString();
  } catch {
    return null;
  }
}

function clampSplitPercent(value: number): number {
  return Math.min(70, Math.max(30, value));
}

function SplitPresetControls({
  onFocusTask,
  onReset,
  onFocusLab,
}: {
  onFocusTask: () => void;
  onReset: () => void;
  onFocusLab: () => void;
}) {
  return (
    <Button.Group>
      <Tooltip label="Focus task">
        <Button size="compact-xs" variant="subtle" onClick={onFocusTask}>
          Task
        </Button>
      </Tooltip>
      <Tooltip label="Balanced split">
        <Button size="compact-xs" variant="subtle" onClick={onReset}>
          50/50
        </Button>
      </Tooltip>
      <Tooltip label="Focus lab">
        <Button size="compact-xs" variant="subtle" onClick={onFocusLab}>
          Lab
        </Button>
      </Tooltip>
    </Button.Group>
  );
}

function SplitResizeHandle({
  isResizing,
  onStartResize,
  onSetSplit,
}: {
  isResizing: boolean;
  onStartResize: () => void;
  onSetSplit: (value: number | ((current: number) => number)) => void;
}) {
  return (
    <Box
      role="separator"
      aria-orientation="vertical"
      aria-label="Resize task and lab panels"
      tabIndex={0}
      onPointerDown={(event) => {
        event.preventDefault();
        onStartResize();
      }}
      onDoubleClick={() => onSetSplit(DEFAULT_SPLIT_PERCENT)}
      onKeyDown={(event) => {
        if (event.key === "ArrowLeft") {
          event.preventDefault();
          onSetSplit((value) => clampSplitPercent(value - 5));
        }
        if (event.key === "ArrowRight") {
          event.preventDefault();
          onSetSplit((value) => clampSplitPercent(value + 5));
        }
        if (event.key === "Home") {
          event.preventDefault();
          onSetSplit(30);
        }
        if (event.key === "End") {
          event.preventDefault();
          onSetSplit(70);
        }
        if (event.key === "Enter") {
          event.preventDefault();
          onSetSplit(DEFAULT_SPLIT_PERCENT);
        }
      }}
      style={{
        cursor: "col-resize",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        minHeight: 420,
        outline: "none",
        touchAction: "none",
      }}
    >
      <Box
        style={{
          width: 4,
          height: 64,
          borderRadius: 999,
          background: isResizing ? "var(--mantine-color-blue-5)" : "rgba(148,163,184,0.35)",
          transition: "background 120ms ease",
        }}
      />
    </Box>
  );
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
    <Group
      justify="space-between"
      gap="xs"
      px="md"
      py={8}
      style={{ borderTop: "1px solid rgba(255,255,255,0.08)" }}
    >
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

function LabFrameFallback({
  activeLabUrl,
  onReload,
}: {
  activeLabUrl: string;
  onReload: () => void;
}) {
  return (
    <Box
      style={{
        position: "absolute",
        right: 12,
        bottom: 12,
        maxWidth: 360,
        border: "1px solid rgba(148,163,184,0.2)",
        borderRadius: 8,
        background: "rgba(2,6,23,0.86)",
        padding: "0.55rem 0.7rem",
        boxShadow: "0 10px 30px rgba(0,0,0,0.25)",
      }}
    >
      <Group gap="xs" justify="space-between" wrap="nowrap">
        <Text size="xs" c="dimmed">
          Blank or blocked?
        </Text>
        <Button
          size="compact-xs"
          variant="subtle"
          onClick={onReload}
          rightSection={<IconRefresh size={12} />}
        >
          Reload
        </Button>
        <Button
          size="compact-xs"
          variant="subtle"
          component="a"
          href={activeLabUrl}
          target="_blank"
          rel="noreferrer"
          rightSection={<IconExternalLink size={12} />}
        >
          Open
        </Button>
      </Group>
    </Box>
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
  const [labFullscreen, setLabFullscreen] = useState(false);
  const [labMode, setLabMode] = useState<LabViewMode>("app");
  const [appUrlInput, setAppUrlInput] = useState("");
  const [appFrameUrl, setAppFrameUrl] = useState<string | null>(null);
  const [appFrameKey, setAppFrameKey] = useState(0);
  const [appUrlError, setAppUrlError] = useState<string | null>(null);
  const [splitPercent, setSplitPercent] = useState(DEFAULT_SPLIT_PERCENT);
  const [isResizingSplit, setIsResizingSplit] = useState(false);
  const [podActionLoading, setPodActionLoading] = useState(false);
  const [podActionError, setPodActionError] = useState<string | null>(null);
  const splitContainerRef = useRef<HTMLDivElement | null>(null);
  const autoStartAttempted = useRef(false);
  const autoReloadedAppUrl = useRef<string | null>(null);
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
  const activeLabUrl = labMode === "app" ? appFrameUrl : pod?.terminalUrl;
  const activeLabLabel = labMode === "app" ? "app" : "console";
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

  useEffect(() => {
    const saved = window.localStorage.getItem(SPLIT_STORAGE_KEY);
    if (!saved) return;

    const next = Number(saved);
    if (Number.isFinite(next)) {
      setSplitPercent(clampSplitPercent(next));
    }
  }, []);

  useEffect(() => {
    window.localStorage.setItem(SPLIT_STORAGE_KEY, String(Math.round(splitPercent)));
  }, [splitPercent]);

  useEffect(() => {
    if (!isResizingSplit) return;

    const previousCursor = document.body.style.cursor;
    const previousUserSelect = document.body.style.userSelect;
    document.body.style.cursor = "col-resize";
    document.body.style.userSelect = "none";

    function handlePointerMove(event: PointerEvent) {
      const container = splitContainerRef.current;
      if (!container) return;

      const rect = container.getBoundingClientRect();
      const availableWidth = rect.width - 12;
      const minLeftPercent = (MIN_TASK_PANEL_PX / availableWidth) * 100;
      const maxLeftPercent = 100 - (MIN_LAB_PANEL_PX / availableWidth) * 100;
      const rawPercent = ((event.clientX - rect.left) / availableWidth) * 100;
      const boundedPercent = Math.min(maxLeftPercent, Math.max(minLeftPercent, rawPercent));
      setSplitPercent(clampSplitPercent(boundedPercent));
    }

    function handlePointerUp() {
      setIsResizingSplit(false);
    }

    window.addEventListener("pointermove", handlePointerMove);
    window.addEventListener("pointerup", handlePointerUp, { once: true });

    return () => {
      document.body.style.cursor = previousCursor;
      document.body.style.userSelect = previousUserSelect;
      window.removeEventListener("pointermove", handlePointerMove);
      window.removeEventListener("pointerup", handlePointerUp);
    };
  }, [isResizingSplit]);

  useEffect(() => {
    if (labMode === "console" && podStatus === "RUNNING" && !pod?.terminalUrl && pod?.appUrl) {
      setLabMode("app");
    }
  }, [labMode, pod?.appUrl, pod?.terminalUrl, podStatus]);

  useEffect(() => {
    if (!pod?.appUrl) return;
    setAppFrameUrl((current) => current ?? pod.appUrl ?? null);
    setAppUrlInput((current) => current || pod.appUrl || "");
  }, [pod?.appUrl]);

  useEffect(() => {
    if (podStatus !== "RUNNING" || !pod?.appUrl || appFrameUrl !== pod.appUrl) return;
    if (autoReloadedAppUrl.current === pod.appUrl) return;

    autoReloadedAppUrl.current = pod.appUrl;
    const id = window.setTimeout(() => {
      setAppFrameKey((key) => key + 1);
    }, 1200);

    return () => window.clearTimeout(id);
  }, [appFrameUrl, pod?.appUrl, podStatus]);

  function navigateAppFrame(nextInput = appUrlInput) {
    const nextUrl = resolveLabUrl(nextInput, pod?.appUrl);
    if (!nextUrl) {
      setAppUrlError("Enter a valid URL or path.");
      return;
    }

    setAppUrlError(null);
    setAppFrameUrl(nextUrl);
    setAppUrlInput(nextUrl);
    setAppFrameKey((key) => key + 1);
  }

  function reloadAppFrame() {
    if (!appFrameUrl) return;
    setAppFrameKey((key) => key + 1);
  }

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
        ref={splitContainerRef}
        style={{
          display: "grid",
          gridTemplateColumns: labFullscreen
            ? "1fr"
            : `minmax(${MIN_TASK_PANEL_PX}px, ${splitPercent}%) 12px minmax(${MIN_LAB_PANEL_PX}px, 1fr)`,
          columnGap: labFullscreen ? "1rem" : 0,
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

        {!labFullscreen && (
          <SplitResizeHandle
            isResizing={isResizingSplit}
            onStartResize={() => setIsResizingSplit(true)}
            onSetSplit={setSplitPercent}
          />
        )}

        {!labFullscreen && (
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
              style={{ borderBottom: "1px solid rgba(255,255,255,0.08)" }}
            >
              <Group gap="xs">
                <Text size="sm" fw={600}>
                  Lab Environment
                </Text>
                <ChallengePodStatusBadge status={podStatus} />
              </Group>
              <Group gap="xs" wrap="nowrap">
                <SplitPresetControls
                  onFocusTask={() => setSplitPercent(65)}
                  onReset={() => setSplitPercent(DEFAULT_SPLIT_PERCENT)}
                  onFocusLab={() => setSplitPercent(35)}
                />
                <SegmentedControl
                  size="xs"
                  value={labMode}
                  onChange={(value) => setLabMode(value as LabViewMode)}
                  data={[
                    {
                      value: "app",
                      label: (
                        <Group gap={4} wrap="nowrap">
                          <IconWorld size={12} />
                          <span>App</span>
                        </Group>
                      ),
                    },
                    {
                      value: "console",
                      label: (
                        <Group gap={4} wrap="nowrap">
                          <IconTerminal2 size={12} />
                          <span>Console</span>
                        </Group>
                      ),
                      disabled: !pod?.terminalUrl,
                    },
                  ]}
                />
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
                {activeLabUrl && (
                  <Tooltip label={`Open ${activeLabLabel} in new tab`}>
                    <ActionIcon
                      component="a"
                      href={activeLabUrl}
                      target="_blank"
                      rel="noreferrer"
                      variant="subtle"
                      aria-label={`Open ${activeLabLabel} in new tab`}
                    >
                      <IconExternalLink size={16} />
                    </ActionIcon>
                  </Tooltip>
                )}
                <Tooltip label="Focus instructions">
                  <ActionIcon
                    variant="subtle"
                    onClick={() => setLabFullscreen(true)}
                    aria-label="Hide lab pane"
                  >
                    <IconMaximize size={16} />
                  </ActionIcon>
                </Tooltip>
              </Group>
            </Group>
            {labMode === "app" && podStatus === "RUNNING" && pod?.appUrl && (
              <Box
                component="form"
                px="md"
                py="xs"
                style={{
                  borderBottom: "1px solid rgba(255,255,255,0.08)",
                  background: "rgba(2,6,23,0.35)",
                }}
                onSubmit={(event) => {
                  event.preventDefault();
                  navigateAppFrame();
                }}
              >
                <Group gap="xs" wrap="nowrap">
                  <TextInput
                    size="xs"
                    value={appUrlInput}
                    onChange={(event) => {
                      setAppUrlInput(event.currentTarget.value);
                      if (appUrlError) setAppUrlError(null);
                    }}
                    onBlur={() => {
                      if (!appUrlInput.trim() && pod.appUrl) {
                        setAppUrlInput(pod.appUrl);
                      }
                    }}
                    placeholder={pod.appUrl}
                    error={appUrlError}
                    aria-label="Lab app URL"
                    style={{ flex: 1 }}
                    styles={{
                      input: {
                        fontFamily: "var(--font-geist-mono), monospace",
                        minHeight: 30,
                      },
                      error: { marginTop: 2 },
                    }}
                  />
                  <Tooltip label="Go">
                    <ActionIcon type="submit" variant="light" aria-label="Go to URL">
                      <IconArrowRight size={16} />
                    </ActionIcon>
                  </Tooltip>
                  <Tooltip label="Reload">
                    <ActionIcon
                      variant="subtle"
                      onClick={reloadAppFrame}
                      aria-label="Reload app frame"
                      disabled={!appFrameUrl}
                    >
                      <IconRefresh size={16} />
                    </ActionIcon>
                  </Tooltip>
                </Group>
              </Box>
            )}
            <Box style={{ position: "relative", flex: 1 }}>
              {podStatus === "RUNNING" && activeLabUrl ? (
                <>
                  <iframe
                    key={labMode === "app" ? appFrameKey : undefined}
                    src={activeLabUrl}
                    title={`Challenge lab ${activeLabLabel}`}
                    allow="clipboard-read; clipboard-write"
                    style={{
                      position: "absolute",
                      inset: 0,
                      width: "100%",
                      height: "100%",
                      border: 0,
                      background: "#050914",
                      pointerEvents: isResizingSplit ? "none" : undefined,
                    }}
                  />
                  {labMode === "app" && (
                    <LabFrameFallback activeLabUrl={activeLabUrl} onReload={reloadAppFrame} />
                  )}
                </>
              ) : (
                <Stack
                  align="center"
                  justify="center"
                  gap="sm"
                  style={{ position: "absolute", inset: 0, padding: "1.5rem", textAlign: "center" }}
                >
                  {podStatusLoading ||
                  podStatus === "PROVISIONING" ||
                  podActionLoading ||
                  dockerImageCheck.status === "checking" ? (
                    <Loader size="sm" />
                  ) : null}
                  <Text fw={600}>
                    {startDisabledReason
                      ? "Lab cannot start yet"
                      : podStatus === "FAILED"
                        ? "Lab failed to start"
                        : podStatus === "TERMINATING"
                          ? "Stopping lab..."
                          : podStatus === "RUNNING"
                            ? `No ${activeLabLabel} URL available`
                            : "Starting lab..."}
                  </Text>
                  <Text size="sm" c="dimmed" maw={360}>
                    {startDisabledReason
                      ? startDisabledReason
                      : podStatus === "FAILED"
                        ? "Try restarting the lab from the toolbar."
                        : "The app will appear here as soon as the pod is ready."}
                  </Text>
                  {(podActionError || podStatusError) && (
                    <Text size="xs" c="red.3">
                      {podActionError ?? podStatusError}
                    </Text>
                  )}
                </Stack>
              )}
            </Box>
            <ConsoleCredentials password={pod?.terminalPassword} expiresAt={terminalExpiry} />
          </Paper>
        )}
      </Box>

      {labFullscreen && (
        <Group justify="flex-end" px="lg" pb="md" style={{ flexShrink: 0 }}>
          <Button
            variant="subtle"
            leftSection={<IconExternalLink size={16} />}
            component="a"
            href={activeLabUrl ?? undefined}
            target="_blank"
            rel="noreferrer"
            disabled={!activeLabUrl}
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
