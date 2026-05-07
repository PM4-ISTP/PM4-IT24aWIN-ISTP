"use client";

import { Anchor, Button, Flex, Loader, Stack, Text } from "@mantine/core";
import { IconExternalLink, IconPlayerPlay, IconPlayerStop } from "@tabler/icons-react";
import { useState } from "react";
import { useApiClient } from "@/src/shared/lib/api/client";
import { DOCKER_IMAGE_ERROR } from "@/src/features/course/constants/challengeConstants";
import { useDockerImageCheck } from "@/src/features/course/hooks/useDockerImageCheck";
import { useLabPodStatus } from "../hooks/useLabPodStatus";
import { LabPodStatusBadge } from "./LabPodStatusBadge";

export function LabPodPanel({
  labId,
  dockerImage,
}: {
  labId: string;
  dockerImage?: string | null;
}) {
  const apiClient = useApiClient();
  const { data, loading, refetch } = useLabPodStatus(labId);
  const dockerImageCheck = useDockerImageCheck(dockerImage ?? "");
  const [actionLoading, setActionLoading] = useState(false);

  const handleStart = async () => {
    setActionLoading(true);
    try {
      await apiClient.POST("/api/v1/lab-pods/{labId}", {
        params: { path: { labId } },
      });
      await refetch();
    } finally {
      setActionLoading(false);
    }
  };

  const handleStop = async () => {
    setActionLoading(true);
    try {
      await apiClient.DELETE("/api/v1/lab-pods/{labId}", {
        params: { path: { labId } },
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
    startDisabledReason = dockerImageCheck.message ?? "Public GHCR image is not reachable";
  } else if (dockerImage?.trim() && dockerImageCheck.status === "idle") {
    startDisabledReason = DOCKER_IMAGE_ERROR;
  }
  const startDisabledReasonColor = dockerImageCheck.status === "checking" ? "dimmed" : "red";

  return (
    <Stack gap={6} align="flex-end">
      <LabPodStatusBadge status={status} />

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
        </Stack>
      )}
    </Stack>
  );
}
