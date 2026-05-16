"use client";

import {
  ActionIcon,
  Badge,
  Box,
  Group,
  Paper,
  Stack,
  Text,
  ThemeIcon,
  Tooltip,
  Button,
} from "@mantine/core";
import {
  IconChevronRight,
  IconClockHour10,
  IconClockPlus,
  IconExternalLink,
  IconPlayerPlay,
  IconPlayerStop,
  IconRefresh,
  IconWorld,
} from "@tabler/icons-react";
import { type ReactNode } from "react";
import { LabPodStatusBadge } from "@/src/features/lab-pod/components/LabPodStatusBadge";

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
          rel="noopener noreferrer"
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

type ExtensionSummary = {
  label: string;
};

export function LabPlayPodPanel({
  isNarrow,
  labPanelId,
  showLabPanel,
  podStatus,
  podActionLoading,
  startDisabledReason,
  startDisabled,
  canExtendPod,
  extendDisabledReason,
  podExpiringNow,
  podExpiringSoon,
  podTimeLeftLabel,
  podExpiresAt,
  extensionSummary,
  labIsStarting,
  appUrl,
  podActionError,
  podStatusError,
  onStartPod,
  onStopPod,
  onExtendPod,
  onCollapsePanel,
}: {
  isNarrow: boolean;
  labPanelId: string;
  showLabPanel: boolean;
  podStatus: string;
  podActionLoading: boolean;
  startDisabledReason: string | null;
  startDisabled: boolean;
  canExtendPod: boolean;
  extendDisabledReason: string;
  podExpiringNow: boolean;
  podExpiringSoon: boolean;
  podTimeLeftLabel: string;
  podExpiresAt: Date | null;
  extensionSummary: ExtensionSummary;
  labIsStarting: boolean;
  appUrl?: string | null;
  podActionError: string | null;
  podStatusError: string | null;
  onStartPod: () => void;
  onStopPod: () => void;
  onExtendPod: () => void;
  onCollapsePanel: () => void;
}) {
  return (
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
                  onClick={onStopPod}
                  aria-label="Stop lab"
                >
                  <IconPlayerStop size={16} />
                </ActionIcon>
              </Tooltip>
            ) : (
              <Tooltip
                label={startDisabledReason ?? (podStatus === "FAILED" ? "Retry lab" : "Start lab")}
              >
                <ActionIcon
                  variant="subtle"
                  color="blue"
                  loading={podActionLoading}
                  disabled={startDisabled}
                  onClick={onStartPod}
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
                  onClick={onCollapsePanel}
                  aria-label="Hide lab panel"
                  aria-controls={labPanelId}
                  aria-expanded={showLabPanel}
                >
                  <IconChevronRight size={16} />
                </ActionIcon>
              </Tooltip>
            )}

            <Tooltip label={extendDisabledReason}>
              <ActionIcon
                variant="subtle"
                color={podExpiringNow ? "orange" : "yellow"}
                loading={podActionLoading}
                disabled={!canExtendPod}
                onClick={onExtendPod}
                aria-label="Extend lab"
              >
                <IconClockPlus size={16} />
              </ActionIcon>
            </Tooltip>
          </Group>
        </Group>

        <Stack gap="sm" p="md" style={{ flex: 1, overflow: isNarrow ? "visible" : "auto" }}>
          {podExpiringSoon && (
            <Paper
              withBorder
              radius="md"
              p="md"
              style={{
                background: podExpiringNow ? "rgba(248,113,113,0.10)" : "rgba(251,191,36,0.12)",
              }}
            >
              <Text size="sm" c={podExpiringNow ? "red.3" : "yellow.2"}>
                <Group gap={6} wrap="nowrap">
                  <IconClockHour10 size={16} />
                  <span>
                    {podExpiringNow
                      ? "Lab expires in less than 2 minutes."
                      : "Lab expires in less than 10 minutes."}
                  </span>
                </Group>
              </Text>
            </Paper>
          )}

          {startDisabledReason && (
            <Paper withBorder radius="md" p="md" style={{ background: "rgba(248,113,113,0.08)" }}>
              <Text size="sm" c="red.3">
                {startDisabledReason}
              </Text>
            </Paper>
          )}

          <LabLaunchCard
            title="Lab app"
            description={
              podStatus === "RUNNING" && appUrl
                ? "Web service ready."
                : labIsStarting
                  ? "Starting web service."
                  : "Start the lab to get app access."
            }
            icon={<IconWorld size={22} />}
            url={podStatus === "RUNNING" ? appUrl : null}
            buttonLabel="Open app"
            disabledLabel={labIsStarting ? "Starting..." : "App not ready"}
          />

          {podStatus === "RUNNING" && podExpiresAt && (
            <Paper withBorder radius="md" p="sm" style={{ background: "rgba(255,255,255,0.03)" }}>
              <Group justify="space-between" align="center" gap="sm">
                <Group gap={8}>
                  <IconClockHour10 size={16} />
                  <Stack gap={0}>
                    <Text size="sm" fw={600} c={podExpiringSoon ? "yellow.2" : undefined}>
                      Expires in {podTimeLeftLabel}
                    </Text>
                    <Text size="xs" c="dimmed">
                      at{" "}
                      {podExpiresAt.toLocaleTimeString("de-CH", {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </Text>
                  </Stack>
                </Group>
                <Badge variant="light" color={canExtendPod ? "blue" : "gray"} radius="sm">
                  {extensionSummary.label}
                </Badge>
              </Group>
              {!canExtendPod && (
                <Text size="xs" c="dimmed" mt={6}>
                  {extendDisabledReason}
                </Text>
              )}
            </Paper>
          )}

          {(podActionError || podStatusError) && (
            <Paper withBorder radius="md" p="md" style={{ background: "rgba(248,113,113,0.08)" }}>
              <Text size="sm" c="red.3">
                {podActionError ?? podStatusError}
              </Text>
            </Paper>
          )}
        </Stack>
      </Paper>
    </Box>
  );
}
