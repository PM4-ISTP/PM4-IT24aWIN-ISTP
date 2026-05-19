/**
 * Central design tokens for the whole app (landing page + dashboard).
 *
 * This is the single source of truth. `features/landing/theme.ts` re-exports
 * from here, so landing components keep working unchanged.
 */

/* ── Colors ──────────────────────────────────────────────────────────────── */
export const LANDING_BG = "#06080f";
export const PANEL = "#0e1322";
export const PANEL_2 = "#141b2e";
export const LINE = "rgba(255,255,255,0.08)";
export const LINE_2 = "rgba(255,255,255,0.14)";
export const INK = "#eaecf3";
export const INK_DIM = "#b8bcd0";
export const MUTED = "#7a8198";
export const ACCENT = "#5d6ef0";
export const ACCENT_2 = "#3b82f6";
export const MINT = "#6df0c8";
export const AMBER = "#f5b462";
export const ROSE = "#f06d8a";

export const GRADIENT = "linear-gradient(135deg, #5d6ef0 0%, #3b82f6 60%, #6df0c8 110%)";
export const GRADIENT_SOFT =
  "linear-gradient(135deg, rgba(93,110,240,0.16), rgba(109,240,200,0.08))";

/* ── Fonts ───────────────────────────────────────────────────────────────── */
export const FONT_SANS = "var(--font-space-grotesk), system-ui, sans-serif";
export const FONT_MONO = "var(--font-geist-mono), monospace";

/* ── App background ──────────────────────────────────────────────────────── */
/** Shared page background: landing base + subtle accent glows. */
export const APP_BG = `
  radial-gradient(900px 500px at 80% -10%, rgba(93,110,240,0.18), transparent 60%),
  radial-gradient(700px 460px at 10% 8%, rgba(109,240,200,0.06), transparent 60%),
  ${LANDING_BG}
`;

/* ── Surface tokens (cards / panels) ─────────────────────────────────────── */
export const surfaceTokens = {
  background: {
    subtle: "rgba(255,255,255,0.02)",
    default: "rgba(255,255,255,0.03)",
    strong: "rgba(255,255,255,0.04)",
  },
  border: `1px solid ${LINE}`,
  radius: {
    sm: 12,
    md: 14,
  },
  shadow: {
    sm: "0 2px 8px rgba(0,0,0,0.2)",
    md: "0 4px 24px rgba(0,0,0,0.25)",
    lg: "0 8px 32px rgba(0,0,0,0.4)",
  },
} as const;

export type SurfaceVariant = "subtle" | "default" | "strong";
export type SurfaceElevation = "sm" | "md" | "lg";
