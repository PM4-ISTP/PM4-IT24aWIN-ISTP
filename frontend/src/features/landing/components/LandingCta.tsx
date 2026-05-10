"use client";

import { useRef } from "react";
import { Box, Container, Group, Paper, Stack, Text, Title } from "@mantine/core";
import { gsap } from "gsap";
import { useGSAP } from "@gsap/react";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import GradientText from "./parts/GradientText";
import Kicker from "./parts/Kicker";
import LandingButton from "./parts/LandingButton";
import useSignInToDashboard from "../hooks/useSignInToDashboard";
import { INK, INK_DIM, LINE_2 } from "../theme";

gsap.registerPlugin(ScrollTrigger);

const CLIPPED = "inset(20% 20% 20% 20% round 24px)";
const REVEALED = "inset(0% 0% 0% 0% round 24px)";

export default function LandingCta() {
  const handleSignIn = useSignInToDashboard();
  const sectionRef = useRef<HTMLElement>(null);

  useGSAP(
    () => {
      const section = sectionRef.current;
      if (!section) return;

      const mm = gsap.matchMedia();
      mm.add("(prefers-reduced-motion: no-preference)", () => {
        gsap.fromTo(
          ".cta-card",
          { clipPath: CLIPPED, scale: 0.85 },
          {
            clipPath: REVEALED,
            scale: 1,
            scrollTrigger: {
              trigger: section,
              start: "top bottom",
              end: "top 45%",
              scrub: 1.2,
            },
          }
        );
      });
    },
    { scope: sectionRef }
  );

  return (
    <Box component="section" ref={sectionRef} id="cta" style={{ padding: "60px 0 100px" }}>
      <Container size="xl" px={32}>
        <Paper
          radius={24}
          p="64px 56px"
          className="cta-card"
          style={{
            position: "relative",
            border: `1px solid ${LINE_2}`,
            background:
              "radial-gradient(700px 300px at 100% 0%, rgba(93,110,240,0.25), transparent 60%), linear-gradient(180deg,#0d1426 0%, #080c18 100%)",
            overflow: "hidden",
            textAlign: "center",
          }}
        >
          <Stack align="center" gap={0}>
            <Box mb={18}>
              <Kicker>— Try it now</Kicker>
            </Box>
            <Title
              order={2}
              style={{
                margin: 0,
                fontSize: 54,
                letterSpacing: "-0.025em",
                fontWeight: 600,
                lineHeight: 1.05,
                color: INK,
              }}
            >
              Spin up a course. Hand out a pod.
              <br />
              <GradientText>Start hacking.</GradientText>
            </Title>
            <Text
              mt={18}
              mb={28}
              style={{
                maxWidth: 520,
                color: INK_DIM,
                fontSize: 18,
                lineHeight: 1.5,
              }}
            >
              Free for universities. On-premises in your own Kubernetes cluster — no SaaS lock-in,
              no per-seat pricing, no student data leaving campus.
            </Text>
            <Group gap={12} justify="center" wrap="wrap">
              <LandingButton
                size="md"
                onClick={handleSignIn}
                style={{ padding: "14px 22px", fontSize: 15 }}
              >
                Sign up — it&apos;s free
              </LandingButton>
              <LandingButton
                tone="ghost"
                size="md"
                component="a"
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
              >
                Go to GitHub ↗
              </LandingButton>
            </Group>
          </Stack>
        </Paper>
      </Container>
    </Box>
  );
}
