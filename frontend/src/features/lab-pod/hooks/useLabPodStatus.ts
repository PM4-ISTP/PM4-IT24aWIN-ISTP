"use client";

import { useCallback, useEffect, useState } from "react";
import { useApiClient } from "@/src/shared/lib/api/client";
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

export function useLabPodStatus(
  labId: string,
  { enabled = true }: { enabled?: boolean } = {}
): {
  data: PodStatusResponse | null;
  error: string | null;
  loading: boolean;
  refetch: () => Promise<void>;
} {
  const apiClient = useApiClient();
  const [data, setData] = useState<PodStatusResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const status = data?.status;

  const fetchOnce = useCallback(async () => {
    try {
      const { data: json, error: apiError } = await apiClient.GET("/api/v1/lab-pods/{labId}", {
        params: { path: { labId } },
      });

      if (apiError !== undefined) {
        setError("Request failed");
        return;
      }

      setData(json as PodStatusResponse);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  }, [apiClient, labId]);

  // Trigger an initial fetch (and re-fetch when labId or enabled changes)
  useEffect(() => {
    if (!enabled) return;
    void fetchOnce();
  }, [enabled, fetchOnce]);

  // Set up polling interval based on the *resolved* status, not stale closure state
  useEffect(() => {
    if (!enabled) return;
    if (!status || status === "NOT_FOUND" || status === "FAILED") return;

    const intervalMs = status === "RUNNING" ? RUNNING_POLL_INTERVAL_MS : POLL_INTERVAL_MS;
    const id = setInterval(() => void fetchOnce(), intervalMs);
    return () => clearInterval(id);
  }, [enabled, status, fetchOnce]);

  return { data, error, loading, refetch: fetchOnce };
}
