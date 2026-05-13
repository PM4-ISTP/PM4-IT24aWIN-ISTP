"use client";

import { useEffect, useRef, useState } from "react";
import { Box, Container, Group, Title } from "@mantine/core";
import { gsap } from "gsap";
import { useGSAP } from "@gsap/react";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import GradientText from "./parts/GradientText";
import Kicker from "./parts/Kicker";
import AutoGradedCell from "./bento/cells/AutoGradedCell";
import CourseCell from "./bento/cells/CourseCell";
import DesignerCell from "./bento/cells/DesignerCell";
import OnPremCell from "./bento/cells/OnPremCell";
import OpenSourceCell from "./bento/cells/OpenSourceCell";
import WorkflowCell from "./bento/cells/WorkflowCell";
import { CHIP_SCENES, SCENE_INTERVAL_MS } from "./bento/data";
import { INK, LINE, MINT } from "../theme";

gsap.registerPlugin(ScrollTrigger);

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
            delay: i * 2,
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

        tl.fromTo(".bento-head", { opacity: 0, y: 40 }, { opacity: 1, y: 0, duration: 0.9 }, 0);

        tl.fromTo(
          ".bento-cell",
          { opacity: 0, y: 35 },
          { opacity: 1, y: 0, duration: 0.85, stagger: 0.2 },
          0.25
        );

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
          <CourseCell sceneIndex={sceneIndex} />
          <OnPremCell />
          <OpenSourceCell />
          <WorkflowCell />
          <AutoGradedCell />
          <DesignerCell />
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
