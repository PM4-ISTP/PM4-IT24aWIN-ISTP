"use client";

import { useRef } from "react";
import Image from "next/image";
import { Box, Container, Group, Stack, Text, Title } from "@mantine/core";
import { gsap } from "gsap";
import { useGSAP } from "@gsap/react";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import BrowserFrame from "./parts/BrowserFrame";
import Kicker from "./parts/Kicker";
import SectionHeader from "./parts/SectionHeader";
import { addDesktopMotion } from "../hooks/useScrollAnimations";
import { SCREENSHOTS } from "../content/screenshots";
import { FONT_MONO, INK, INK_DIM, LINE, MUTED } from "../theme";

// The browser-frame caps its own width by viewport height so the 16:9 card
// always fits vertically without overlapping the floating header.
const FRAME_MAX_WIDTH: React.CSSProperties = {
  maxWidth: "calc((100vh - 400px) * 16 / 9)",
  margin: "0 auto",
  width: "100%",
};

gsap.registerPlugin(ScrollTrigger);

export default function LandingScreenshots() {
  const sectionRef = useRef<HTMLElement>(null);
  const headerRef = useRef<HTMLDivElement>(null);
  const trackRef = useRef<HTMLDivElement>(null);

  useGSAP(
    () => {
      const section = sectionRef.current;
      const header = headerRef.current;
      const track = trackRef.current;
      if (!section || !header || !track) return;

      const mm = gsap.matchMedia();
      addDesktopMotion(mm, () => {
        // Header entrance — fades up as the section enters, before the pin starts
        gsap.fromTo(
          header,
          { opacity: 0, y: 40 },
          {
            opacity: 1,
            y: 0,
            ease: "power2.out",
            scrollTrigger: {
              trigger: section,
              start: "top 80%",
              end: "top 45%",
              scrub: 1,
            },
          }
        );

        // Horizontal pinned scroll — total vertical scroll is decoupled from
        // viewport width so ultra-wide screens don't have to scroll forever.
        const getDistance = () => track.scrollWidth - window.innerWidth;
        const getScrollLength = () => (SCREENSHOTS.length - 1) * window.innerHeight * 0.8;

        gsap.to(track, {
          x: () => -getDistance(),
          ease: "none",
          scrollTrigger: {
            trigger: section,
            start: "top top",
            end: () => `+=${getScrollLength()}`,
            scrub: 1,
            pin: true,
            anticipatePin: 1,
            invalidateOnRefresh: true,
          },
        });
      });
    },
    { scope: sectionRef }
  );

  return (
    <Box
      component="section"
      ref={sectionRef}
      id="screens"
      style={{ overflow: "hidden", position: "relative", marginTop: 40 }}
    >
      {/* Section header — fixed-position during pin, fades up on entrance */}
      <Box
        ref={headerRef}
        className="screens-label"
        style={{
          position: "absolute",
          top: 40,
          left: 0,
          right: 0,
          zIndex: 2,
          pointerEvents: "none",
        }}
      >
        <Container size="xl" px={32}>
          <SectionHeader kicker="— 04 · Product tour">The product, plainly.</SectionHeader>
        </Container>
      </Box>

      <Box
        ref={trackRef}
        className="screens-track"
        style={{ display: "flex", height: "100vh", willChange: "transform" }}
      >
        {SCREENSHOTS.map((s, i) => (
          <Box
            key={s.title}
            className="screens-panel"
            style={{
              flex: "0 0 100vw",
              height: "100vh",
              display: "flex",
              alignItems: "center",
              padding: "120px 0 60px",
            }}
          >
            <Container size="xl" px={32} style={{ width: "100%" }}>
              <Stack gap={24} className="screens-panel-stack">
                {/* Header row — title left, step right */}
                <Group
                  justify="space-between"
                  align="flex-end"
                  wrap="nowrap"
                  pb={14}
                  style={{ borderBottom: `1px solid ${LINE}` }}
                >
                  <Title
                    order={3}
                    style={{
                      fontSize: "clamp(28px, 3.4vw, 32px)",
                      fontWeight: 600,
                      letterSpacing: "-0.022em",
                      margin: 0,
                      color: INK,
                      lineHeight: 1.05,
                    }}
                  >
                    {s.title}
                  </Title>
                  <Text
                    style={{
                      fontFamily: FONT_MONO,
                      fontSize: 18,
                      color: INK,
                      fontWeight: 500,
                      letterSpacing: "0.04em",
                      lineHeight: 1,
                      flexShrink: 0,
                    }}
                  >
                    {String(i + 1).padStart(2, "0")}
                    <Text component="span" style={{ color: MUTED, fontSize: 14, marginLeft: 8 }}>
                      / {String(SCREENSHOTS.length).padStart(2, "0")}
                    </Text>
                  </Text>
                </Group>

                {/* Browser frame */}
                <BrowserFrame
                  url={s.url}
                  dots="muted"
                  className="screens-frame"
                  style={{ ...FRAME_MAX_WIDTH, aspectRatio: "16 / 9" }}
                >
                  <Box style={{ position: "relative", flex: 1, width: "100%" }}>
                    <Image
                      src={s.src}
                      alt={s.title}
                      fill
                      sizes="90vw"
                      style={{ objectFit: "cover", objectPosition: "top left" }}
                    />
                  </Box>
                </BrowserFrame>

                {/* Footer row — description left, role tag right */}
                <Group justify="space-between" align="flex-start" wrap="nowrap" gap={32}>
                  <Text
                    style={{
                      color: INK_DIM,
                      fontSize: 16,
                      lineHeight: 1.55,
                      maxWidth: 720,
                    }}
                  >
                    {s.description}
                  </Text>
                  <Box
                    style={{
                      borderRadius: 6,
                      flexShrink: 0,
                    }}
                  >
                    <Kicker size={11}>{s.role}</Kicker>
                  </Box>
                </Group>
              </Stack>
            </Container>
          </Box>
        ))}
      </Box>

      <style>{`
        @media (max-width: 900px), (prefers-reduced-motion: reduce) {
          .screens-label { position: relative !important; top: 0 !important; padding: 60px 0 0; }
          .screens-track {
            flex-direction: column;
            height: auto !important;
            transform: none !important;
          }
          .screens-panel {
            flex: 0 0 auto !important;
            width: 100% !important;
            height: auto !important;
            padding: 40px 0 !important;
          }
        }
      `}</style>
    </Box>
  );
}
