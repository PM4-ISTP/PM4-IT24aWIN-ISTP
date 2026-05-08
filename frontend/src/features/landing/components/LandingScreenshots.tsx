"use client";

import { useRef, useState } from "react";
import Image from "next/image";
import { ActionIcon, Box, Container, Group, ScrollArea, Stack, Text, Title } from "@mantine/core";
import Kicker from "./parts/Kicker";
import { FONT_MONO, INK, LINE, LINE_2, MUTED } from "../theme";

const shots = [
  {
    src: "/images/landing/Home.png",
    alt: "Home dashboard",
    url: "istp.pm4.init-lab.ch",
    label: "01 · Home dashboard",
    role: "student",
  },
  {
    src: "/images/landing/Course_catalog.png",
    alt: "Browse catalog",
    url: "istp.pm4.init-lab.ch/catalog",
    label: "02 · Browse catalog",
    role: "everyone",
  },
  {
    src: "/images/landing/Course_overview.png",
    alt: "Course overview",
    url: "istp.pm4.init-lab.ch/courses/web-security",
    label: "03 · Course overview",
    role: "student",
  },
  {
    src: "/images/landing/Course_lab.png",
    alt: "Lab view",
    url: "istp.pm4.init-lab.ch/lab/campus-helpdesk",
    label: "04 · Lab · live pod",
    role: "student",
  },
];

const SHOT_WIDTH = 720;
const SHOT_GAP = 18;
const STEP = SHOT_WIDTH + SHOT_GAP;

export default function LandingScreenshots() {
  const viewportRef = useRef<HTMLDivElement>(null);
  const [activeIndex, setActiveIndex] = useState(0);

  function scrollToIndex(idx: number) {
    const clamped = Math.min(Math.max(idx, 0), shots.length - 1);
    viewportRef.current?.scrollTo({ left: clamped * STEP, behavior: "smooth" });
  }

  function handleScrollPositionChange({ x }: { x: number }) {
    const el = viewportRef.current;
    const maxScroll = el ? el.scrollWidth - el.clientWidth : 0;
    if (maxScroll > 0 && x >= maxScroll - 4) {
      setActiveIndex(shots.length - 1);
      return;
    }
    const idx = Math.round(x / STEP);
    setActiveIndex(Math.min(Math.max(idx, 0), shots.length - 1));
  }

  return (
    <Box component="section" id="screens" style={{ padding: "140px 0 60px" }}>
      <Container size="xl" px={32}>
        <Group justify="space-between" align="flex-end" mb={32} wrap="wrap">
          <Stack gap={10}>
            <Kicker>— 04 · Product tour</Kicker>
            <Title
              order={2}
              style={{
                fontSize: 48,
                fontWeight: 600,
                letterSpacing: "-0.022em",
                margin: 0,
                color: INK,
              }}
            >
              The product, plainly.
            </Title>
          </Stack>
          <Group gap={16} align="center">
            <Text
              style={{
                fontFamily: FONT_MONO,
                fontSize: 12,
                color: MUTED,
              }}
            >
              {String(activeIndex + 1).padStart(2, "0")} / {String(shots.length).padStart(2, "0")}
            </Text>
            <ActionIcon
              variant="default"
              radius="xl"
              size={38}
              aria-label="Previous"
              onClick={() => scrollToIndex(activeIndex - 1)}
              style={{
                border: `1px solid ${LINE_2}`,
                background: "rgba(255,255,255,0.02)",
                color: INK,
              }}
            >
              ←
            </ActionIcon>
            <ActionIcon
              variant="default"
              radius="xl"
              size={38}
              aria-label="Next"
              onClick={() => scrollToIndex(activeIndex + 1)}
              style={{
                border: `1px solid ${LINE_2}`,
                background: "rgba(255,255,255,0.02)",
                color: INK,
              }}
            >
              →
            </ActionIcon>
          </Group>
        </Group>

        <ScrollArea
          viewportRef={viewportRef}
          type="never"
          offsetScrollbars={false}
          onScrollPositionChange={handleScrollPositionChange}
          styles={{
            viewport: {
              scrollSnapType: "x mandatory",
              paddingBottom: 14,
            },
          }}
        >
          <Group gap={18} wrap="nowrap" align="flex-start">
            {shots.map((s) => (
              <Box
                key={s.label}
                style={{
                  flex: `0 0 ${SHOT_WIDTH}px`,
                  scrollSnapAlign: "start",
                }}
              >
                <Box
                  style={{
                    aspectRatio: "16 / 10",
                    border: `1px solid ${LINE_2}`,
                    borderRadius: 14,
                    overflow: "hidden",
                    background: "linear-gradient(180deg,#0c1120,#070a14)",
                    boxShadow: "0 30px 80px -20px rgba(0,0,0,0.6)",
                    display: "flex",
                    flexDirection: "column",
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
                    {[0, 1, 2].map((i) => (
                      <Box
                        key={i}
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
                  <Box
                    style={{
                      position: "relative",
                      flex: 1,
                      width: "100%",
                    }}
                  >
                    <Image
                      src={s.src}
                      alt={s.alt}
                      fill
                      sizes="720px"
                      style={{ objectFit: "cover", objectPosition: "top left" }}
                    />
                  </Box>
                </Box>
                <Group justify="space-between" mt={12}>
                  <Text
                    style={{
                      fontFamily: FONT_MONO,
                      fontSize: 11,
                      color: INK,
                      fontWeight: 500,
                      letterSpacing: "0.04em",
                    }}
                  >
                    {s.label}
                  </Text>
                  <Text
                    style={{
                      fontFamily: FONT_MONO,
                      fontSize: 11,
                      color: MUTED,
                    }}
                  >
                    {s.role}
                  </Text>
                </Group>
              </Box>
            ))}
          </Group>
        </ScrollArea>
      </Container>
    </Box>
  );
}
