"use client";

import { useEffect, useMemo, useState } from "react";
import { useApiClient } from "@/src/shared/lib/api/client";
import { apiErrorText } from "@/src/shared/lib/api";
import { toUserFriendlyBackendError } from "@/src/shared/lib/userFriendlyBackendError";

type TopicOption = { value: string; label: string };

export function useCourseTopicOptions() {
  const client = useApiClient();
  const [topics, setTopics] = useState<string[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const { data, error: requestError } = await client.GET("/api/v1/courses/topics");
        if (requestError) {
          const msg = toUserFriendlyBackendError(apiErrorText(requestError));
          throw new Error(msg ?? "Failed to load topics");
        }
        if (!cancelled) {
          setTopics(data ?? []);
        }
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : String(e));
          setTopics([]);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [client]);

  const options: TopicOption[] = useMemo(
    () => (topics ?? []).map((t) => ({ value: t, label: t })),
    [topics]
  );

  return { options, loading, error };
}
