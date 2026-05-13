import { Badge, Box, Group, Paper, Progress, Stack, Text } from "@mantine/core";
import Kicker from "../parts/Kicker";
import LandingButton from "../parts/LandingButton";
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
} from "../../theme";
import { CHALLENGES } from "./data";

export default function LabCard() {
  return (
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
            <Box w={6} h={6} style={{ borderRadius: 99, background: MINT, marginTop: 4 }} />
            <Text style={{ fontSize: 15, fontWeight: 600, color: INK, letterSpacing: "-0.01em" }}>
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
            <Text style={{ fontFamily: FONT_MONO, fontSize: 10.5, color: MUTED }}>
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

        <Text mb={12} style={{ fontSize: 12.5, color: INK_DIM, lineHeight: 1.55 }}>
          Campus Helpdesk is an internal support web application for a school. Reconnaissance,
          authentication, IDOR access control, SSRF, insecure API endpoints — find a way in.
        </Text>

        <Box mb={6}>
          <Kicker size={9.5} letterSpacing="0.16em">
            Challenges
          </Kicker>
        </Box>
        <Stack gap={4} mb={14}>
          {CHALLENGES.map((c, i) => (
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
                {c.done ? <Text style={{ fontSize: 9, color: MINT, lineHeight: 1 }}>✓</Text> : null}
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
  );
}
