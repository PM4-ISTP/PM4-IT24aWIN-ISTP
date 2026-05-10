"use client";

import { useRef } from "react";
import Image from "next/image";
import { Box, Container, Group, Stack, Text, Title } from "@mantine/core";
import { gsap } from "gsap";
import { useGSAP } from "@gsap/react";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import Kicker from "./parts/Kicker";
import { FONT_MONO, INK, INK_DIM, LINE, LINE_2, MUTED } from "../theme";

// The browser-frame caps its own width by viewport height so the 16:9 card
// always fits vertically without overlapping the floating header.
const FRAME_MAX_WIDTH: React.CSSProperties = {
  maxWidth: "calc((100vh - 400px) * 16 / 9)",
  margin: "0 auto",
  width: "100%",
};

gsap.registerPlugin(ScrollTrigger);

const shots = [
  {
    src: "/images/landing/Home.png",
    title: "Home dashboard",
    role: "student",
    description:
      "Enrolled courses, completed labs and time online — all your progress at a glance.",
    url: "istp.pm4.init-lab.ch",
  },
  {
    src: "/images/landing/Course_catalog.png",
    title: "Browse catalog",
    role: "everyone",
    description:
      "Search and filter every course running on this ISTP instance.",
    url: "istp.pm4.init-lab.ch/catalog",
  },
  {
    src: "/images/landing/Course_overview.png",
    title: "Course overview",
    role: "student",
    description:
      "All labs in a course, your per-lab progress, and what's due next — in one view.",
    url: "istp.pm4.init-lab.ch/courses/web-security",
  },
  {
    src: "/images/landing/Course_lab.png",
    title: "Lab · live pod",
    role: "student",
    description:
      "Challenge brief, a Kubernetes pod running just for you, and a flag submission box.",
    url: "istp.pm4.init-lab.ch/lab/campus-helpdesk",
  },
];

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
      mm.add(
        "(min-width: 900px) and (prefers-reduced-motion: no-preference)",
        () => {
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
          const getScrollLength = () =>
            (shots.length - 1) * window.innerHeight * 0.8;

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
        }
      );
    },
    { scope: sectionRef }
  );

  return (
    <Box
      component="section"
      ref={sectionRef}
      id="screens"
      style={{ overflow: "hidden", position: "relative", marginTop: 40}}
    >
      {/* Section header — fixed-position during pin, fades up on entrance */}
      <Box
        ref={headerRef}
        className="screens-label"
        style={{
          position: "absolute",
          top:40,
          left: 0,
          right: 0,
          zIndex: 2,
          pointerEvents: "none",
        }}
      >
        <Container size="xl" px={32}>
          <Stack gap={10}>
            <Kicker>— 04 · Product tour</Kicker>
            <Title
              order={2}
              style={{
                fontSize: 54,
                fontWeight: 600,
                letterSpacing: "-0.025em",
                margin: 0,
                color: INK,
                lineHeight: 1,
              }}
            >
              The product, plainly.
            </Title>
          </Stack>
        </Container>
      </Box>

      <Box
        ref={trackRef}
        className="screens-track"
        style={{ display: "flex", height: "100vh", willChange: "transform" }}
      >
        {shots.map((s, i) => (
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
                    <Text
                      component="span"
                      style={{ color: MUTED, fontSize: 14, marginLeft: 8 }}
                    >
                      / {String(shots.length).padStart(2, "0")}
                    </Text>
                  </Text>
                </Group>

                {/* Browser frame */}
                <Box
                  className="screens-frame"
                  style={{
                    ...FRAME_MAX_WIDTH,
                    aspectRatio: "16 / 9",
                    border: `1px solid ${LINE_2}`,
                    borderRadius: 14,
                    overflow: "hidden",
                    background: "linear-gradient(180deg,#0c1120,#070a14)",
                    boxShadow: "0 30px 80px -20px rgba(0,0,0,0.6)",
                    display: "flex",
                    flexDirection: "column",
                    minWidth: 0,
                  }}
                >
                  <Group
                    gap={6}
                    align="center"
                    px={12}
                    py={10}
                    style={{
                      borderBottom: `1px solid ${LINE}`,
                      background: "rgba(255,255,255,0.02)",
                    }}
                  >
                    {[0, 1, 2].map((d) => (
                      <Box
                        key={d}
                        w={9}
                        h={9}
                        style={{
                          borderRadius: 99,
                          background: "rgba(255,255,255,0.18)",
                        }}
                      />
                    ))}
                    <Text
                      ml={10}
                      px={10}
                      py={3}
                      style={{
                        fontFamily: FONT_MONO,
                        fontSize: 10.5,
                        color: MUTED,
                        background: "rgba(255,255,255,0.04)",
                        borderRadius: 6,
                      }}
                    >
                      {s.url}
                    </Text>
                  </Group>
                  <Box style={{ position: "relative", flex: 1, width: "100%" }}>
                    <Image
                      src={s.src}
                      alt={s.title}
                      fill
                      sizes="90vw"
                      style={{ objectFit: "cover", objectPosition: "top left" }}
                    />
                  </Box>
                </Box>

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
