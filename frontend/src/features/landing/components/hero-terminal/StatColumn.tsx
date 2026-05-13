import { Box, Group, Paper, Stack, Text } from "@mantine/core";
import Kicker from "../parts/Kicker";
import { FONT_MONO, INK, LINE, LINE_2, MINT, MUTED } from "../../theme";
import { STATS } from "./data";

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <Paper p={14} radius={10} style={{ border: `1px solid ${LINE}`, background: "transparent" }}>
      <Box mb={6}>
        <Kicker size={9.5} letterSpacing="0.16em">
          {label}
        </Kicker>
      </Box>
      <Text style={{ fontSize: 22, fontWeight: 600, color: INK, lineHeight: 1 }}>{value}</Text>
    </Paper>
  );
}

export default function StatColumn() {
  return (
    <Stack
      visibleFrom="md"
      gap={10}
      p="22px 16px"
      style={{ borderLeft: `1px solid ${LINE}`, background: "rgba(255,255,255,0.015)" }}
    >
      {STATS.map((s) => (
        <StatCard key={s.label} label={s.label} value={s.value} />
      ))}

      <Paper
        p={14}
        radius={10}
        style={{ border: `1px dashed ${LINE_2}`, background: "transparent" }}
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
  );
}
