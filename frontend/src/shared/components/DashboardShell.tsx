"use client";

import {
  AppShell,
  AppShellHeader,
  AppShellNavbar,
  AppShellMain,
  Box,
  Burger,
  Group,
} from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import UserMenu from "@/src/features/user/components/UserMenu";
import DashboardNav from "@/src/shared/components/DashboardNav";
import JoinCourseButton from "@/src/features/course/components/enrollment/JoinCourseButton";
import { ROLES } from "@/src/shared/lib/roles";
import SessionErrorHandler from "@/src/features/user/components/SessionErrorHandler";
import TimeTracker from "@/src/shared/components/TimeTracker";
import BrandLockup from "@/src/shared/components/brand/BrandLockup";

interface DashboardShellProps {
  name: string;
  roles: string[];
  image: string | null;
  userId: string | null;
  children: React.ReactNode;
}

/**
 * Client-side dashboard shell. Holds the navbar open/close state so the
 * navigation is reachable on small screens via the burger menu — the AppShell
 * cannot do this from a server component.
 */
export default function DashboardShell({
  name,
  roles,
  image,
  userId,
  children,
}: DashboardShellProps) {
  const [opened, { toggle, close }] = useDisclosure(false);

  return (
    <AppShell
      header={{ height: 60 }}
      navbar={{
        width: 224,
        breakpoint: "sm",
        collapsed: { mobile: !opened, desktop: false },
      }}
      padding="md"
    >
      <AppShellHeader
        style={{
          background: "rgba(14,19,34,0.95)",
          backdropFilter: "blur(12px)",
          borderBottom: "1px solid rgba(255,255,255,0.08)",
        }}
      >
        <Group h="100%" px="xl" justify="space-between" wrap="nowrap">
          <Group gap="sm" wrap="nowrap">
            <Burger
              opened={opened}
              onClick={toggle}
              hiddenFrom="sm"
              size="sm"
              color="#b8bcd0"
              aria-label="Toggle navigation"
            />
            <BrandLockup size={28} subtitle="ZHAW" />
          </Group>

          {/* Right side */}
          <Group gap="sm" wrap="nowrap">
            {roles.includes(ROLES.STUDENT) && (
              <Box visibleFrom="sm">
                <JoinCourseButton />
              </Box>
            )}
            <UserMenu name={name} roles={roles} image={image} />
          </Group>
        </Group>
      </AppShellHeader>

      <AppShellNavbar
        style={{
          background: "rgba(14,19,34,0.97)",
          borderRight: "1px solid rgba(255,255,255,0.06)",
        }}
      >
        <DashboardNav roles={roles} onNavigate={close} />
        {roles.includes(ROLES.STUDENT) && (
          <Box hiddenFrom="sm" p="md">
            <JoinCourseButton size="sm" fullWidth />
          </Box>
        )}
      </AppShellNavbar>

      <AppShellMain
        style={{
          background: `
            radial-gradient(900px 500px at 80% -10%, rgba(93,110,240,0.18), transparent 60%),
            radial-gradient(700px 460px at 10% 8%, rgba(109,240,200,0.06), transparent 60%),
            #06080f
          `,
          minHeight: "calc(100vh - 60px)",
          WebkitFontSmoothing: "antialiased",
          MozOsxFontSmoothing: "grayscale",
          position: "relative",
        }}
      >
        {/* Subtle 48px grid scaffolding — matches the landing page */}
        <Box
          aria-hidden
          style={{
            position: "absolute",
            inset: 0,
            pointerEvents: "none",
            backgroundImage: `
              linear-gradient(rgba(255,255,255,0.025) 1px, transparent 1px),
              linear-gradient(90deg, rgba(255,255,255,0.025) 1px, transparent 1px)
            `,
            backgroundSize: "48px 48px",
            WebkitMaskImage:
              "radial-gradient(ellipse 1100px 700px at 50% 0px, #000 30%, transparent 75%)",
            maskImage:
              "radial-gradient(ellipse 1100px 700px at 50% 0px, #000 30%, transparent 75%)",
          }}
        />

        <Box style={{ position: "relative", zIndex: 1 }}>
          <TimeTracker userId={userId} />
          <SessionErrorHandler />
          {children}
        </Box>
      </AppShellMain>
    </AppShell>
  );
}
