"use client";

import { useEffect, useMemo, useState } from "react";

type TopicOption = { value: string; label: string };

export function useCourseTopicOptions() {
  const [topics, setTopics] = useState<string[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const res = await fetch("/api/backend/api/v1/courses/topics", { method: "GET" });
        if (!res.ok) {
          throw new Error(`HTTP ${res.status}`);
        }
        const data = (await res.json()) as string[];
        if (!cancelled) {
          setTopics(Array.isArray(data) ? data : []);
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
  }, []);

  const options: TopicOption[] = useMemo(
    () => (topics ?? []).map((t) => ({ value: t, label: t })),
    [topics]
  );

  return { options, loading, error };
}

