"use client";

import { Badge, Box, Container, Group, Stack, Text, Title } from "@mantine/core";
import GradientText from "./parts/GradientText";
import LandingButton from "./parts/LandingButton";
import useSignInToDashboard from "../hooks/useSignInToDashboard";
import { FONT_MONO, INK, INK_DIM, LINE_2, MINT, MUTED } from "../theme";
import { useRef } from "react";
import { useGSAP } from "@gsap/react";
import { gsap } from "gsap";

const schools = ["ZHAW", "ZHAW", "ZHAW", "ZHAW", "ZHAW", "ZHAW", "ZHAW"];

export default function LandingHero() {
  const handleSignIn = useSignInToDashboard();
  const dotRef = useRef<HTMLDivElement>(null);

  useGSAP(
    () => {
      if (!dotRef.current) return;
      const mm = gsap.matchMedia();
      mm.add("(prefers-reduced-motion: no-preference)", () => {
        gsap.to(dotRef.current, {
          scale: 1.25,
          boxShadow: `0 0 22px ${MINT}, 0 0 6px ${MINT}`,
          repeat: -1,
          yoyo: true,
          duration: 1.4,
          ease: "sine.inOut",
        });
      });
    },
    { scope: dotRef }
  );
  return (
    <Box
      component="header"
      style={{ padding: "96px 0 40px", position: "relative", minHeight: "80vh" }}
    >
      <Container size="xl" px={32}>
        <Stack align="center" gap={0} style={{ textAlign: "center" }}>
          <Badge
            variant="outline"
            radius="xl"
            leftSection={
              <Box
                ref={dotRef}
                w={7}
                h={7}
                style={{ borderRadius: 99, background: MINT, boxShadow: `0 0 12px ${MINT}` }}
              />
            }
            styles={{
              root: {
                border: `1px solid ${LINE_2}`,
                background: "rgba(255,255,255,0.02)",
                color: INK_DIM,
                padding: "6px 14px",
                height: "auto",
                textTransform: "none",
              },
              label: {
                fontFamily: FONT_MONO,
                fontSize: 11,
                letterSpacing: "0.14em",
                textTransform: "uppercase",
              },
            }}
          >
            Open-source · v0.4 alpha
          </Badge>

          <Title
            order={1}
            mt={24}
            style={{
              fontSize: "clamp(56px, 8.6vw, 108px)",
              lineHeight: 0.98,
              fontWeight: 600,
              letterSpacing: "-0.03em",
              maxWidth: 980,
              color: INK,
            }}
          >
            Break things.
            <br />
            <GradientText>Build</GradientText> brains.
          </Title>

          <Text
            mt={24}
            style={{
              maxWidth: 640,
              fontSize: 19,
              lineHeight: 1.5,
              color: INK_DIM,
            }}
          >
            A self-hosted, open-source security training platform for universities. Each student
            gets their own isolated Kubernetes pod — exploit real vulnerabilities instead of just
            reading about them.
          </Text>

          <Group gap={12} justify="center" mt={36} wrap="wrap">
            <LandingButton
              size="md"
              onClick={handleSignIn}
              style={{ padding: "14px 22px", fontSize: 15 }}
              leftSection={<span>▶</span>}
            >
              Get started — it&apos;s free
            </LandingButton>
            <LandingButton
              tone="ghost"
              size="md"
              href="https://github.com/PM4-ISTP/PM4-IT24aWIN-ISTP"
              style={{ padding: "14px 22px", fontSize: 15 }}
              leftSection={
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 16 16"
                  fill="currentColor"
                  aria-hidden="true"
                >
                  <path d="M8 0C3.58 0 0 3.58 0 8a8 8 0 0 0 5.47 7.59c.4.07.55-.17.55-.38v-1.33c-2.23.48-2.7-1.08-2.7-1.08-.36-.92-.89-1.17-.89-1.17-.73-.5.06-.49.06-.49.8.06 1.23.83 1.23.83.72 1.23 1.88.88 2.34.67.07-.52.28-.88.51-1.08-1.78-.2-3.65-.89-3.65-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.13 0 0 .67-.21 2.2.82a7.66 7.66 0 0 1 4 0c1.53-1.04 2.2-.82 2.2-.82.44 1.11.16 1.93.08 2.13.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48v2.2c0 .21.15.46.55.38A8 8 0 0 0 16 8c0-4.42-3.58-8-8-8Z" />
                </svg>
              }
              rightSection={
                <Text
                  component="span"
                  style={{
                    fontFamily: FONT_MONO,
                    fontSize: 13,
                    color: MUTED,
                    marginLeft: 4,
                  }}
                >
                  6
                </Text>
              }
            >
              Star on GitHub
            </LandingButton>
          </Group>

          <Group
            justify="center"
            gap={48}
            mt={80}
            wrap="wrap"
            aria-label="Used at"
            style={{ opacity: 0.65 }}
          >
            {schools.map((s, i) => (
              <Group key={`${s}-${i}`} gap={48} wrap="nowrap">
                <Text
                  style={{
                    fontFamily: FONT_MONO,
                    fontSize: 12,
                    letterSpacing: "0.18em",
                    color: MUTED,
                  }}
                >
                  {s}
                </Text>
                {i < schools.length - 1 && <Text style={{ color: MUTED, opacity: 0.4 }}>·</Text>}
              </Group>
            ))}
          </Group>

        </Stack>
      </Container>
    </Box>
  );
}
