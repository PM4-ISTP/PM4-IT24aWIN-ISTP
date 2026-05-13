import type { CSSProperties } from "react";

export type TerminalBubble = { title: string; body: string; style: CSSProperties };

export const TERMINAL_BUBBLES: TerminalBubble[] = [
  {
    title: "Navigation",
    body: "Jump between courses and labs without losing progress.",
    style: { top: 110, left: -20, maxWidth: 220 },
  },
  {
    title: "Overview",
    body: "Lab summary and description keep students focused on the goal.",
    style: { top: 180, left: "50%", transform: "translateX(-50%)", maxWidth: 260 },
  },
  {
    title: "Status",
    body: "Live pod status and progress meters stay visible.",
    style: { top: 120, right: -10, maxWidth: 240 },
  },
];
