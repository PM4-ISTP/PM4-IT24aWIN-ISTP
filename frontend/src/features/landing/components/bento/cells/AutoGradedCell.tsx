import { Text } from "@mantine/core";
import Cell from "../Cell";
import CellHead from "../CellHead";
import CodePeek from "../CodePeek";
import { ACCENT, AMBER, INK_DIM, MINT } from "../../../theme";

export default function AutoGradedCell() {
  return (
    <Cell span={4} glow="mint">
      <CellHead
        tag="— 05"
        title="Auto-graded, instantly scored."
        description="Flags and multiple-choice answers grade themselves. Pods auto-terminate after 60 minutes idle to free up the cluster."
      />
      <CodePeek>
        <Text component="span" style={{ color: INK_DIM }}>
          # POST /api/submissions
        </Text>
        <br />✓ flag ·{" "}
        <Text component="b" style={{ color: MINT, fontWeight: 500 }}>
          matches
        </Text>
        <br />✓ score ·{" "}
        <Text component="span" style={{ color: ACCENT }}>
          +400 pts
        </Text>
        <br />
        <Text component="span" style={{ color: AMBER }}>
          ⏲ pod idle · 58m left
        </Text>
      </CodePeek>
    </Cell>
  );
}
