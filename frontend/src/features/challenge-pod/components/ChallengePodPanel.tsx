"use client";

import { Anchor, Box, Button, Flex, Loader, Stack, Text } from "@mantine/core";
import {
  IconExternalLink,
  IconPlayerPlay,
  IconPlayerStop,
  IconTerminal2,
} from "@tabler/icons-react";
import { useState } from "react";
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

export function ChallengePodPanel({ challengeId }: { challengeId: string }) {
  const { data, loading, refetch } = useChallengePodStatus(challengeId);
  const [actionLoading, setActionLoading] = useState(false);

  const handleStart = async () => {
    setActionLoading(true);
    try {
      await fetch(`/api/backend/api/v1/challenge-pods/${challengeId}`, {
        method: "POST",
        credentials: "include",
      });
      await refetch();
    } finally {
      setActionLoading(false);
    }
  };

  const handleStop = async () => {
    setActionLoading(true);
    try {
      await fetch(`/api/backend/api/v1/challenge-pods/${challengeId}`, {
        method: "DELETE",
        credentials: "include",
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
