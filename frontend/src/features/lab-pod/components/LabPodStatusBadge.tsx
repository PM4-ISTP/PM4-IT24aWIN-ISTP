"use client";

import { Badge } from "@mantine/core";
import type { PodStatusEnum } from "../hooks/useLabPodStatus";

const STATUS_COLOR: Record<PodStatusEnum, string> = {
  NOT_FOUND: "gray",
  PROVISIONING: "yellow",
  RUNNING: "green",
  FAILED: "red",
  TERMINATING: "orange",
};

const STATUS_LABEL: Record<PodStatusEnum, string> = {
  NOT_FOUND: "Not started",
  PROVISIONING: "Starting…",
  RUNNING: "Running",
  FAILED: "Failed",
  TERMINATING: "Stopping…",
};

export function LabPodStatusBadge({ status }: { status: PodStatusEnum }) {
  return (
    <Badge variant="light" color={STATUS_COLOR[status]}>
      {STATUS_LABEL[status]}
    </Badge>
  );
}
