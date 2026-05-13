import {
  ACCENT,
  AMBER,
  INK_DIM,
  LINE_2,
  MINT,
  ROSE,
} from "../../theme";

export type ChipTone = "hot" | "warm" | "live" | "rare";

export const CHIPS: { label: string }[] = [
  { label: "SQL injection" },
  { label: "XSS" },
  { label: "IDOR" },
  { label: "broken auth" },
  { label: "broken access" },
  { label: "SSRF" },
  { label: "CSRF" },
  { label: "insecure deser" },
  { label: "file upload" },
  { label: "session" },
  { label: "API security" },
  { label: "crypto" },
  { label: "misconfig" },
  { label: "OWASP Top 10 · core" },
];

export type ChipScene = Partial<Record<number, ChipTone>>;

export const CHIP_SCENES: ChipScene[] = [
  { 0: "hot", 2: "hot", 13: "live" },
  { 3: "warm", 5: "warm", 7: "rare", 10: "live" },
  { 1: "hot", 4: "warm", 11: "rare", 13: "live" },
  { 0: "hot", 6: "warm", 7: "rare", 9: "warm", 10: "live" },
  { 2: "hot", 5: "warm", 8: "rare", 12: "warm", 13: "live" },
  { 1: "hot", 3: "warm", 7: "rare", 10: "live", 11: "warm" },
];

export const SCENE_INTERVAL_MS = 2200;

export const CHIP_PALETTES: Record<
  ChipTone | "default",
  { color: string; border: string; background: string }
> = {
  hot: {
    color: ACCENT,
    border: "rgba(93,110,240,0.4)",
    background: "rgba(93,110,240,0.1)",
  },
  warm: {
    color: AMBER,
    border: "rgba(245,180,98,0.35)",
    background: "rgba(245,180,98,0.08)",
  },
  live: {
    color: MINT,
    border: "rgba(109,240,200,0.3)",
    background: "rgba(109,240,200,0.06)",
  },
  rare: {
    color: ROSE,
    border: "rgba(240,109,138,0.35)",
    background: "rgba(240,109,138,0.08)",
  },
  default: {
    color: INK_DIM,
    border: LINE_2,
    background: "rgba(255,255,255,0.02)",
  },
};

export type GlowTone = "indigo" | "mint" | "amber" | "rose";

export const GLOW_COLORS: Record<
  GlowTone,
  { primary: string; secondary: string; border: string }
> = {
  indigo: {
    primary: "rgba(93,110,240,0.28)",
    secondary: "rgba(109,240,200,0.10)",
    border: "rgba(93,110,240,0.28)",
  },
  mint: {
    primary: "rgba(109,240,200,0.22)",
    secondary: "rgba(93,110,240,0.10)",
    border: "rgba(109,240,200,0.30)",
  },
  amber: {
    primary: "rgba(245,180,98,0.24)",
    secondary: "rgba(240,109,138,0.10)",
    border: "rgba(245,180,98,0.30)",
  },
  rose: {
    primary: "rgba(240,109,138,0.24)",
    secondary: "rgba(93,110,240,0.10)",
    border: "rgba(240,109,138,0.30)",
  },
};

export const WORKFLOW_STATS = [
  { n: "24", l: "students", w: 78 },
  { n: "9 / 12", l: "labs", w: 75 },
  { n: "87%", l: "solved", w: 87 },
  { n: "3", l: "courses", w: 62 },
];

export const ON_PREM_TAGS = ["docker compose", "kubernetes", "keycloak"];
