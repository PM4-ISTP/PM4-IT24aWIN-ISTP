import { Box, Progress, Stack, Text } from "@mantine/core";
import Cell from "../Cell";
import CellHead from "../CellHead";
import { FONT_MONO, GRADIENT, INK, LINE, MUTED } from "../../../theme";
import { WORKFLOW_STATS } from "../data";

export default function WorkflowCell() {
  return (
    <Cell span={4} glow="amber">
      <CellHead
        tag="— 04"
        title="Built for academic workflows."
        description="Three roles — student, instructor, admin. University email-domain sign-up, per-course progress, no spreadsheets."
      />
      <Box style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8 }}>
        {WORKFLOW_STATS.map((s) => (
          <Stack
            key={s.l}
            gap={2}
            px={10}
            py={9}
            style={{ border: `1px solid ${LINE}`, borderRadius: 8 }}
          >
            <Text style={{ fontFamily: FONT_MONO, fontSize: 16, color: INK, fontWeight: 600 }}>
              {s.n}
            </Text>
            <Text
              style={{
                fontSize: 10,
                color: MUTED,
                textTransform: "uppercase",
                letterSpacing: "0.12em",
              }}
            >
              {s.l}
            </Text>
            <Progress
              value={s.w}
              size={3}
              mt={4}
              radius="xl"
              styles={{
                root: { background: "rgba(255,255,255,0.06)" },
                section: { background: GRADIENT },
              }}
            />
          </Stack>
        ))}
      </Box>
    </Cell>
  );
}
