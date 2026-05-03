"use client";

import { useEffect } from "react";

const BASE_STORAGE_KEY = "istp_total_seconds_online";
const TICK_INTERVAL_MS = 10_000; // save every 10 seconds

function storageKey(userId: string | null): string {
  return userId ? `${BASE_STORAGE_KEY}_${userId}` : BASE_STORAGE_KEY;
}

/**
 * Invisible component — tracks time the user spends on the platform.
 * Accumulates seconds in localStorage keyed by userId. Mount once in the dashboard layout.
 */
export default function TimeTracker({ userId }: { userId: string | null }) {
  useEffect(() => {
    const key = storageKey(userId);
    let lastTick = Date.now();

    const interval = setInterval(() => {
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

    // Also save on page hide (tab switch, close)
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
      } else {
        // tab became visible again — reset lastTick
        lastTick = Date.now();
      }
    };

    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      clearInterval(interval);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
      // Final save on unmount
      try {
        const delta = Math.floor((Date.now() - lastTick) / 1000);
        const prev = Number(localStorage.getItem(key) ?? "0");
        localStorage.setItem(key, String(prev + delta));
      } catch {
        // ignore
      }
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
