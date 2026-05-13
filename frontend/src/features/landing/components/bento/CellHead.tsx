import { Box, Text } from "@mantine/core";
import Kicker from "../parts/Kicker";
import { INK, INK_DIM } from "../../theme";

type CellHeadProps = {
  tag: string;
  title: string;
  description: string;
  accent?: boolean;
  big?: boolean;
};

export default function CellHead({ tag, title, description, accent, big }: CellHeadProps) {
  return (
    <Box>
      <Box mb={8}>
        <Kicker size={10} style={accent ? { color: "rgba(255,255,255,0.85)" } : undefined}>
          {tag}
        </Kicker>
      </Box>
      <Text
        style={{
          fontSize: big ? 30 : 20,
          lineHeight: big ? 1.1 : 1.2,
          fontWeight: 600,
          letterSpacing: "-0.01em",
          margin: "10px 0 6px",
          color: accent ? "#fff" : INK,
        }}
      >
        {title}
      </Text>
      <Text
        style={{
          margin: 0,
          fontSize: big ? 15 : 13.5,
          color: accent ? "rgba(255,255,255,0.85)" : INK_DIM,
          lineHeight: 1.5,
          maxWidth: big ? 460 : undefined,
        }}
      >
        {description}
      </Text>
    </Box>
  );
}
