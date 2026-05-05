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
  Radio,
  Stack,
  Stepper,
  Text,
  TextInput,
  ThemeIcon,
  Title,
  Tooltip,
} from "@mantine/core";
import { useMediaQuery } from "@mantine/hooks";
import { notifications } from "@mantine/notifications";
import {
  IconArrowLeft,
  IconArrowRight,
  IconBulb,
  IconCheck,
  IconChevronLeft,
  IconChevronRight,
  IconExternalLink,
  IconPlayerPlay,
  IconPlayerStop,
  IconRefresh,
  IconTrophy,
  IconWorld,
} from "@tabler/icons-react";
import Link from "next/link";
import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { LabPodStatusBadge } from "@/src/features/lab-pod/components/LabPodStatusBadge";
import { useLabPodStatus } from "@/src/features/lab-pod/hooks/useLabPodStatus";
import {
  submitChallengeFlag,
  submitChallengeChoice,
  completeTheoryChallenge,
} from "@/src/features/course/actions/labs";
import {
  DOCKER_IMAGE_ERROR,
  getDifficultyColor,
  getStatusColor,
} from "@/src/features/course/constants/challengeConstants";
import { useDockerImageCheck } from "@/src/features/course/hooks/useDockerImageCheck";
import { useApiClient } from "@/src/shared/lib/api/client";
import { getSanitizedHtml } from "@/src/shared/lib/utils";
import type { LabStudentDto, ChallengeStudentDto } from "@/src/shared/types/course";

const FLAG_WRAPPED_PATTERN = /^ISTP\{[A-Za-z0-9_]+\}$/;
const FLAG_INNER_PATTERN = /^[A-Za-z0-9_]+$/;

function pickInitialStep(challenges: ChallengeStudentDto[]): number {
  const firstUnsolved = challenges.findIndex((st) => !st.isSolved);
  return firstUnsolved === -1 ? Math.max(challenges.length - 1, 0) : firstUnsolved;
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
      p="sm"
      style={{
        background: url
          ? "linear-gradient(135deg, rgba(59,130,246,0.14), rgba(20,184,166,0.08))"
          : "rgba(255,255,255,0.03)",
      }}
    >
      <Stack gap="sm">
        <Group justify="space-between" align="flex-start" gap="sm" wrap="nowrap">
          <Group gap="sm" align="flex-start" wrap="nowrap">
            <ThemeIcon variant="light" radius="md" size={38}>
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

export function LabPlayView({
  courseId,
  labId,
  initialChallenge,
}: {
  courseId: string;
  labId: string;
  initialChallenge: LabStudentDto;
}) {
  const isNarrow = useMediaQuery("(max-width: 900px)");
  const apiClient = useApiClient();
  const [lab, setChallenge] = useState<LabStudentDto>(initialChallenge);
  const [activeStep, setActiveStep] = useState<number>(() =>
    pickInitialStep(initialChallenge.challenges ?? [])
  );
  const [flagInput, setFlagInput] = useState("");
  const [selectedOption, setSelectedOption] = useState<string>("");
  const [submitting, setSubmitting] = useState(false);
  const [hintOpen, setHintOpen] = useState(false);
  const [labCollapsed, setLabCollapsed] = useState(false);
  const [podActionLoading, setPodActionLoading] = useState(false);
  const [podActionError, setPodActionError] = useState<string | null>(null);
  const autoStartAttempted = useRef(false);

  const {
    data: pod,
    error: podStatusError,
    loading: podStatusLoading,
    refetch: refetchPodStatus,
  } = useLabPodStatus(labId);

  const dockerImage = lab.dockerImage ?? "";
  const dockerImageCheck = useDockerImageCheck(dockerImage);

  const challenges = lab.challenges ?? [];
  const total = challenges.length;
  const solvedCount = lab.solvedChallengeCount ?? 0;
  const percent = total === 0 ? 0 : Math.round((solvedCount / total) * 100);
  const current = challenges[activeStep] ?? null;
  const allSolved = lab.isSolved ?? false;

  const sanitizedChallengeDescription = useMemo(
    () => (lab.description ? getSanitizedHtml(lab.description) : ""),
    [lab.description]
  );
  const sanitizedChallengeDescription = useMemo(
    () => (current?.description ? getSanitizedHtml(current.description) : ""),
    [current]
  );

  const podStatus = pod?.status ?? "NOT_FOUND";
  const canStartPod =
    dockerImageCheck.status === "success" ||
    (dockerImageCheck.status === "idle" && !dockerImage.trim());
  const startDisabled = podActionLoading || dockerImageCheck.status === "checking" || !canStartPod;
  const showLabPanel = isNarrow || !labCollapsed;
  const labPanelId = "lab-play-lab-panel";
  const labIsStarting =
    podStatusLoading ||
    podStatus === "PROVISIONING" ||
    podActionLoading ||
    podStatus === "TERMINATING";

  let startDisabledReason: string | null = null;
  if (dockerImageCheck.status === "checking") {
    startDisabledReason = "Checking Docker image...";
  } else if (dockerImageCheck.status === "error") {
    startDisabledReason = dockerImageCheck.message ?? "Public GHCR image is not reachable";
  } else if (dockerImage.trim() && dockerImageCheck.status === "idle") {
    startDisabledReason = DOCKER_IMAGE_ERROR;
  }

  const isMC = current?.type === "MULTIPLE_CHOICE";
  const isTheory = Boolean(current?.isTheory);
  const hasHint = Boolean(current?.hint?.trim());

  // Reset per-subtask UI when navigating between labs
  useEffect(() => {
    setFlagInput("");
    setSelectedOption(current?.selectedOptionId ?? "");
    setHintOpen(false);
  }, [activeStep, current?.selectedOptionId]);

  const handleStartPod = useCallback(async () => {
    if (!canStartPod) {
      setPodActionError(startDisabledReason ?? "Lab cannot be started with this Docker image.");
      return;
    }
    setPodActionLoading(true);
    setPodActionError(null);
    try {
      await apiClient.POST("/api/v1/lab-pods/{labId}", {
        params: { path: { labId } },
      });
      await refetchPodStatus();
    } catch (e) {
      setPodActionError(e instanceof Error ? e.message : "Failed to start lab.");
    } finally {
      setPodActionLoading(false);
    }
  }, [apiClient, canStartPod, labId, refetchPodStatus, startDisabledReason]);

  const handleStopPod = useCallback(async () => {
    setPodActionLoading(true);
    setPodActionError(null);
    try {
      await apiClient.DELETE("/api/v1/lab-pods/{labId}", {
        params: { path: { labId } },
      });
      await refetchPodStatus();
    } catch (e) {
      setPodActionError(e instanceof Error ? e.message : "Failed to stop lab.");
    } finally {
      setPodActionLoading(false);
    }
  }, [apiClient, labId, refetchPodStatus]);

  useEffect(() => {
    if (podStatusLoading || autoStartAttempted.current) return;
    if (podStatus !== "NOT_FOUND" && podStatus !== "FAILED") return;
    if (dockerImageCheck.status === "checking") return;
    autoStartAttempted.current = true;
    void handleStartPod();
  }, [dockerImageCheck.status, handleStartPod, podStatus, podStatusLoading]);

  function updateChallengeSolved(challengeId: string, patch: Partial<ChallengeStudentDto>) {
    setChallenge((prev) => {
      const updatedChallenges = (prev.challenges ?? []).map((st) =>
        st.id === challengeId ? { ...st, ...patch, isSolved: true } : st
      );
      const newSolved = updatedChallenges.filter((st) => st.isSolved).length;
      return {
        ...prev,
        challenges: updatedChallenges,
        solvedChallengeCount: newSolved,
        isSolved: newSolved === updatedChallenges.length && updatedChallenges.length > 0,
      };
    });
  }

  async function handleSubmitFlag() {
    if (!current || !current.id || !lab.id) return;

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
    const result = await submitChallengeFlag(lab.id, current.id, normalizedFlag);
    setSubmitting(false);

    if (!result.success) {
      notifications.show({ color: "red", title: "Submission failed", message: result.error });
      return;
    }

    if (result.data.isCorrect && current.id) {
      updateChallengeSolved(current.id, { solvedFlag: normalizedFlag });
      notifications.show({
        color: "teal",
        title: "Correct flag!",
        message: result.data.isChallengeSolved ? "Lab completed. Nice work!" : "Lab solved.",
      });
    } else {
      notifications.show({
        color: "red",
        title: "Incorrect flag",
        message: "Not quite — try again.",
      });
    }
  }

  async function handleSubmitChoice() {
    if (!current || !current.id || !lab.id || !selectedOption) return;

    const isOnceMode = (lab.mcAttemptsMode ?? "UNLIMITED") === "ONCE";

    setSubmitting(true);
    const result = await submitChallengeChoice(lab.id, current.id, selectedOption, courseId);
    setSubmitting(false);

    if (!result.success) {
      notifications.show({ color: "red", title: "Submission failed", message: result.error });
      return;
    }

    if (result.data.isCorrect) {
      // Correct in any mode → mark solved
      updateChallengeSolved(current.id, { selectedOptionId: selectedOption });
      notifications.show({
        color: "teal",
        title: "Correct answer!",
        message: result.data.isChallengeSolved ? "Lab completed. Nice work!" : "Lab solved.",
      });
    } else if (isOnceMode) {
      // ONCE mode + wrong: mark as done (progress counts), highlight correct option
      updateChallengeSolved(current.id, {
        selectedOptionId: selectedOption,
        correctOptionId: result.data.correctOptionId ?? undefined,
      });
      notifications.show({
        color: "orange",
        title: "Incorrect answer",
        message: "That was your only attempt — the correct answer is highlighted.",
      });
    } else {
      // UNLIMITED mode + wrong: no state saved, allow retry
      notifications.show({
        color: "red",
        title: "Incorrect answer",
        message: "Not quite — try again!",
      });
    }
  }

  async function handleCompleteTheory() {
    if (!current || !current.id || !lab.id || current.isSolved) return;
    setSubmitting(true);
    const result = await completeTheoryChallenge(lab.id, current.id);
    setSubmitting(false);
    if (!result.success) {
      notifications.show({ color: "red", title: "Could not complete task", message: result.error });
      return;
    }
    updateChallengeSolved(current.id, {});
    notifications.show({
      color: "teal",
      title: "Task completed!",
      message: result.data.isChallengeSolved ? "Lab completed. Nice work!" : "Moving on.",
    });
    if (activeStep < total - 1) {
      goToStep(activeStep + 1);
    }
  }

  // Free-order navigation — any step is reachable at any time
  function goToStep(step: number) {
    if (step < 0 || step >= total) return;
    setActiveStep(step);
  }

  const displaySelectedOption = current?.isSolved
    ? (current.selectedOptionId ?? selectedOption)
    : selectedOption;
  const correctOptionId = current?.correctOptionId ?? null;
  // ONCE mode: subtask is "solved" but the answer was wrong (correctOptionId is set on the subtask)
  const isOnceModeWrongAnswer = Boolean(current?.isSolved && current?.correctOptionId);
  // UNLIMITED mode: a wrong option was picked this session but not yet persisted → lock options temporarily
  const hasWrongAnswer =
    Boolean(!current?.isSolved && current?.selectedOptionId) || isOnceModeWrongAnswer;

  return (
    <Box
      style={{
        display: "flex",
        flexDirection: "column",
        height: isNarrow ? "auto" : "calc(100vh - 60px - var(--app-shell-padding) * 2)",
        minHeight: 0,
        overflow: isNarrow ? "visible" : "hidden",
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
          {!showLabPanel && (
            <Button
              size="xs"
              variant="light"
              leftSection={<IconChevronLeft size={14} />}
              onClick={() => setLabCollapsed(false)}
              aria-controls={labPanelId}
              aria-expanded={showLabPanel}
            >
              Show lab
            </Button>
          )}
          <Badge variant="light" color={getStatusColor(lab.status ?? "")}>
            {lab.status}
          </Badge>
          <Badge variant="light" color={getDifficultyColor(lab.difficulty ?? "")}>
            {lab.difficulty}
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
          gridTemplateColumns: showLabPanel
            ? isNarrow
              ? "1fr"
              : "minmax(0, 1.12fr) minmax(340px, 0.88fr)"
            : "minmax(0, 1fr) minmax(0, 0fr)",
          gap: showLabPanel ? "1rem" : 0,
          padding: "0 1rem 1rem",
          flex: 1,
          minHeight: 0,
          overflow: isNarrow ? "visible" : "hidden",
          transition: "grid-template-columns 220ms ease, gap 220ms ease",
        }}
      >
        {/* Left panel */}
        <Paper
          withBorder
          radius="md"
          p="lg"
          style={{
            background: "rgba(255,255,255,0.02)",
            overflow: "auto",
            minHeight: isNarrow ? undefined : 0,
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
                  Lab
                </Text>
                <Title order={2} style={{ lineHeight: 1.2 }}>
                  {lab.title}
                </Title>
              </Stack>

              <Stack gap={6}>
                <Group justify="space-between" align="center">
                  <Text size="xs" tt="uppercase" c="dimmed" fw={700}>
                    Progress
                  </Text>
                  <Text size="xs" fw={600} c={allSolved ? "teal.3" : "blue.3"}>
                    {solvedCount} / {total} labs
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

            {/* Stepper — all steps freely clickable */}
            {total > 0 && (
              <Stepper
                active={activeStep}
                onStepClick={goToStep}
                allowNextStepsSelect={true}
                size="xs"
                iconSize={28}
              >
                {challenges.map((st) => (
                  <Stepper.Step
                    key={st.id}
                    completedIcon={<IconCheck size={14} />}
                    icon={st.isSolved ? <IconCheck size={14} /> : undefined}
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
                        Lab {activeStep + 1} of {total}
                      </Text>
                      {current.isSolved && (
                        <Badge variant="light" color="teal" size="xs">
                          Solved
                        </Badge>
                      )}
                      {isMC && (
                        <Badge variant="light" color="violet" size="xs">
                          Multiple Choice
                        </Badge>
                      )}
                      {(current.points ?? 0) > 0 && (
                        <Badge variant="light" color="blue" size="xs">
                          {current.points}pt
                        </Badge>
                      )}
                    </Group>
                    <Title order={4} style={{ lineHeight: 1.3 }}>
                      {current.title}
                    </Title>
                  </Stack>

                  {sanitizedChallengeDescription && (
                    <Box
                      className="course-description"
                      style={{ fontSize: "var(--mantine-font-size-sm)" }}
                      dangerouslySetInnerHTML={{ __html: sanitizedChallengeDescription }}
                    />
                  )}

                  {/* Hint */}
                  {hasHint && (
                    <Box>
                      <Button
                        variant="subtle"
                        size="xs"
                        color="yellow"
                        leftSection={<IconBulb size={14} />}
                        onClick={() => setHintOpen((o) => !o)}
                      >
                        {hintOpen ? "Hide hint" : "Show hint"}
                      </Button>
                      {hintOpen && (
                        <Paper
                          withBorder
                          radius="md"
                          p="sm"
                          mt="xs"
                          style={{
                            background: "rgba(234,179,8,0.07)",
                            borderColor: "rgba(234,179,8,0.25)",
                          }}
                        >
                          <Text size="sm" c="yellow.3">
                            {current?.hint}
                          </Text>
                        </Paper>
                      )}
                    </Box>
                  )}

                  {/* THEORY task — no submission needed */}
                  {isTheory && !isMC && (
                    <Button
                      onClick={() => void handleCompleteTheory()}
                      disabled={current.isSolved || submitting}
                      loading={submitting}
                      color={current.isSolved ? "teal" : "blue"}
                      leftSection={current.isSolved ? <IconCheck size={16} /> : undefined}
                    >
                      {current.isSolved ? "Completed" : "Mark as done"}
                    </Button>
                  )}

                  {/* FLAG submission */}
                  {!isMC && !isTheory && (
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
                              void handleSubmitFlag();
                            }
                          }}
                          aria-label="Flag input"
                        />
                        <Button
                          onClick={() => void handleSubmitFlag()}
                          disabled={current.isSolved}
                          loading={submitting}
                          color={current.isSolved ? "teal" : "blue"}
                          leftSection={current.isSolved ? <IconCheck size={16} /> : undefined}
                        >
                          {current.isSolved ? "Solved" : "Submit"}
                        </Button>
                      </Group>
                    </Stack>
                  )}

                  {/* MULTIPLE CHOICE submission */}
                  {isMC && (
                    <Stack gap="sm">
                      <Text size="xs" tt="uppercase" c="dimmed" fw={700}>
                        Choose your answer
                      </Text>
                      <Radio.Group
                        value={displaySelectedOption}
                        onChange={(val) => {
                          if (!current.isSolved && !hasWrongAnswer) setSelectedOption(val);
                        }}
                      >
                        <Stack gap="xs">
                          {(current.options ?? []).map((opt) => {
                            const isSelected = displaySelectedOption === opt.id;
                            const isCorrectOpt = correctOptionId === opt.id;
                            const isWrongSelected = hasWrongAnswer && isSelected && !isCorrectOpt;

                            let bg = "rgba(255,255,255,0.02)";
                            let border: string | undefined = undefined;
                            // Correct solved: selected option shown teal
                            if (current.isSolved && isSelected && !isOnceModeWrongAnswer) {
                              bg = "rgba(20,184,166,0.12)";
                              border = "rgba(20,184,166,0.4)";
                              // Correct option highlight (after wrong answer in any mode)
                            } else if (isCorrectOpt && hasWrongAnswer) {
                              bg = "rgba(20,184,166,0.10)";
                              border = "rgba(20,184,166,0.35)";
                              // Wrong selected option: show red
                            } else if (isWrongSelected) {
                              bg = "rgba(248,113,113,0.10)";
                              border = "rgba(248,113,113,0.35)";
                              // Normal selection before submit
                            } else if (!hasWrongAnswer && !current.isSolved && isSelected) {
                              bg = "rgba(59,130,246,0.10)";
                              border = "rgba(59,130,246,0.4)";
                            }

                            const locked = current.isSolved || hasWrongAnswer;

                            return (
                              <Paper
                                key={opt.id}
                                withBorder
                                radius="md"
                                p="sm"
                                style={{
                                  background: bg,
                                  borderColor: border,
                                  cursor: locked ? "default" : "pointer",
                                  opacity: locked && !isSelected && !isCorrectOpt ? 0.45 : 1,
                                  transition: "background 140ms, border-color 140ms",
                                }}
                                onClick={() => {
                                  if (!locked && opt.id) setSelectedOption(opt.id);
                                }}
                              >
                                <Group gap="sm" wrap="nowrap">
                                  <Radio
                                    value={opt.id ?? ""}
                                    disabled={locked}
                                    style={{ flexShrink: 0 }}
                                  />
                                  <Text
                                    size="sm"
                                    style={{
                                      flex: 1,
                                      // Strikethrough on wrong selected option in ONCE mode
                                      textDecoration:
                                        isOnceModeWrongAnswer && isWrongSelected
                                          ? "line-through"
                                          : undefined,
                                      color:
                                        isOnceModeWrongAnswer && isWrongSelected
                                          ? "var(--mantine-color-red-4)"
                                          : undefined,
                                    }}
                                  >
                                    {opt.text}
                                  </Text>
                                  {/* Correct answer checkmark (correct solved, not once-wrong) */}
                                  {current.isSolved && isSelected && !isOnceModeWrongAnswer && (
                                    <IconCheck
                                      size={15}
                                      color="var(--mantine-color-teal-4)"
                                      style={{ flexShrink: 0 }}
                                    />
                                  )}
                                  {/* Correct option highlight after any wrong answer */}
                                  {isCorrectOpt && hasWrongAnswer && (
                                    <IconCheck
                                      size={15}
                                      color="var(--mantine-color-teal-4)"
                                      style={{ flexShrink: 0 }}
                                    />
                                  )}
                                  {isWrongSelected && (
                                    <Text size="xs" c="red.4" fw={600} style={{ flexShrink: 0 }}>
                                      ✗
                                    </Text>
                                  )}
                                </Group>
                              </Paper>
                            );
                          })}
                        </Stack>
                      </Radio.Group>
                      <Button
                        onClick={() => void handleSubmitChoice()}
                        disabled={current.isSolved || hasWrongAnswer || !selectedOption}
                        loading={submitting}
                        color={
                          isOnceModeWrongAnswer ? "orange" : current.isSolved ? "teal" : "blue"
                        }
                        leftSection={
                          isOnceModeWrongAnswer ? undefined : current.isSolved ? (
                            <IconCheck size={16} />
                          ) : undefined
                        }
                      >
                        {isOnceModeWrongAnswer
                          ? "Attempted (wrong)"
                          : current.isSolved
                            ? "Answered"
                            : "Submit answer"}
                      </Button>
                    </Stack>
                  )}

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
                      disabled={activeStep >= total - 1}
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
                    <Text fw={600}>Lab completed</Text>
                    <Text size="sm" c="dimmed">
                      You solved every lab. Well done!
                    </Text>
                  </Stack>
                </Group>
              </Paper>
            )}
          </Stack>
        </Paper>

        {/* Right panel — lab environment */}
        <Box
          id={labPanelId}
          aria-hidden={!showLabPanel}
          style={{
            minWidth: 0,
            overflow: "hidden",
            pointerEvents: showLabPanel ? "auto" : "none",
          }}
        >
          <Paper
            withBorder
            radius="md"
            p={0}
            style={{
              background: "rgba(255,255,255,0.02)",
              display: "flex",
              flexDirection: "column",
              minHeight: isNarrow ? undefined : 0,
              overflow: "hidden",
              opacity: showLabPanel ? 1 : 0,
              transform: showLabPanel ? "translateX(0)" : "translateX(14px)",
              transition: "opacity 180ms ease, transform 220ms ease",
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
                  Lab
                </Text>
                <LabPodStatusBadge status={podStatus} />
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

                {!isNarrow && (
                  <Tooltip label="Hide lab panel">
                    <ActionIcon
                      variant="subtle"
                      onClick={() => setLabCollapsed(true)}
                      aria-label="Hide lab panel"
                      aria-controls={labPanelId}
                      aria-expanded={showLabPanel}
                    >
                      <IconChevronRight size={16} />
                    </ActionIcon>
                  </Tooltip>
                )}
              </Group>
            </Group>

            <Stack gap="sm" p="md" style={{ flex: 1, overflow: isNarrow ? "visible" : "auto" }}>
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
                title="Lab app"
                description={
                  podStatus === "RUNNING" && pod?.appUrl
                    ? "Web service ready."
                    : labIsStarting
                      ? "Starting web service."
                      : "Start the lab to get app access."
                }
                icon={<IconWorld size={22} />}
                url={podStatus === "RUNNING" ? pod?.appUrl : null}
                buttonLabel="Open app"
                disabledLabel={labIsStarting ? "Starting..." : "App not ready"}
              />

              {(podActionError || podStatusError) && (
                <Paper
                  withBorder
                  radius="md"
                  p="md"
                  style={{ background: "rgba(248,113,113,0.08)" }}
                >
                  <Text size="sm" c="red.3">
                    {podActionError ?? podStatusError}
                  </Text>
                </Paper>
              )}
            </Stack>
          </Paper>
        </Box>
      </Box>
    </Box>
  );
}
