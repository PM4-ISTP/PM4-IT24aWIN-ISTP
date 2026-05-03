"use client";

import { useEffect } from "react";

const BASE_STORAGE_KEY = "istp_total_seconds_online";
const SYNCED_STORAGE_KEY = "istp_synced_seconds_online";
const TICK_INTERVAL_MS = 10_000; // accumulate locally every 10 seconds
const SYNC_INTERVAL_MS = 60_000; // sync to backend every 60 seconds

function storageKey(userId: string | null): string {
  return userId ? `${BASE_STORAGE_KEY}_${userId}` : BASE_STORAGE_KEY;
}

function syncedKey(userId: string | null): string {
  return userId ? `${SYNCED_STORAGE_KEY}_${userId}` : SYNCED_STORAGE_KEY;
}

/** Sends the unsynced delta (currentTotal - lastSyncedTotal) to the backend. */
async function syncToBackend(userId: string | null): Promise<void> {
  if (!userId) return;
  try {
    const current = Number(localStorage.getItem(storageKey(userId)) ?? "0");
    const synced = Number(localStorage.getItem(syncedKey(userId)) ?? "0");
    const delta = current - synced;
    if (delta <= 0) return;
    const res = await fetch("/api/backend/api/v1/users/me/online-time", {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ seconds: Math.min(delta, 3600) }),
    });
    if (res.ok) {
      localStorage.setItem(syncedKey(userId), String(current));
    }
  } catch {
    // network failures are silently ignored — will retry on next sync
  }
}

/** Fetches the server-stored total and seeds localStorage if the server has a higher value. */
async function seedFromServer(userId: string | null): Promise<void> {
  if (!userId) return;
  try {
    const res = await fetch("/api/backend/api/v1/users/me/profile", { cache: "no-store" });
    if (!res.ok) return;
    const data = (await res.json()) as { totalSecondsOnline?: number };
    const serverTotal = typeof data.totalSecondsOnline === "number" ? data.totalSecondsOnline : 0;
    const localTotal = Number(localStorage.getItem(storageKey(userId)) ?? "0");
    if (serverTotal > localTotal) {
      // Server has more time (e.g. from another device) — adopt it
      localStorage.setItem(storageKey(userId), String(serverTotal));
      localStorage.setItem(syncedKey(userId), String(serverTotal));
    } else {
      // Local has unsynchronised delta — just record what the server knows as the sync baseline
      const localSynced = Number(localStorage.getItem(syncedKey(userId)) ?? "0");
      if (serverTotal > localSynced) {
        localStorage.setItem(syncedKey(userId), String(serverTotal));
      }
    }
  } catch {
    // ignore — tracker will still work locally
  }
}

/**
 * Invisible component — tracks time the user spends on the platform.
 * Accumulates seconds in localStorage keyed by userId. Mount once in the dashboard layout.
 * Periodically syncs the accumulated delta to the backend so time is account-based.
 */
export default function TimeTracker({ userId }: { userId: string | null }) {
  useEffect(() => {
    // Seed from server on mount so we pick up time from other devices
    void seedFromServer(userId);

    const key = storageKey(userId);
    let lastTick = Date.now();

    const tickInterval = setInterval(() => {
      try {
        const now = Date.now();
        const delta = Math.floor((now - lastTick) / 1000);
        lastTick = now;
        const prev = Number(localStorage.getItem(key) ?? "0");
        localStorage.setItem(key, String(prev + delta));
      } catch {
        // ignore
      }
    }, TICK_INTERVAL_MS);

    const syncInterval = setInterval(() => {
      void syncToBackend(userId);
    }, SYNC_INTERVAL_MS);

    // Also save and sync on tab switch / close
    const handleVisibilityChange = () => {
      if (document.visibilityState === "hidden") {
        try {
          const now = Date.now();
          const delta = Math.floor((now - lastTick) / 1000);
          lastTick = now;
          const prev = Number(localStorage.getItem(key) ?? "0");
          localStorage.setItem(key, String(prev + delta));
        } catch {
          // ignore
        }
        void syncToBackend(userId);
      } else {
        // tab became visible again — reset lastTick
        lastTick = Date.now();
      }
    };

    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      clearInterval(tickInterval);
      clearInterval(syncInterval);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
      // Final local save on unmount
      try {
        const delta = Math.floor((Date.now() - lastTick) / 1000);
        const prev = Number(localStorage.getItem(key) ?? "0");
        localStorage.setItem(key, String(prev + delta));
      } catch {
        // ignore
      }
      void syncToBackend(userId);
    };
  }, [userId]);

  return null;
}

export function getTotalSecondsOnline(userId: string | null = null): number {
  try {
    return Number(localStorage.getItem(storageKey(userId)) ?? "0");
  } catch {
    return 0;
  }
}

export function formatTimeOnline(seconds: number): string {
  const hours = seconds / 3600;
  if (hours < 1) {
    // Under 1h: show one decimal place (0.1h steps)
    const tenths = Math.round(hours * 10) / 10;
    if (tenths >= 1) return "1h"; // edge case: rounds up to exactly 1h
    return `${tenths.toFixed(1)}h`;
  }
  // 1h and above: round to nearest whole hour
  return `${Math.round(hours)}h`;
}

