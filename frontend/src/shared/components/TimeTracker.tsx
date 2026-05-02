"use client";

import { useEffect } from "react";

const STORAGE_KEY = "istp_total_seconds_online";
const TICK_INTERVAL_MS = 10_000; // save every 10 seconds

/**
 * Invisible component — tracks time the user spends on the platform.
 * Accumulates seconds in localStorage. Mount once in the dashboard layout.
 */
export default function TimeTracker() {
  useEffect(() => {
    let lastTick = Date.now();

    const interval = setInterval(() => {
      try {
        const now = Date.now();
        const delta = Math.floor((now - lastTick) / 1000);
        lastTick = now;
        const prev = Number(localStorage.getItem(STORAGE_KEY) ?? "0");
        localStorage.setItem(STORAGE_KEY, String(prev + delta));
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
          const prev = Number(localStorage.getItem(STORAGE_KEY) ?? "0");
          localStorage.setItem(STORAGE_KEY, String(prev + delta));
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
        const prev = Number(localStorage.getItem(STORAGE_KEY) ?? "0");
        localStorage.setItem(STORAGE_KEY, String(prev + delta));
      } catch {
        // ignore
      }
    };
  }, []);

  return null;
}

export function getTotalSecondsOnline(): number {
  try {
    return Number(localStorage.getItem(STORAGE_KEY) ?? "0");
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
