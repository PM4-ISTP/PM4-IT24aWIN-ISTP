import { Text } from "@mantine/core";
import Cell from "../Cell";
import CellHead from "../CellHead";
import CodePeek from "../CodePeek";
import { ACCENT, INK_DIM, MINT, MUTED } from "../../../theme";

export default function DesignerCell() {
  return (
    <Cell span={4} glow="rose">
      <CellHead
        tag="— 06"
        title="Course, lab & challenge designer."
        description="Pick a Docker image, write a description, add flag or multiple-choice challenges. Done. The platform handles pods, scoring and lifecycle."
      />
      <CodePeek>
        <Text component="span" style={{ color: INK_DIM }}>
          # new lab
        </Text>
        <br />
        <Text component="span" style={{ color: ACCENT }}>
          image:
        </Text>{" "}
        <Text component="b" style={{ color: MINT, fontWeight: 500 }}>
          ghcr.io/school/sql-inject:1.0
        </Text>
        <br />
        <Text component="span" style={{ color: ACCENT }}>
          challenges:
        </Text>{" "}
        5{" "}
        <Text component="span" style={{ color: MUTED }}>
          ·
        </Text>{" "}
        <Text component="span" style={{ color: ACCENT }}>
          port:
        </Text>{" "}
        8080
      </CodePeek>
    </Cell>
  );
}
