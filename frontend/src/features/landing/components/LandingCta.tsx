"use client";

import { Box, Container, Group, Paper, Stack, Text, Title } from "@mantine/core";
import GradientText from "./parts/GradientText";
import Kicker from "./parts/Kicker";
import LandingButton from "./parts/LandingButton";
import useSignInToDashboard from "../hooks/useSignInToDashboard";
import { INK, INK_DIM, LINE_2 } from "../theme";

export default function LandingCta() {
  const handleSignIn = useSignInToDashboard();

  return (
    <Box component="section" id="cta" style={{ padding: "60px 0 100px" }}>
      <Container size="xl" px={32}>
        <Paper
          radius={24}
          p="64px 56px"
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
                href="#"
                style={{ padding: "14px 22px", fontSize: 15 }}
              >
                Read the docs ↗
              </LandingButton>
            </Group>
          </Stack>
        </Paper>
      </Container>
    </Box>
  );
}
