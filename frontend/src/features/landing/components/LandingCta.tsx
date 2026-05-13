"use client";

import { useRef } from "react";
import { Box, Container, Group, Paper, Stack, Text, Title } from "@mantine/core";
import { gsap } from "gsap";
import { useGSAP } from "@gsap/react";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import GradientText from "./parts/GradientText";
import GitHubIcon from "./parts/GitHubIcon";
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
    <Box
      component="section"
      ref={sectionRef}
      id="cta"
      style={{ padding: "clamp(40px, 8vw, 60px) 0 clamp(60px, 10vw, 100px)" }}
    >
      <Container size="xl" px={{ base: 16, sm: 32 }}>
        <Paper
          radius={24}
          className="cta-card"
          style={{
            position: "relative",
            padding: "clamp(28px, 6vw, 64px) clamp(20px, 5vw, 56px)",
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
                fontSize: "clamp(28px, 6.5vw, 54px)",
                letterSpacing: "-0.025em",
                fontWeight: 600,
                lineHeight: 1.05,
                color: INK,
              }}
            >
              Spin up a course. Hand out a pod.
              <br className="cta-br-desktop" />{" "}
              <GradientText>Start hacking.</GradientText>
            </Title>
            <Text
              mt={18}
              mb={28}
              style={{
                maxWidth: 520,
                color: INK_DIM,
                fontSize: "clamp(15px, 1.6vw, 18px)",
                lineHeight: 1.5,
              }}
            >
              Free for universities. On-premises in your own Kubernetes cluster — no SaaS lock-in,
              no per-seat pricing, no student data leaving campus.
            </Text>
            <Group
              className="cta-buttons"
              gap={12}
              justify="center"
              wrap="wrap"
              w="100%"
              maw={520}
            >
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
                href="https://github.com/PM4-ISTP/PM4-IT24aWIN-ISTP"
                style={{ padding: "14px 22px", fontSize: 15 }}
                leftSection={<GitHubIcon />}
              >
                Go to GitHub ↗
              </LandingButton>
            </Group>
          </Stack>
        </Paper>
      </Container>

      <style>{`
        @media (max-width: 640px) {
          .cta-br-desktop { display: none; }
          .cta-buttons { flex-direction: column; align-items: stretch; }
          .cta-buttons > * { width: 100%; justify-content: center; }
        }
      `}</style>
    </Box>
  );
}
