"use client";

import { useCallback, useEffect, useState } from "react";
import { useDebouncedCallback } from "@mantine/hooks";
import { readBackendError } from "@/src/shared/lib/readBackendError";

type PageResponse<T> = {
  content?: T[];
  totalPages?: number;
};

type Params = {
  endpoint: string;
  label: string;
  pageSize: number;
  sort?: string;
  debounceMs?: number;
};

export function useAdminPagedList<T>({
  endpoint,
  label,
  pageSize,
  sort = "updatedAt,desc",
  debounceMs = 300,
}: Params) {
  const [query, setQuery] = useState("");
  const [activeQuery, setActiveQuery] = useState("");
  const [page, setPage] = useState(0);
  const [items, setItems] = useState<T[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reloadSeq, setReloadSeq] = useState(0);

  const debouncedApplyQuery = useDebouncedCallback((next: string) => {
    setPage(0);
    setActiveQuery(next.trim());
  }, debounceMs);

  const onQueryChange = useCallback(
    (next: string) => {
      setQuery(next);
      debouncedApplyQuery(next);
    },
    [debouncedApplyQuery]
  );

  const refresh = useCallback(() => setReloadSeq((s) => s + 1), []);

  useEffect(() => {
    const controller = new AbortController();

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const url = new URL(endpoint, window.location.origin);
        if (activeQuery) url.searchParams.set("q", activeQuery);
        url.searchParams.set("page", String(page));
        url.searchParams.set("size", String(pageSize));
        if (sort) url.searchParams.set("sort", sort);

        const res = await fetch(url.toString(), { method: "GET", signal: controller.signal });
        if (!res.ok) {
          const msg = await readBackendError(res);
          setError(`Failed to load ${label} (HTTP ${res.status})${msg ? ` — ${msg}` : ""}`);
          return;
        }
        const data = (await res.json()) as PageResponse<T>;
        setItems(data.content ?? []);
        setTotalPages(data.totalPages ?? 0);
      } catch (e) {
        if ((e as { name?: string }).name === "AbortError") return;
        setError(`Failed to load ${label}`);
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void load();
    return () => controller.abort();
  }, [endpoint, activeQuery, page, pageSize, sort, reloadSeq, label]);

  return {
    query,
    onQueryChange,
    page,
    setPage,
    items,
    totalPages,
    loading,
    error,
    setError,
    refresh,
  };
}
