"use client";

import { useEffect, useRef, useState } from "react";
import { Box, Container, Group, Paper, Progress, Stack, Text, Title } from "@mantine/core";
import { gsap } from "gsap";
import { useGSAP } from "@gsap/react";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import GradientText from "./parts/GradientText";
import Kicker from "./parts/Kicker";
import {
  ACCENT,
  AMBER,
  FONT_MONO,
  GRADIENT,
  INK,
  INK_DIM,
  LINE,
  LINE_2,
  MINT,
  MUTED,
  ROSE,
} from "../theme";

gsap.registerPlugin(ScrollTrigger);

type ChipTone = "hot" | "warm" | "live" | "rare";
const chips: { label: string }[] = [
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

type ChipScene = Partial<Record<number, ChipTone>>;

const CHIP_SCENES: ChipScene[] = [
  { 0: "hot", 2: "hot", 13: "live" },
  { 3: "warm", 5: "warm", 7: "rare", 10: "live" },
  { 1: "hot", 4: "warm", 11: "rare", 13: "live" },
  { 0: "hot", 6: "warm", 7: "rare", 9: "warm", 10: "live" },
  { 2: "hot", 5: "warm", 8: "rare", 12: "warm", 13: "live" },
  { 1: "hot", 3: "warm", 7: "rare", 10: "live", 11: "warm" },
];

const SCENE_INTERVAL_MS = 2200;

const CHIP_PALETTES: Record<
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

function Chip({ label, tone }: { label: string; tone?: ChipTone }) {
  const palette = CHIP_PALETTES[tone ?? "default"];
  return (
    <Box
      component="span"
      px={10}
      py={5}
      style={{
        fontFamily: FONT_MONO,
        fontSize: 11.5,
        borderRadius: 6,
        border: `1px solid ${palette.border}`,
        color: palette.color,
        background: palette.background,
        transition: "color 0.6s ease, border-color 0.6s ease, background 0.6s ease",
      }}
    >
      {label}
    </Box>
  );
}

type GlowTone = "indigo" | "mint" | "amber" | "rose";

const GLOW_COLORS: Record<GlowTone, { primary: string; secondary: string; border: string }> = {
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

function Cell({
  span,
  rowSpan,
  accent,
  glow = "indigo",
  children,
}: {
  span: number;
  rowSpan?: number;
  accent?: boolean;
  glow?: GlowTone;
  children: React.ReactNode;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const tone = GLOW_COLORS[glow];

  function prefersReducedMotion() {
    return (
      typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches
    );
  }

  function handleMouseMove(e: React.MouseEvent<HTMLDivElement>) {
    const card = ref.current;
    if (!card || prefersReducedMotion()) return;
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
    if (!card || prefersReducedMotion()) return;
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

function CellHead({
  tag,
  title,
  description,
  accent,
  big,
}: {
  tag: string;
  title: string;
  description: string;
  accent?: boolean;
  big?: boolean;
}) {
  return (
    <Box>
      <Box mb={8}>
        <Kicker size={10} style={accent ? { color: "rgba(255,255,255,0.85)" } : undefined}>
          {tag}
        </Kicker>
      </Box>
      <Text
        style={{
          fontSize: big ? 30 : 20,
          lineHeight: big ? 1.1 : 1.2,
          fontWeight: 600,
          letterSpacing: "-0.01em",
          margin: "10px 0 6px",
          color: accent ? "#fff" : INK,
        }}
      >
        {title}
      </Text>
      <Text
        style={{
          margin: 0,
          fontSize: big ? 15 : 13.5,
          color: accent ? "rgba(255,255,255,0.85)" : INK_DIM,
          lineHeight: 1.5,
          maxWidth: big ? 460 : undefined,
        }}
      >
        {description}
      </Text>
    </Box>
  );
}

function CodePeek({ children, inverted }: { children: React.ReactNode; inverted?: boolean }) {
  return (
    <Box
      className="code-peek"
      px={11}
      py={9}
      style={{
        fontFamily: FONT_MONO,
        fontSize: 11,
        background: inverted ? "rgba(0,0,0,0.18)" : "rgba(0,0,0,0.3)",
        border: `1px solid ${inverted ? "rgba(255,255,255,0.22)" : LINE}`,
        borderRadius: 8,
        lineHeight: 1.6,
        color: inverted ? "rgba(255,255,255,0.85)" : MUTED,
      }}
    >
      {children}
    </Box>
  );
}

export default function LandingBento() {
  const sectionRef = useRef<HTMLElement>(null);
  const [sceneIndex, setSceneIndex] = useState(0);

  // Cycle chip-tone scenes while the bento section is in viewport
  useEffect(() => {
    const el = sectionRef.current;
    if (!el) return;
    let intervalId: ReturnType<typeof setInterval> | null = null;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          if (intervalId) return;
          intervalId = setInterval(() => {
            setSceneIndex((i) => (i + 1) % CHIP_SCENES.length);
          }, SCENE_INTERVAL_MS);
        } else if (intervalId) {
          clearInterval(intervalId);
          intervalId = null;
        }
      },
      { threshold: 0.15 }
    );
    observer.observe(el);
    return () => {
      observer.disconnect();
      if (intervalId) clearInterval(intervalId);
    };
  }, []);

  useGSAP(
    () => {
      const section = sectionRef.current;
      if (!section) return;
      const mm = gsap.matchMedia();

      mm.add("(prefers-reduced-motion: no-preference)", () => {
        gsap.utils.toArray(".pulse-items").forEach((item, i) => {
          gsap.from(item as HTMLElement, {
            boxShadow: `0 0 16px ${MINT}, 0 0 6px ${MINT}`,
            repeat: -1,
            yoyo: true,
            scale: 1.2,
            duration: 1.5,
            ease: "sine.inOut",
            delay: i * 2, // Stagger based on index
          });
        });
      });

      mm.add("(min-width: 900px) and (prefers-reduced-motion: no-preference)", () => {
        const tl = gsap.timeline({
          defaults: { ease: "power2.out" },
          scrollTrigger: {
            trigger: section,
            start: "top 85%",
            end: "top 15%",
            scrub: 1.2,
          },
        });

        // Phase 1 — title block fades up
        tl.fromTo(".bento-head", { opacity: 0, y: 40 }, { opacity: 1, y: 0, duration: 0.9 }, 0);

        // Phase 2 — cards stagger in (slower stagger, gentle slide)
        tl.fromTo(
          ".bento-cell",
          { opacity: 0, y: 35 },
          { opacity: 1, y: 0, duration: 0.85, stagger: 0.2 },
          0.25
        );

        // Phase 3 — CodePeeks clip-reveal left to right (typewriter feel)
        tl.fromTo(
          ".code-peek",
          { clipPath: "inset(0% 100% 0% 0%)" },
          { clipPath: "inset(0% 0% 0% 0%)", duration: 1.1, stagger: 0.25 },
          0.9
        );
      });

      mm.add("(max-width: 899px) and (prefers-reduced-motion: no-preference)", () => {
        gsap.fromTo(
          ".bento-head",
          { opacity: 0, y: 30 },
          {
            opacity: 1,
            y: 0,
            duration: 0.6,
            ease: "power2.out",
            scrollTrigger: { trigger: section, start: "top 85%", once: true },
          }
        );
        gsap.fromTo(
          ".bento-cell",
          { opacity: 0, y: 25 },
          {
            opacity: 1,
            y: 0,
            duration: 0.55,
            stagger: 0.1,
            ease: "power2.out",
            scrollTrigger: { trigger: section, start: "top 80%", once: true },
          }
        );
        gsap.set(".code-peek", { clipPath: "inset(0% 0% 0% 0%)" });
      });
    },
    { scope: sectionRef }
  );

  return (
    <Box component="section" id="bento" ref={sectionRef} style={{ padding: "80px 0 40px" }}>
      <Container size="xl" px={32}>
        <Group
          className="bento-head"
          justify="space-between"
          align="flex-end"
          pb={18}
          mb={32}
          style={{ borderBottom: `1px solid ${LINE}` }}
        >
          <Title
            order={2}
            style={{
              fontSize: 54,
              fontWeight: 600,
              letterSpacing: "-0.025em",
              margin: 0,
              lineHeight: 1,
              color: INK,
            }}
          >
            What you <GradientText>get</GradientText>.
          </Title>
          <Box visibleFrom="sm">
            <Kicker>Chapter · 03 / Features</Kicker>
          </Box>
        </Group>

        <Box
          className="bento-grid"
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(12, 1fr)",
            gridAutoRows: "minmax(260px, auto)",
            gap: 14,
            perspective: 1400,
          }}
        >
          {/* 01 BIG — Courses, labs, challenges */}
          <Cell span={7} rowSpan={2} glow="indigo">
            <CellHead
              tag="— 01"
              title="Courses, labs, challenges."
              description="A course holds multiple labs. Each lab spins up its own pod from a Docker image. Stack flag or multiple-choice challenges on top — every student in their own sandbox."
              big
            />
            <Group gap={6} style={{ flexWrap: "wrap", maxWidth: 560 }}>
              {chips.map((c, i) => (
                <Chip key={c.label} label={c.label} tone={CHIP_SCENES[sceneIndex][i]} />
              ))}
            </Group>
          </Cell>

          {/* 02 deploy */}
          <Cell span={5} glow="mint">
            <CellHead
              tag="— 02"
              title="On-premises by design."
              description="Runs on your own Kubernetes cluster. No external SaaS, no per-seat pricing, no student data leaving campus."
            />
            <Group
              gap={6}
              style={{ flexWrap: "wrap", fontFamily: FONT_MONO, fontSize: 10.5, color: MUTED }}
            >
              {["docker compose", "kubernetes", "keycloak"].map((p) => (
                <Group
                  key={p}
                  gap={6}
                  px={9}
                  py={5}
                  align="center"
                  style={{ border: `1px solid ${LINE_2}`, borderRadius: 6 }}
                >
                  <Box
                    className="pulse-items"
                    w={6}
                    h={6}
                    style={{ borderRadius: 99, background: MINT, boxShadow: `0 0 12px ${MINT}` }}
                  />
                  <Text style={{ fontFamily: FONT_MONO, fontSize: 10.5, color: MUTED }}>{p}</Text>
                </Group>
              ))}
            </Group>
          </Cell>

          {/* 03 Open-source (accent) */}
          <Cell span={5} accent glow="indigo">
            <CellHead
              tag="— 03"
              title="Open-source. No catch."
              description="Fork it. Brand it. Translate it. Run a hundred instances. We'd love a PR back, but you don't owe us one."
              accent
            />
            <CodePeek inverted>
              ${" "}
              <Text component="b" style={{ color: "#fff", fontWeight: 500 }}>
                git clone
              </Text>{" "}
              <Text component="span" style={{ color: "#dfe7ff" }}>
                github.com/PM4-ISTP/istp
              </Text>
              <br />${" "}
              <Text component="b" style={{ color: "#fff", fontWeight: 500 }}>
                docker compose
              </Text>{" "}
              up -d
            </CodePeek>
          </Cell>

          {/* 04 classroom */}
          <Cell span={4} glow="amber">
            <CellHead
              tag="— 04"
              title="Built for academic workflows."
              description="Three roles — student, instructor, admin. University email-domain sign-up, per-course progress, no spreadsheets."
            />
            <Box
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: 8,
              }}
            >
              {[
                { n: "24", l: "students", w: 78 },
                { n: "9 / 12", l: "labs", w: 75 },
                { n: "87%", l: "solved", w: 87 },
                { n: "3", l: "courses", w: 62 },
              ].map((s) => (
                <Stack
                  key={s.l}
                  gap={2}
                  px={10}
                  py={9}
                  style={{ border: `1px solid ${LINE}`, borderRadius: 8 }}
                >
                  <Text
                    style={{ fontFamily: FONT_MONO, fontSize: 16, color: INK, fontWeight: 600 }}
                  >
                    {s.n}
                  </Text>
                  <Text
                    style={{
                      fontSize: 10,
                      color: MUTED,
                      textTransform: "uppercase",
                      letterSpacing: "0.12em",
                    }}
                  >
                    {s.l}
                  </Text>
                  <Progress
                    value={s.w}
                    size={3}
                    mt={4}
                    radius="xl"
                    styles={{
                      root: { background: "rgba(255,255,255,0.06)" },
                      section: { background: GRADIENT },
                    }}
                  />
                </Stack>
              ))}
            </Box>
          </Cell>

          {/* 05 auto-graded */}
          <Cell span={4} glow="mint">
            <CellHead
              tag="— 05"
              title="Auto-graded, instantly scored."
              description="Flags and multiple-choice answers grade themselves. Pods auto-terminate after 60 minutes idle to free up the cluster."
            />
            <CodePeek>
              <Text component="span" style={{ color: INK_DIM }}>
                # POST /api/submissions
              </Text>
              <br />✓ flag ·{" "}
              <Text component="b" style={{ color: MINT, fontWeight: 500 }}>
                matches
              </Text>
              <br />✓ score ·{" "}
              <Text component="span" style={{ color: ACCENT }}>
                +400 pts
              </Text>
              <br />
              <Text component="span" style={{ color: AMBER }}>
                ⏲ pod idle · 58m left
              </Text>
            </CodePeek>
          </Cell>

          {/* 06 BYOC */}
          <Cell span={4} glow="rose">
            <CellHead
              tag="— 06"
              title="Course, lab & challenge designer."
              description="Pick a Docker image, write a description, add flag or multiple-choice challenges. Done. The platform handles pods, scoring and lifecycle."
            />
            <CodePeek>
              <Text component="span" style={{ color: INK_DIM }}>
                # new lab
              </Text>
              <br />
              <Text component="span" style={{ color: ACCENT }}>
                image:
              </Text>{" "}
              <Text component="b" style={{ color: MINT, fontWeight: 500 }}>
                ghcr.io/school/sql-inject:1.0
              </Text>
              <br />
              <Text component="span" style={{ color: ACCENT }}>
                challenges:
              </Text>{" "}
              5{" "}
              <Text component="span" style={{ color: MUTED }}>
                ·
              </Text>{" "}
              <Text component="span" style={{ color: ACCENT }}>
                port:
              </Text>{" "}
              8080
            </CodePeek>
          </Cell>
        </Box>
      </Container>

      <style>{`
        .bento-cell {
          box-shadow: 0 0 0 0 rgba(0,0,0,0);
          transition: box-shadow 0.6s ease, border-color 0.4s ease;
          z-index: 1;
        }
        /* While any cell in the grid is hovered, lift the non-hovered cells
           above it so the hovered one's glow tucks under the neighbours. */
        .bento-grid:hover .bento-cell { z-index: 3; }
        .bento-grid:hover .bento-cell:hover { z-index: 2; }
        .bento-cell:hover {
          box-shadow:
            0 30px 90px -25px var(--glow-primary),
            0 0 120px -35px var(--glow-secondary),
            0 0 0 1px var(--glow-border) inset;
          border-color: var(--glow-border);
        }
        .bento-cell-accent:hover {
          box-shadow:
            0 30px 90px -25px var(--glow-primary),
            0 0 130px -30px var(--glow-secondary);
        }
        @media (max-width: 900px) {
          .bento-grid {
            grid-template-columns: repeat(6, 1fr) !important;
            grid-auto-rows: auto !important;
          }
          .bento-grid > * {
            grid-column: span 6 !important;
            grid-row: auto !important;
          }
        }
        @media (prefers-reduced-motion: reduce) {
          .bento-cell { transition: none; }
        }
      `}</style>
    </Box>
  );
}
