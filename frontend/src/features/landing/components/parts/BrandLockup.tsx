import { Box, Group, Text } from "@mantine/core";
import { FONT_MONO, INK, MUTED } from "../../theme";
import BrandMark from "./BrandMark";

interface BrandLockupProps {
  size?: number;
  subtitle?: string;
  labelSize?: number;
}

export default function BrandLockup({
  size = 30,
  subtitle,
  labelSize,
}: BrandLockupProps) {
  const finalLabelSize = labelSize ?? (size >= 28 ? 14 : 13);
  return (
    <Group gap={10} align="center" wrap="nowrap">
      <BrandMark size={size} />
      <Box style={{ display: "flex", flexDirection: "column", lineHeight: 1 }}>
        <Text
          fw={600}
          style={{ letterSpacing: "0.06em", color: INK, fontSize: finalLabelSize }}
        >
          ISTP
        </Text>
        {subtitle && (
          <Text
            style={{
              fontFamily: FONT_MONO,
              fontSize: 9,
              letterSpacing: "0.22em",
              color: MUTED,
              marginTop: 3,
            }}
          >
            {subtitle}
          </Text>
        )}
      </Box>
    </Group>
  );
}
