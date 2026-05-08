import { Badge, Box, Group, Paper, Progress, Stack, Text } from "@mantine/core";
import BrandLockup from "./parts/BrandLockup";
import Kicker from "./parts/Kicker";
import LandingButton from "./parts/LandingButton";
import {
  ACCENT,
  AMBER,
  FONT_MONO,
  FONT_SANS,
  GRADIENT,
  INK,
  INK_DIM,
  LINE,
  LINE_2,
  MINT,
  MUTED,
} from "../theme";

const navItems = [
  { label: "Home", icon: "⌂" },
  { label: "My Courses", icon: "▤", active: true },
  { label: "Browse / Catalog", icon: "▦" },
];

const challenges = [
  { name: "Recon · SQL Error Hint", done: true },
  { name: "Login Bypass", done: true },
  { name: "IDOR-Based Ticket Extraction", done: false, current: true },
  { name: "Internal Audit Trail", done: false },
  { name: "How did you find this challenge?", done: false },
];

const stats = [
  { label: "Enrolled Courses", value: "3" },
  { label: "Completed Labs", value: "12" },
  { label: "Time Online", value: "24.5h" },
];

function SidebarItem({
  icon,
  label,
  active,
}: {
  icon: string;
  label: string;
  active?: boolean;
}) {
  return (
    <Group
      gap={10}
      px={12}
      py={9}
      wrap="nowrap"
      style={{
        borderRadius: 8,
        background: active ? "rgba(93,110,240,0.12)" : "transparent",
        boxShadow: active ? `inset 2px 0 0 ${ACCENT}` : undefined,
        color: active ? INK : INK_DIM,
        fontSize: 13,
      }}
    >
      <Text style={{ color: active ? ACCENT : MUTED, fontSize: 14, lineHeight: 1 }}>
        {icon}
      </Text>
      <Text style={{ color: "inherit", fontSize: 13 }}>{label}</Text>
    </Group>
  );
}

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <Paper
      p={14}
      radius={10}
      style={{ border: `1px solid ${LINE}`, background: "transparent" }}
    >
      <Box mb={6}>
        <Kicker size={9.5} letterSpacing="0.16em">
          {label}
        </Kicker>
      </Box>
      <Text style={{ fontSize: 22, fontWeight: 600, color: INK, lineHeight: 1 }}>
        {value}
      </Text>
    </Paper>
  );
}

export default function HeroTerminal() {
  return (
    <Box
      style={{
        margin: "80px auto 0",
        maxWidth: 1080,
        position: "relative",
      }}
    >
      <Box
        style={{
          position: "absolute",
          inset: -40,
          background: GRADIENT,
          filter: "blur(60px)",
          opacity: 0.18,
          borderRadius: 40,
          zIndex: 0,
          pointerEvents: "none",
        }}
      />
      <Paper
        radius={14}
        style={{
          position: "relative",
          border: `1px solid ${LINE_2}`,
          background: "linear-gradient(180deg,#0c1120 0%, #080c18 100%)",
          boxShadow:
            "0 30px 80px -20px rgba(0,0,0,0.6), 0 0 0 1px rgba(255,255,255,0.04) inset",
          overflow: "hidden",
          zIndex: 1,
        }}
      >
        {/* Browser chrome */}
        <Group
          gap={8}
          align="center"
          px={16}
          py={12}
          style={{
            borderBottom: `1px solid ${LINE}`,
            background: "rgba(255,255,255,0.02)",
          }}
        >
          <Group gap={6}>
            {["#f06d6d", "#f5b462", "#6df0a0"].map((c) => (
              <Box key={c} w={11} h={11} style={{ borderRadius: 99, background: c }} />
            ))}
          </Group>
          <Text
            ml={12}
            px={12}
            py={3}
            style={{
              fontFamily: FONT_MONO,
              fontSize: 11.5,
              color: MUTED,
              background: "rgba(255,255,255,0.04)",
              borderRadius: 6,
            }}
          >
            istp.pm4.init-lab.ch/courses/web-security
          </Text>
        </Group>

        {/* App body */}
        <Box
          className="hero-app-body"
          style={{
            display: "grid",
            gridTemplateColumns: "200px 1fr 280px",
            minHeight: 480,
          }}
        >
          {/* Left sidebar */}
          <Stack
            gap={4}
            visibleFrom="md"
            p="22px 12px"
            style={{ borderRight: `1px solid ${LINE}` }}
          >
            <Box px={12} mb={14}>
              <BrandLockup size={22} labelSize={13} />
            </Box>
            {navItems.map((item) => (
              <SidebarItem
                key={item.label}
                icon={item.icon}
                label={item.label}
                active={item.active}
              />
            ))}
          </Stack>

          {/* Main content */}
          <Box p="22px 26px" style={{ fontFamily: FONT_SANS, color: INK_DIM, minWidth: 0 }}>
            <Group gap={8} mb={6}>
              <Kicker size={10.5} letterSpacing="0.16em">
                Course
              </Kicker>
              <Text style={{ color: MUTED }}>·</Text>
              <Text style={{ fontSize: 12, color: INK_DIM }}>Web Application Security</Text>
            </Group>
            <Text
              mb={14}
              style={{
                fontSize: 22,
                lineHeight: 1.15,
                fontWeight: 600,
                color: INK,
                letterSpacing: "-0.01em",
              }}
            >
              Course Labs
            </Text>

            {/* Lab card */}
            <Paper
              p={16}
              radius={10}
              style={{
                background: "rgba(255,255,255,0.02)",
                border: `1px solid ${LINE}`,
              }}
            >
              <Group justify="space-between" wrap="nowrap" mb={10}>
                <Group gap={10} wrap="nowrap">
                  <Box
                    w={6}
                    h={6}
                    style={{ borderRadius: 99, background: MINT, marginTop: 4 }}
                  />
                  <Text
                    style={{
                      fontSize: 15,
                      fontWeight: 600,
                      color: INK,
                      letterSpacing: "-0.01em",
                    }}
                  >
                    #1 Campus Helpdesk
                  </Text>
                </Group>
                <Group gap={6} wrap="nowrap">
                  <Badge
                    size="sm"
                    variant="light"
                    color="orange"
                    radius="sm"
                    styles={{
                      root: {
                        background: "rgba(245,180,98,0.1)",
                        color: AMBER,
                        textTransform: "none",
                        fontWeight: 500,
                      },
                    }}
                  >
                    MEDIUM
                  </Badge>
                  <Text
                    style={{
                      fontFamily: FONT_MONO,
                      fontSize: 10.5,
                      color: MUTED,
                    }}
                  >
                    Due 25.05, 11:59
                  </Text>
                </Group>
              </Group>

              <Box mb={6}>
                <Kicker size={11} letterSpacing="0.16em">
                  Progress
                </Kicker>
              </Box>
              <Progress
                value={40}
                size="sm"
                radius="xl"
                mb={14}
                styles={{
                  root: { background: "rgba(255,255,255,0.06)" },
                  section: { background: GRADIENT },
                }}
              />

              <Text
                mb={12}
                style={{ fontSize: 12.5, color: INK_DIM, lineHeight: 1.55 }}
              >
                Campus Helpdesk is an internal support web application for a school. Reconnaissance,
                authentication, IDOR access control, SSRF, insecure API endpoints — find a way in.
              </Text>

              <Box mb={6}>
                <Kicker size={9.5} letterSpacing="0.16em">
                  Challenges
                </Kicker>
              </Box>
              <Stack gap={4} mb={14}>
                {challenges.map((c, i) => (
                  <Group key={c.name} gap={10} wrap="nowrap">
                    <Box
                      w={14}
                      h={14}
                      style={{
                        borderRadius: 4,
                        border: `1px solid ${c.done ? MINT : LINE_2}`,
                        background: c.done ? "rgba(109,240,200,0.1)" : "transparent",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        flexShrink: 0,
                      }}
                    >
                      {c.done ? (
                        <Text style={{ fontSize: 9, color: MINT, lineHeight: 1 }}>✓</Text>
                      ) : null}
                    </Box>
                    <Text
                      style={{
                        fontSize: 12,
                        color: c.current ? ACCENT : c.done ? INK_DIM : MUTED,
                        textDecoration: c.done ? "line-through" : "none",
                      }}
                    >
                      {i + 1}. {c.name}
                    </Text>
                  </Group>
                ))}
              </Stack>

              <Group justify="flex-end">
                <LandingButton
                  size="xs"
                  rightSection={<span>→</span>}
                  style={{ boxShadow: "0 4px 12px -4px rgba(93,110,240,0.6)" }}
                >
                  Start
                </LandingButton>
              </Group>
            </Paper>
          </Box>

          {/* Right sidebar */}
          <Stack
            visibleFrom="md"
            gap={10}
            p="22px 16px"
            style={{
              borderLeft: `1px solid ${LINE}`,
              background: "rgba(255,255,255,0.015)",
            }}
          >
            {stats.map((s) => (
              <StatCard key={s.label} label={s.label} value={s.value} />
            ))}

            <Paper
              p={14}
              radius={10}
              style={{
                border: `1px dashed ${LINE_2}`,
                background: "transparent",
              }}
            >
              <Box mb={8}>
                <Kicker size={9.5} letterSpacing="0.16em">
                  Active Labs
                </Kicker>
              </Box>
              <Group gap={8} mb={6} wrap="nowrap">
                <Box w={6} h={6} style={{ borderRadius: 99, background: MINT }} />
                <Text style={{ fontSize: 12, color: INK }}>Campus Helpdesk</Text>
              </Group>
              <Text style={{ fontSize: 10.5, color: MUTED, fontFamily: FONT_MONO }}>
                pod ready · :8443
              </Text>
            </Paper>
          </Stack>
        </Box>
      </Paper>

      <style>{`
        @media (max-width: 900px) {
          .hero-app-body {
            grid-template-columns: 1fr !important;
          }
        }
      `}</style>
    </Box>
  );
}
