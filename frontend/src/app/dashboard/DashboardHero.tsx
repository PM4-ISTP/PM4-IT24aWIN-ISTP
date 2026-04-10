import { Box, Button, Group, Stack, Text } from "@mantine/core";
import { IconBook2, IconBolt, IconTrophy, IconChevronRight } from "@tabler/icons-react";
import WelcomeTitle from "./WelcomeTitle";
import { labelStyle } from "./_shared";

const heroStats = [
  { icon: <IconBook2 size={18} />, label: "Enrolled Courses", value: "—" },
  { icon: <IconTrophy size={18} />, label: "Completed", value: "—" },
  { icon: <IconBolt size={18} />, label: "Current Streak", value: "—" },
];

export default function DashboardHero({
  firstName,
  dateStr,
}: {
  firstName: string;
  dateStr: string;
}) {
  return (
    <Box
      style={{
        background: "linear-gradient(135deg, #060D1A 0%, #0A1628 50%, #0D1F5C 100%)",
        borderRadius: 16,
        padding: "2rem 2.5rem",
        position: "relative",
        overflow: "hidden",
      }}
    >
      {/* Dot grid overlay */}
      <Box
        style={{
          position: "absolute",
          inset: 0,
          backgroundImage: "radial-gradient(circle, rgba(0,122,255,0.18) 1px, transparent 1px)",
          backgroundSize: "24px 24px",
          pointerEvents: "none",
        }}
      />
      {/* Blue glow top-right */}
      <Box
        className="dashboard-glow-tr"
        style={{
          position: "absolute",
          top: -60,
          right: -40,
          width: 240,
          height: 240,
          borderRadius: "50%",
          background: "radial-gradient(circle, rgba(0,122,255,0.35) 0%, transparent 70%)",
          pointerEvents: "none",
        }}
      />
      {/* Cyan glow bottom-left */}
      <Box
        className="dashboard-glow-bl"
        style={{
          position: "absolute",
          bottom: -60,
          left: -40,
          width: 180,
          height: 180,
          borderRadius: "50%",
          background: "radial-gradient(circle, rgba(6,182,212,0.2) 0%, transparent 70%)",
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
          <Text style={{ ...labelStyle, color: "rgba(255,255,255,0.45)" }}>{dateStr}</Text>
          <WelcomeTitle firstName={firstName} />
          <Text size="md" style={{ color: "rgba(255,255,255,0.55)" }}>
            Here&apos;s an overview of your learning progress.
          </Text>
          <Group mt={8} gap="sm">
            <Button
              size="md"
              radius="xl"
              variant="filled"
              color="blue"
              rightSection={<IconChevronRight size={15} />}
            >
              Browse Courses
            </Button>
            <Button
              size="md"
              radius="xl"
              variant="outline"
              style={{ borderColor: "rgba(255,255,255,0.3)", color: "#fff" }}
            >
              View Leaderboard
            </Button>
          </Group>
        </Stack>

        {/* Right: inline hero stats */}
        <Stack gap="sm" className="dashboard-hero-stats" style={{ flexShrink: 0, minWidth: 220 }}>
          {heroStats.map(({ icon, label, value }) => (
            <Box
              key={label}
              style={{
                background: "rgba(255,255,255,0.06)",
                border: "1px solid rgba(255,255,255,0.1)",
                borderRadius: 12,
                padding: "0.75rem 1rem",
              }}
            >
              <Group gap="sm" wrap="nowrap">
                <Box
                  style={{
                    width: 36,
                    height: 36,
                    borderRadius: 8,
                    background: "rgba(0,122,255,0.18)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    color: "#0a84ff",
                    flexShrink: 0,
                  }}
                >
                  {icon}
                </Box>
                <Stack gap={1}>
                  <Text style={{ ...labelStyle, color: "rgba(255,255,255,0.45)" }}>{label}</Text>
                  <Text fw={700} size="lg" style={{ color: "#fff", lineHeight: 1.1 }}>
                    {value}
                  </Text>
                </Stack>
              </Group>
            </Box>
          ))}
        </Stack>
      </Group>
    </Box>
  );
}
