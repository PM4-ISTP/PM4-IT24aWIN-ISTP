"use client";

import { useRef } from "react";
import { Box, Container, Stack, Title } from "@mantine/core";
import { gsap } from "gsap";
import { useGSAP } from "@gsap/react";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import Kicker from "./parts/Kicker";
import { INK, LINE_2 } from "../theme";

gsap.registerPlugin(ScrollTrigger);

const VIDEO_ID = "3nkFtJMCs1Q";
const VIDEO_START = 0;
const INITIAL_MAX_WIDTH = 1180;

const CLIPPED = "polygon(20% 20%, 80% 20%, 80% 80%, 20% 80%)";
const REVEALED = "polygon(0% 0%, 100% 0%, 100% 100%, 0% 100%)";

export default function LandingVideo() {
  const sectionRef = useRef<HTMLElement>(null);
  const headerRef = useRef<HTMLDivElement>(null);
  const frameRef = useRef<HTMLDivElement>(null);

  useGSAP(
    () => {
      const section = sectionRef.current;
      const header = headerRef.current;
      const frame = frameRef.current;
      if (!section || !header || !frame) return;

      const mm = gsap.matchMedia();

      mm.add(
        {
          motionOk: "(prefers-reduced-motion: no-preference)",
          motionReduced: "(prefers-reduced-motion: reduce)",
        },
        (ctx) => {
          const { motionOk } = ctx.conditions as { motionOk: boolean };
          if (!motionOk) {
            gsap.set(header, { opacity: 1, y: 0 });
            gsap.set(frame, { yPercent: 0, opacity: 1, clipPath: REVEALED });
            return;
          }

          const tl = gsap.timeline({
            defaults: { ease: "power2.out" },
            scrollTrigger: {
              trigger: section,
              start: "top 75%",
              end: "top 15%",
              scrub: 1,
            },
          });
          tl.fromTo(
            header,
            { opacity: 0, y: 40 },
            { opacity: 1, y: 0, duration: 0.3 },
            0
          );
          tl.fromTo(
            frame,
            { yPercent: 80, opacity: 0 },
            { yPercent: 0, opacity: 1, duration: 0.4 },
            0.35
          );
          tl.to(frame, { clipPath: REVEALED, duration: 0.3 }, 0.7);
        }
      );
    },
    { scope: sectionRef }
  );

  return (
    <Box component="section" ref={sectionRef} style={{ padding: "60px 0" }}>
      <Container size="xl" px={32}>
        <Stack gap={28} align="center">
          <Stack
            ref={headerRef}
            gap={10}
            align="center"
            style={{ textAlign: "center" }}
          >
            <Kicker>$ ./watch-demo.sh — 02:14</Kicker>
            <Title
              order={2}
              style={{
                fontSize: 38,
                fontWeight: 600,
                letterSpacing: "-0.02em",
                margin: 0,
                color: INK,
              }}
            >
              A class, in two minutes.
            </Title>
          </Stack>

          <Box
            ref={frameRef}
            style={{
              width: "100%",
              maxWidth: INITIAL_MAX_WIDTH,
              aspectRatio: "16 / 9",
              border: `1px solid ${LINE_2}`,
              borderRadius: 16,
              overflow: "hidden",
              boxShadow: "0 30px 80px -20px rgba(0,0,0,0.6)",
              background: "#000",
              clipPath: CLIPPED,
            }}
          >
            <iframe
              src={`https://www.youtube-nocookie.com/embed/${VIDEO_ID}?start=${VIDEO_START}&rel=0`}
              title="ISTP demo video"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              allowFullScreen
              style={{
                width: "100%",
                height: "100%",
                border: 0,
                display: "block",
              }}
            />
          </Box>
        </Stack>
      </Container>
    </Box>
  );
}
