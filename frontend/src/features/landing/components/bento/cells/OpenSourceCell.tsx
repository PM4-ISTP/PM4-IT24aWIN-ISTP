import { Text } from "@mantine/core";
import Cell from "../Cell";
import CellHead from "../CellHead";
import CodePeek from "../CodePeek";

export default function OpenSourceCell() {
  return (
    <Cell span={5} accent glow="indigo">
      <CellHead
        tag="— 03"
        title="Open-source. No catch."
        description="Fork it. Brand it. Translate it. Run a hundred instances. We'd love a PR back, but you don't owe us one."
        accent
      />
      <CodePeek inverted>
        ${" "}
        <Text component="b" style={{ color: "#fff", fontWeight: 500 }}>
          git clone
        </Text>{" "}
        <Text component="span" style={{ color: "#dfe7ff" }}>
          github.com/PM4-ISTP/istp
        </Text>
        <br />${" "}
        <Text component="b" style={{ color: "#fff", fontWeight: 500 }}>
          docker compose
        </Text>{" "}
        up -d
      </CodePeek>
    </Cell>
  );
}
