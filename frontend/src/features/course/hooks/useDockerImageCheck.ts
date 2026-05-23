"use client";

import { useEffect, useState } from "react";
import { useApiClient } from "@/src/shared/lib/api/client";
import { apiErrorText } from "@/src/shared/lib/api";
import { DOCKER_IMAGE_PATTERN } from "@/src/features/course/constants/challengeConstants";

export type DockerImageCheckStatus = "idle" | "checking" | "success" | "error";
type DockerImageCheckResult = {
  image: string;
  status: "success" | "error";
  message: string;
};

export function useDockerImageCheck(image: string): {
  status: DockerImageCheckStatus;
  message: string | null;
} {
  const client = useApiClient();
  const trimmed = image.trim();
  const isCheckable = DOCKER_IMAGE_PATTERN.test(trimmed);
  const [result, setResult] = useState<DockerImageCheckResult | null>(null);

  useEffect(() => {
    if (!isCheckable) {
      return;
    }

    const controller = new AbortController();
    const timeout = window.setTimeout(() => {
      void client
        .GET("/api/v1/labs/docker-image", {
          params: { query: { image: trimmed } },
          signal: controller.signal,
        })
        .then(({ data, error }) => {
          if (controller.signal.aborted) return;
          if (error) {
            const message = apiErrorText(error) ?? "Public GHCR image is not reachable";
            setResult({ image: trimmed, status: "error", message });
            return;
          }
          setResult({
            image: trimmed,
            status: "success",
            message: data?.message ?? "Public GHCR image found",
          });
        })
        .catch((error: unknown) => {
          if (controller.signal.aborted) return;
          setResult({
            image: trimmed,
            status: "error",
            message: error instanceof Error ? error.message : "Public GHCR image is not reachable",
          });
        });
    }, 600);

    return () => {
      window.clearTimeout(timeout);
      controller.abort();
    };
  }, [client, isCheckable, trimmed]);

  if (!trimmed || !isCheckable) {
    return { status: "idle", message: null };
  }
  if (result?.image === trimmed) {
    return { status: result.status, message: result.message };
  }
  return { status: "checking", message: "Checking image..." };
}
