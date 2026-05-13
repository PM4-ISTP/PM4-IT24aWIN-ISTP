import { Group } from "@mantine/core";
import Cell from "../Cell";
import CellHead from "../CellHead";
import Chip from "../Chip";
import { CHIPS, CHIP_SCENES } from "../data";

export default function CourseCell({ sceneIndex }: { sceneIndex: number }) {
  return (
    <Cell span={7} rowSpan={2} glow="indigo">
      <CellHead
        tag="— 01"
        title="Courses, labs, challenges."
        description="A course holds multiple labs. Each lab spins up its own pod from a Docker image. Stack flag or multiple-choice challenges on top — every student in their own sandbox."
        big
      />
      <Group gap={6} style={{ flexWrap: "wrap", maxWidth: 560 }}>
        {CHIPS.map((c, i) => (
          <Chip key={c.label} label={c.label} tone={CHIP_SCENES[sceneIndex][i]} />
        ))}
      </Group>
    </Cell>
  );
}
