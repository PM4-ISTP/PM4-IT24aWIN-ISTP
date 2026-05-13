"use client";

import { Badge, Box, Container, Group, Stack, Text, Title } from "@mantine/core";
import GradientText from "./parts/GradientText";
import GitHubIcon from "./parts/GitHubIcon";
import LandingButton from "./parts/LandingButton";
import useSignInToDashboard from "../hooks/useSignInToDashboard";
import { FONT_MONO, INK, INK_DIM, LINE_2, MINT, MUTED } from "../theme";
import { useRef } from "react";
import { useGSAP } from "@gsap/react";
import { gsap } from "gsap";

const schools = ["ZHAW"];

export default function LandingHero() {
  const handleSignIn = useSignInToDashboard();
  const dotRef = useRef<HTMLDivElement>(null);
  const heroTextRef = useRef<HTMLDivElement>(null);

  useGSAP(
    () => {
      if (!dotRef.current) return;
      const heroText = heroTextRef.current;
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
        if (heroText) {
          gsap.fromTo(
            heroText.children,
            { opacity: 0, y: 22 },
            { opacity: 1, y: 0, duration: 0.6, stagger: 0.12, ease: "power2.out" }
          );
        }
      });
    },
    { scope: dotRef }
  );
  return (
    <Box
      component="header"
      style={{ padding: "48px 0 40px", position: "relative", minHeight: "80svh" }}
    >
      <Container size="xl" px={32}>
        <Stack
          align="center"
          gap={0}
          style={{
            textAlign: "center",
            minHeight: "calc(80svh - 88px)",
            justifyContent: "center",
          }}
        >
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

          <Box ref={heroTextRef}>
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
          </Box>

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
              leftSection={<GitHubIcon />}
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
