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
    const startedAt = Date.now();

    const tick = () => {
      try {
        const elapsed = Math.floor((Date.now() - startedAt) / 1000);
        const prev = Number(localStorage.getItem(STORAGE_KEY) ?? "0");
        localStorage.setItem(STORAGE_KEY, String(prev + elapsed));
        // reset startedAt baseline via closure — store running delta separately
      } catch {
        // ignore
      }
    };

    // Accumulate time continuously
    let accumulated = 0;
    let lastTick = Date.now();

    const interval = setInterval(() => {
      try {
        const now = Date.now();
        const delta = Math.floor((now - lastTick) / 1000);
        lastTick = now;
        accumulated += delta;
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
      tick();
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
    // 0.1h increments — like Steam for < 1h
    const tenths = Math.round(hours * 10) / 10;
    if (tenths >= 1) return "1h"; // rounding edge case (e.g. 3570s)
    return `${tenths.toFixed(1)}h`;
  }
  // 1h and above: round to nearest whole hour (1.2→1h, 1.5→2h)
  return `${Math.round(hours)}h`;
}
