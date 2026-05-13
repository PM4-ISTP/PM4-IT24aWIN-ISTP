import type { gsap } from "gsap";

export const DESKTOP_BREAKPOINT_PX = 900;

export const MEDIA = {
  reduced: "(prefers-reduced-motion: reduce)",
  motion: "(prefers-reduced-motion: no-preference)",
  desktop: `(min-width: ${DESKTOP_BREAKPOINT_PX}px) and (prefers-reduced-motion: no-preference)`,
  mobile: `(max-width: ${DESKTOP_BREAKPOINT_PX - 1}px) and (prefers-reduced-motion: no-preference)`,
} as const;

type Callback = Parameters<gsap.MatchMedia["add"]>[1];

export function addReducedMotion(mm: gsap.MatchMedia, fn: Callback) {
  mm.add(MEDIA.reduced, fn);
}

export function addMotion(mm: gsap.MatchMedia, fn: Callback) {
  mm.add(MEDIA.motion, fn);
}

export function addDesktopMotion(mm: gsap.MatchMedia, fn: Callback) {
  mm.add(MEDIA.desktop, fn);
}

export function addMobileMotion(mm: gsap.MatchMedia, fn: Callback) {
  mm.add(MEDIA.mobile, fn);
}
