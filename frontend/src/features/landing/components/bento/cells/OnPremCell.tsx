import { Box, Group, Text } from "@mantine/core";
import Cell from "../Cell";
import CellHead from "../CellHead";
import { FONT_MONO, LINE_2, MINT, MUTED } from "../../../theme";
import { ON_PREM_TAGS } from "../data";

export default function OnPremCell() {
  return (
    <Cell span={5} glow="mint">
      <CellHead
        tag="— 02"
        title="On-premises by design."
        description="Runs on your own Kubernetes cluster. No external SaaS, no per-seat pricing, no student data leaving campus."
      />
      <Group
        gap={6}
        style={{ flexWrap: "wrap", fontFamily: FONT_MONO, fontSize: 10.5, color: MUTED }}
      >
        {ON_PREM_TAGS.map((p) => (
          <Group
            key={p}
            gap={6}
            px={9}
            py={5}
            align="center"
            style={{ border: `1px solid ${LINE_2}`, borderRadius: 6 }}
          >
            <Box
              className="pulse-items"
              w={6}
              h={6}
              style={{ borderRadius: 99, background: MINT, boxShadow: `0 0 12px ${MINT}` }}
            />
            <Text style={{ fontFamily: FONT_MONO, fontSize: 10.5, color: MUTED }}>{p}</Text>
          </Group>
        ))}
      </Group>
    </Cell>
  );
}
