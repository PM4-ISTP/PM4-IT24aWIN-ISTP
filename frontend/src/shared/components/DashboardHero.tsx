"use client";

import { useState, useEffect } from "react";
import { Box, Group, Stack, Text } from "@mantine/core";
import { IconBook2, IconClock, IconTrophy, IconChevronRight } from "@tabler/icons-react";
import WelcomeTitle from "./WelcomeTitle";
import { useRouter } from "next/navigation";
import TrophyCabinet from "@/src/features/badge/components/TrophyCabinet";
import { getTotalSecondsOnline, formatTimeOnline } from "./TimeTracker";
import { SurfaceCard } from "@/src/shared/components/SurfaceCard";
import SectionLabel from "@/src/shared/components/SectionLabel";
import AppButton from "@/src/shared/components/AppButton";

export default function DashboardHero({
  firstName,
  dateStr,
  enrolledCoursesCount,
  completedLabsCount,
  userId,
}: {
  firstName: string;
  dateStr: string;
  enrolledCoursesCount?: number | null;
  completedLabsCount?: number | null;
  userId?: string | null;
}) {
  const router = useRouter();
  const [cabinetOpen, setCabinetOpen] = useState(false);
  const [timeOnline, setTimeOnline] = useState<string>("—");

  // Read time from localStorage after hydration, update every minute
  useEffect(() => {
    const update = () => setTimeOnline(formatTimeOnline(getTotalSecondsOnline(userId ?? null)));
    update();
    const interval = setInterval(update, 60_000);
    return () => clearInterval(interval);
  }, [userId]);

  const heroStats = [
    {
      icon: <IconBook2 size={18} />,
      label: "Enrolled Courses",
      value:
        typeof enrolledCoursesCount === "number" && Number.isFinite(enrolledCoursesCount)
          ? String(enrolledCoursesCount)
          : "—",
    },
    {
      icon: <IconTrophy size={18} />,
      label: "Completed Labs",
      value:
        typeof completedLabsCount === "number" && Number.isFinite(completedLabsCount)
          ? String(completedLabsCount)
          : "—",
    },
    {
      icon: <IconClock size={18} />,
      label: "Time Online",
      value: timeOnline,
    },
  ];

  return (
    <>
      <SurfaceCard
        variant="default"
        elevation="md"
        padding="2rem 2.5rem"
        style={{ position: "relative", overflow: "hidden" }}
      >
        {/* Subtle brand accent glow */}
        <Box
          style={{
            position: "absolute",
            top: -60,
            right: -30,
            width: 220,
            height: 220,
            borderRadius: "50%",
            background: "radial-gradient(circle, rgba(93,110,240,0.14) 0%, transparent 70%)",
            pointerEvents: "none",
          }}
        />

        <Group
          className="dashboard-hero-content"
          justify="space-between"
          align="center"
          wrap="nowrap"
          style={{ position: "relative", gap: "2rem" }}
        >
          {/* Left: welcome text + buttons */}
          <Stack gap={8} style={{ flex: 1, minWidth: 0 }}>
            <SectionLabel>{dateStr}</SectionLabel>
            <WelcomeTitle firstName={firstName} />
            <Text
              style={{
                color: "#94a3b8",
                fontSize: "1rem",
                lineHeight: 1.65,
                fontFamily: "var(--font-space-grotesk), sans-serif",
              }}
            >
              Here&apos;s an overview of your learning progress.
            </Text>
            <Group mt={8} gap="sm">
              <AppButton
                onClick={() => router.push("/dashboard/catalog")}
                size="md"
                rightSection={<IconChevronRight size={15} />}
              >
                Browse Courses
              </AppButton>
              <AppButton
                tone="ghost"
                size="md"
                onClick={() => setCabinetOpen(true)}
                leftSection={<span style={{ fontSize: 16 }}>🏆</span>}
              >
                My Badges
              </AppButton>
            </Group>
          </Stack>

          {/* Right: inline hero stats */}
          <Stack gap="sm" className="dashboard-hero-stats" style={{ flexShrink: 0, minWidth: 220 }}>
            {heroStats.map(({ icon, label, value }) => (
              <SurfaceCard
                key={label}
                variant="strong"
                elevation="sm"
                radius="sm"
                padding="0.75rem 1rem"
              >
                <Group gap="sm" wrap="nowrap">
                  <Box
                    style={{
                      width: 36,
                      height: 36,
                      borderRadius: 8,
                      background: "rgba(93,110,240,0.12)",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      color: "#5d6ef0",
                      flexShrink: 0,
                    }}
                  >
                    {icon}
                  </Box>
                  <Stack gap={1}>
                    <SectionLabel>{label}</SectionLabel>
                    <Text fw={700} size="lg" style={{ color: "#f1f5f9", lineHeight: 1.1 }}>
                      {value}
                    </Text>
                  </Stack>
                </Group>
              </SurfaceCard>
            ))}
          </Stack>
        </Group>
      </SurfaceCard>

      <TrophyCabinet
        opened={cabinetOpen}
        onClose={() => setCabinetOpen(false)}
        userName={firstName}
      />
    </>
  );
}
