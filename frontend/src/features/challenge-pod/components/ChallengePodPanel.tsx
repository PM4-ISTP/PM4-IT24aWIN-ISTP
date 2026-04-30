"use client";

import { Anchor, Box, Button, Flex, Loader, Stack, Text } from "@mantine/core";
import {
  IconExternalLink,
  IconPlayerPlay,
  IconPlayerStop,
  IconTerminal2,
} from "@tabler/icons-react";
import { useState } from "react";
import { useApiClient } from "@/src/shared/lib/api/client";
import { DOCKER_IMAGE_ERROR } from "@/src/features/course/constants/challengeConstants";
import { useDockerImageCheck } from "@/src/features/course/hooks/useDockerImageCheck";
import { useChallengePodStatus } from "../hooks/useChallengePodStatus";
import { ChallengePodStatusBadge } from "./ChallengePodStatusBadge";

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

export function ChallengePodPanel({
  challengeId,
  dockerImage,
}: {
  challengeId: string;
  dockerImage?: string | null;
}) {
  const apiClient = useApiClient();
  const { data, loading, refetch } = useChallengePodStatus(challengeId);
  const dockerImageCheck = useDockerImageCheck(dockerImage ?? "");
  const [actionLoading, setActionLoading] = useState(false);

  const handleStart = async () => {
    setActionLoading(true);
    try {
      await apiClient.POST("/api/v1/challenge-pods/{challengeId}", {
        params: { path: { challengeId } },
      });
      await refetch();
    } finally {
      setActionLoading(false);
    }
  };

  const handleStop = async () => {
    setActionLoading(true);
    try {
      await apiClient.DELETE("/api/v1/challenge-pods/{challengeId}", {
        params: { path: { challengeId } },
      });
      await refetch();
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <Flex justify="flex-end" align="center" gap="xs">
        <Loader size="xs" />
        <Text size="xs" c="dimmed">
          Loading…
        </Text>
      </Flex>
    );
  }

  const status = data?.status ?? "NOT_FOUND";
  const canStart =
    dockerImageCheck.status === "success" ||
    (dockerImageCheck.status === "idle" && !dockerImage?.trim());
  const startDisabled = actionLoading || dockerImageCheck.status === "checking" || !canStart;
  let startDisabledReason: string | null = null;
  if (dockerImageCheck.status === "checking") {
    startDisabledReason = "Checking Docker image...";
  } else if (dockerImageCheck.status === "error") {
    startDisabledReason = dockerImageCheck.message ?? "Docker image is not reachable";
  } else if (dockerImage?.trim() && dockerImageCheck.status === "idle") {
    startDisabledReason = DOCKER_IMAGE_ERROR;
  }
  const startDisabledReasonColor = dockerImageCheck.status === "checking" ? "dimmed" : "red";

  return (
    <Stack gap={6} align="flex-end">
      <ChallengePodStatusBadge status={status} />

      {(status === "PROVISIONING" || status === "TERMINATING") && (
        <Loader size="xs" color="yellow" />
      )}

      {status === "NOT_FOUND" || status === "FAILED" ? (
        <Button
          size="xs"
          color="green"
          leftSection={<IconPlayerPlay size={14} />}
          loading={actionLoading}
          disabled={startDisabled}
          onClick={() => void handleStart()}
        >
          {status === "FAILED" ? "Retry" : "Start"}
        </Button>
      ) : status === "RUNNING" || status === "PROVISIONING" ? (
        <Button
          size="xs"
          color="red"
          variant="light"
          leftSection={<IconPlayerStop size={14} />}
          loading={actionLoading}
          onClick={() => void handleStop()}
        >
          Stop
        </Button>
      ) : null}

      {startDisabledReason && (status === "NOT_FOUND" || status === "FAILED") && (
        <Text size="xs" c={startDisabledReasonColor} ta="right">
          {startDisabledReason}
        </Text>
      )}

      {status === "RUNNING" && data && (
        <Stack gap={4} align="flex-end">
          {data.appUrl && (
            <Anchor href={data.appUrl} target="_blank" size="xs">
              <Flex align="center" gap={4}>
                <IconExternalLink size={12} />
                Open app
              </Flex>
            </Anchor>
          )}
          {data.terminalUrl && (
            <Anchor href={data.terminalUrl} target="_blank" size="xs">
              <Flex align="center" gap={4}>
                <IconTerminal2 size={12} />
                Open terminal
              </Flex>
            </Anchor>
          )}
          {data.terminalPassword && (
            <Box>
              <Text size="xs" c="dimmed">
                Username:{" "}
                <Text component="span" ff="monospace" size="xs">
                  student
                </Text>
              </Text>
              <Text size="xs" c="dimmed">
                Password:{" "}
                <Text component="span" ff="monospace" size="xs">
                  {data.terminalPassword}
                </Text>
              </Text>
            </Box>
          )}
          {data.expiresAt && (
            <Text size="xs" c="dimmed">
              Expires: {formatExpiry(data.expiresAt)}
            </Text>
          )}
        </Stack>
      )}
    </Stack>
  );
}
