"use client";

import { useState, useEffect } from "react";
import { Box, Button, Group, Stack, Text } from "@mantine/core";
import { IconBook2, IconClock, IconTrophy, IconChevronRight } from "@tabler/icons-react";
import WelcomeTitle from "./WelcomeTitle";
import { useRouter } from "next/navigation";
import TrophyCabinet from "@/src/features/badge/components/TrophyCabinet";
import { getTotalSecondsOnline, formatTimeOnline } from "./TimeTracker";

const statLabelStyle: React.CSSProperties = {
  fontFamily: "var(--font-space-grotesk), sans-serif",
  textTransform: "uppercase",
  letterSpacing: "0.1em",
  fontSize: "0.7rem",
  fontWeight: 700,
  color: "rgba(255,255,255,0.4)",
};

export default function DashboardHero({
  firstName,
  dateStr,
  enrolledCoursesCount,
  completedLabsCount,
}: {
  firstName: string;
  dateStr: string;
  enrolledCoursesCount?: number | null;
  completedLabsCount?: number | null;
}) {
  const router = useRouter();
  const [cabinetOpen, setCabinetOpen] = useState(false);
  const [timeOnline, setTimeOnline] = useState<string>("—");

  // Read time from localStorage after hydration, update every minute
  useEffect(() => {
    const update = () => setTimeOnline(formatTimeOnline(getTotalSecondsOnline()));
    update();
    const interval = setInterval(update, 60_000);
    return () => clearInterval(interval);
  }, []);

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
      <Box
        style={{
          background: "rgba(255,255,255,0.03)",
          border: "1px solid rgba(255,255,255,0.08)",
          borderRadius: 14,
          padding: "2rem 2.5rem",
          position: "relative",
          overflow: "hidden",
          boxShadow: "0 4px 24px rgba(0,0,0,0.3)",
        }}
      >
        {/* Subtle blue accent glow */}
        <Box
          style={{
            position: "absolute",
            top: -60,
            right: -30,
            width: 220,
            height: 220,
            borderRadius: "50%",
            background: "radial-gradient(circle, rgba(96,165,250,0.10) 0%, transparent 70%)",
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
            <Text style={statLabelStyle}>{dateStr}</Text>
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
              <Button
                onClick={() => router.push("/dashboard/catalog")}
                size="md"
                radius="md"
                rightSection={<IconChevronRight size={15} />}
                style={{
                  background: "linear-gradient(90deg, #2563eb, #4f46e5)",
                  border: "none",
                  fontFamily: "var(--font-space-grotesk), sans-serif",
                  fontWeight: 600,
                  letterSpacing: "0.02em",
                  boxShadow: "0 2px 12px rgba(79,70,229,0.3)",
                }}
              >
                Browse Courses
              </Button>
              <Button
                size="md"
                radius="md"
                onClick={() => setCabinetOpen(true)}
                leftSection={<span style={{ fontSize: 16 }}>🏆</span>}
                style={{
                  borderColor: "rgba(255,255,255,0.12)",
                  color: "#e2e8f0",
                  background: "rgba(255,255,255,0.04)",
                  fontFamily: "var(--font-space-grotesk), sans-serif",
                  fontWeight: 600,
                }}
                variant="outline"
              >
                My Badges
              </Button>
            </Group>
          </Stack>

          {/* Right: inline hero stats */}
          <Stack gap="sm" className="dashboard-hero-stats" style={{ flexShrink: 0, minWidth: 220 }}>
            {heroStats.map(({ icon, label, value }) => (
              <Box
                key={label}
                style={{
                  background: "rgba(255,255,255,0.04)",
                  border: "1px solid rgba(255,255,255,0.08)",
                  borderRadius: 10,
                  padding: "0.75rem 1rem",
                }}
              >
                <Group gap="sm" wrap="nowrap">
                  <Box
                    style={{
                      width: 36,
                      height: 36,
                      borderRadius: 8,
                      background: "rgba(96,165,250,0.1)",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      color: "#60a5fa",
                      flexShrink: 0,
                    }}
                  >
                    {icon}
                  </Box>
                  <Stack gap={1}>
                    <Text style={statLabelStyle}>{label}</Text>
                    <Text fw={700} size="lg" style={{ color: "#f1f5f9", lineHeight: 1.1 }}>
                      {value}
                    </Text>
                  </Stack>
                </Group>
              </Box>
            ))}
          </Stack>
        </Group>
      </Box>

      <TrophyCabinet
        opened={cabinetOpen}
        onClose={() => setCabinetOpen(false)}
        userName={firstName}
      />
    </>
  );
}
