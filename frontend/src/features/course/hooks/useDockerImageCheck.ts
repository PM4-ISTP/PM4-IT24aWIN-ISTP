"use client";

import { useEffect, useState } from "react";
import { readBackendError } from "@/src/shared/lib/readBackendError";
import { DOCKER_IMAGE_PATTERN } from "@/src/features/course/constants/challengeConstants";

export type DockerImageCheckStatus = "idle" | "checking" | "success" | "error";

export function useDockerImageCheck(image: string): {
  status: DockerImageCheckStatus;
  message: string | null;
} {
  const [status, setStatus] = useState<DockerImageCheckStatus>("idle");
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    const trimmed = image.trim();
    if (!trimmed || !DOCKER_IMAGE_PATTERN.test(trimmed)) {
      setStatus("idle");
      setMessage(null);
      return;
    }

    const controller = new AbortController();
    setStatus("checking");
    setMessage("Checking image...");
    const timeout = window.setTimeout(() => {
      void fetch(
        `/api/backend/api/v1/challenges/docker-image?image=${encodeURIComponent(trimmed)}`,
        {
          method: "GET",
          signal: controller.signal,
        }
      )
        .then(async (res) => {
          if (!res.ok) {
            const error = await readBackendError(res);
            throw new Error(error ?? "Docker image is not reachable");
          }
          const json = (await res.json().catch(() => null)) as { message?: unknown } | null;
          setStatus("success");
          setMessage(typeof json?.message === "string" ? json.message : "Image found");
        })
        .catch((error: unknown) => {
          if (controller.signal.aborted) return;
          setStatus("error");
          setMessage(error instanceof Error ? error.message : "Docker image is not reachable");
        });
    }, 600);

    return () => {
      window.clearTimeout(timeout);
      controller.abort();
    };
  }, [image]);

  return { status, message };
}
