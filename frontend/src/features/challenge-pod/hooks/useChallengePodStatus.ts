"use client";

import { useCallback, useEffect, useState } from "react";
import { POLL_INTERVAL_MS, RUNNING_POLL_INTERVAL_MS } from "../constants";

export type PodStatusEnum = "NOT_FOUND" | "PROVISIONING" | "RUNNING" | "FAILED" | "TERMINATING";

export interface PodStatusResponse {
  status: PodStatusEnum;
  podName?: string | null;
  appUrl?: string | null;
  terminalUrl?: string | null;
  terminalPassword?: string | null;
  createdAt?: string | null;
  expiresAt?: string | null;
}

export function useChallengePodStatus(
  challengeId: string,
  { enabled = true }: { enabled?: boolean } = {}
): {
  data: PodStatusResponse | null;
  error: string | null;
  loading: boolean;
  refetch: () => Promise<void>;
} {
  const [data, setData] = useState<PodStatusResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchOnce = useCallback(async () => {
    try {
      const response = await fetch(`/api/backend/api/v1/challenge-pods/${challengeId}`, {
        headers: {
          "Content-Type": "application/json",
        },
        credentials: "include",
      });

      if (!response.ok) {
        setError(`Request failed: ${response.status}`);
        return;
      }

      const json = (await response.json()) as PodStatusResponse;
      setData(json);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  }, [challengeId]);

  useEffect(() => {
    if (!enabled) return;

    void fetchOnce();

    // Poll faster when PROVISIONING, slower when RUNNING
    const intervalMs = data?.status === "RUNNING" ? RUNNING_POLL_INTERVAL_MS : POLL_INTERVAL_MS;

    const id = setInterval(() => void fetchOnce(), intervalMs);
    return () => clearInterval(id);
  }, [enabled, fetchOnce, data?.status]);

  return { data, error, loading, refetch: fetchOnce };
}
