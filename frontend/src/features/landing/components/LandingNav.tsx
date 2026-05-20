"use client";

import { Anchor, Box, Container, Group } from "@mantine/core";
import { INK_DIM, LINE } from "../theme";
import useSignInToDashboard from "../hooks/useSignInToDashboard";
import BrandLockup from "@/src/shared/components/brand/BrandLockup";
import AppButton from "@/src/shared/components/AppButton";

const navLinks = [
  { label: "Features", href: "#bento" },
  { label: "Product", href: "#screens" },
  { label: "For Schools", href: "#cta" },
  { label: "GitHub ↗", href: "https://github.com/PM4-ISTP/PM4-IT24aWIN-ISTP" },
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
            <AppButton
              tone="primary"
              size="sm"
              onClick={handleSignIn}
              rightSection={<span>→</span>}
            >
              Login
            </AppButton>
          </Group>
        </Group>
      </Container>
    </Box>
  );
}
