import { useRef } from "react";
import type { ReactNode } from "react";
import { Paper } from "@mantine/core";
import { gsap } from "gsap";
import { LINE } from "../../theme";
import { prefersReducedMotionNow } from "../../hooks/usePrefersReducedMotion";
import { GLOW_COLORS, type GlowTone } from "./data";

type CellProps = {
  span: number;
  rowSpan?: number;
  accent?: boolean;
  glow?: GlowTone;
  children: ReactNode;
};

export default function Cell({ span, rowSpan, accent, glow = "indigo", children }: CellProps) {
  const ref = useRef<HTMLDivElement>(null);
  const tone = GLOW_COLORS[glow];

  function handleMouseMove(e: React.MouseEvent<HTMLDivElement>) {
    const card = ref.current;
    if (!card || prefersReducedMotionNow()) return;
    const rect = card.getBoundingClientRect();
    const x = (e.clientX - rect.left) / rect.width - 0.5;
    const y = (e.clientY - rect.top) / rect.height - 0.5;
    gsap.to(card, {
      rotateY: x * 4,
      rotateX: -y * 4,
      duration: 0.45,
      ease: "power2.out",
      overwrite: "auto",
    });
  }

  function handleMouseLeave() {
    const card = ref.current;
    if (!card || prefersReducedMotionNow()) return;
    gsap.to(card, {
      rotateY: 0,
      rotateX: 0,
      duration: 0.7,
      ease: "power2.out",
      overwrite: "auto",
    });
  }

  return (
    <Paper
      ref={ref}
      className={accent ? "bento-cell bento-cell-accent" : "bento-cell"}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      p={22}
      radius={18}
      style={
        {
          position: "relative",
          gridColumn: `span ${span}`,
          gridRow: rowSpan ? `span ${rowSpan}` : undefined,
          border: accent ? "1px solid transparent" : `1px solid ${LINE}`,
          background: accent
            ? "linear-gradient(135deg,#5d6ef0 0%, #3b82f6 100%)"
            : "linear-gradient(180deg, #0d1322 0%, #0a0f1c 100%)",
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          gap: 14,
          overflow: "hidden",
          minWidth: 0,
          transformStyle: "preserve-3d",
          willChange: "transform",
          ["--glow-primary" as string]: tone.primary,
          ["--glow-secondary" as string]: tone.secondary,
          ["--glow-border" as string]: tone.border,
        } as React.CSSProperties
      }
    >
      {children}
    </Paper>
  );
}
