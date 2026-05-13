import { useSyncExternalStore } from "react";

export function prefersReducedMotionNow() {
  return (
    typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches
  );
}

function subscribe(callback: () => void) {
  if (typeof window === "undefined") return () => {};
  const mq = window.matchMedia("(prefers-reduced-motion: reduce)");
  mq.addEventListener("change", callback);
  return () => mq.removeEventListener("change", callback);
}

export default function usePrefersReducedMotion() {
  return useSyncExternalStore(subscribe, prefersReducedMotionNow, () => false);
}
