"use client";

import { useEffect, useState } from "react";
import { readBackendError } from "@/src/shared/lib/readBackendError";
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
  const trimmed = image.trim();
  const isCheckable = DOCKER_IMAGE_PATTERN.test(trimmed);
  const [result, setResult] = useState<DockerImageCheckResult | null>(null);

  useEffect(() => {
    if (!isCheckable) {
      return;
    }

    const controller = new AbortController();
    const timeout = window.setTimeout(() => {
      void fetch(
        `/api/backend/api/v1/labs/docker-image?image=${encodeURIComponent(trimmed)}`,
        {
          method: "GET",
          signal: controller.signal,
        }
      )
        .then(async (res) => {
          if (!res.ok) {
            const error = await readBackendError(res);
            throw new Error(error ?? "Public GHCR image is not reachable");
          }
          const json = (await res.json().catch(() => null)) as { message?: unknown } | null;
          setResult({
            image: trimmed,
            status: "success",
            message: typeof json?.message === "string" ? json.message : "Public GHCR image found",
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
  }, [isCheckable, trimmed]);

  if (!trimmed || !isCheckable) {
    return { status: "idle", message: null };
  }
  if (result?.image === trimmed) {
    return { status: result.status, message: result.message };
  }
  return { status: "checking", message: "Checking image..." };
}
