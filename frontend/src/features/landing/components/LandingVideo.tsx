"use client";

import { useRef } from "react";
import { Box, Container, Stack } from "@mantine/core";
import { gsap } from "gsap";
import { useGSAP } from "@gsap/react";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import SectionHeader from "./parts/SectionHeader";
import { addDesktopMotion, addMobileMotion, addReducedMotion } from "../hooks/useScrollAnimations";
import { LINE_2 } from "../theme";

gsap.registerPlugin(ScrollTrigger);

const VIDEO_ID = "n-Q4wn7UH_c";
const VIDEO_START = 0;

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

      addReducedMotion(mm, () => {
        gsap.set(header, { opacity: 1, y: 0 });
        gsap.set(frame, { yPercent: 0, opacity: 1, clipPath: REVEALED });
      });

      addDesktopMotion(mm, () => {
        const tl = gsap.timeline({
          defaults: { ease: "power2.out" },
          scrollTrigger: {
            trigger: section,
            start: "top 75%",
            end: "top 15%",
            scrub: 1,
          },
        });
        tl.fromTo(header, { opacity: 0, y: 40 }, { opacity: 1, y: 0, duration: 0.3 }, 0);
        tl.fromTo(
          frame,
          { yPercent: 80, opacity: 0 },
          { yPercent: 0, opacity: 1, duration: 0.4 },
          0.35
        );
        tl.to(frame, { clipPath: REVEALED, duration: 0.3 }, 0.7);
      });

      addMobileMotion(mm, () => {
        gsap.set(frame, { clipPath: REVEALED });
        gsap.fromTo(
          header,
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
          frame,
          { opacity: 0, y: 40 },
          {
            opacity: 1,
            y: 0,
            duration: 0.7,
            ease: "power2.out",
            scrollTrigger: { trigger: section, start: "top 80%", once: true },
          }
        );
      });
    },
    { scope: sectionRef }
  );

  return (
    <Box component="section" ref={sectionRef} style={{ padding: "60px 0" }}>
      <Container size="xl" px={32}>
        <Stack gap={28} align="center">
          <SectionHeader
            innerRef={headerRef}
            kicker="$ ./play-promo.sh — 03:00"
            align="center"
            fontSize={38}
            titleStyle={{ letterSpacing: "-0.02em" }}
            style={{ opacity: 0, transform: "translateY(20px)" }}
          >
            A cybersecurity platform built by students, for students.
          </SectionHeader>

          <Box
            ref={frameRef}
            style={{
              width: "100%",
              maxWidth: "100%",
              aspectRatio: "16 / 9",
              border: `1px solid ${LINE_2}`,
              borderRadius: 16,
              overflow: "hidden",
              boxShadow: "0 30px 80px -20px rgba(0,0,0,0.6)",
              background: "#000",
              clipPath: CLIPPED,
              opacity: 0,
              transform: "translateY(30px)",
            }}
          >
            <iframe
              src={`https://www.youtube-nocookie.com/embed/${VIDEO_ID}?start=${VIDEO_START}&rel=0`}
              title="ISTP promo video"
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
