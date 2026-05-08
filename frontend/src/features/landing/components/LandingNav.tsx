"use client";

import { Anchor, Box, Container, Group } from "@mantine/core";
import { INK_DIM, LINE } from "../theme";
import useSignInToDashboard from "../hooks/useSignInToDashboard";
import BrandLockup from "./parts/BrandLockup";
import LandingButton from "./parts/LandingButton";

const navLinks = [
  { label: "Courses", href: "#bento" },
  { label: "Labs", href: "#screens" },
  { label: "For Schools", href: "#cta" },
  { label: "Docs", href: "#" },
  { label: "GitHub ↗", href: "https://github.com" },
];

export default function LandingNav() {
  const handleSignIn = useSignInToDashboard();

  return (
    <Box
      component="nav"
      style={{
        position: "sticky",
        top: 0,
        zIndex: 50,
        backdropFilter: "blur(14px)",
        WebkitBackdropFilter: "blur(14px)",
        background: "rgba(6,8,15,0.72)",
        borderBottom: `1px solid ${LINE}`,
      }}
    >
      <Container size="xl" px={32}>
        <Group justify="space-between" align="center" h={64} wrap="nowrap">
          <BrandLockup subtitle="ZHAW" />

          <Group gap={28} visibleFrom="md" wrap="nowrap">
            {navLinks.map((link) => (
              <Anchor
                key={link.label}
                href={link.href}
                size="sm"
                style={{ color: INK_DIM }}
                underline="never"
              >
                {link.label}
              </Anchor>
            ))}
          </Group>

          <Group gap={10} wrap="nowrap">
            <LandingButton tone="ghost" size="sm" onClick={handleSignIn}>
              Log in
            </LandingButton>
            <LandingButton
              tone="primary"
              size="sm"
              onClick={handleSignIn}
              rightSection={<span>→</span>}
            >
              Sign up
            </LandingButton>
          </Group>
        </Group>
      </Container>
    </Box>
  );
}
