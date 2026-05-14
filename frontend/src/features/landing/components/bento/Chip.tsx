import { Box } from "@mantine/core";
import { FONT_MONO } from "../../theme";
import { CHIP_PALETTES, type ChipTone } from "./data";

export default function Chip({ label, tone }: { label: string; tone?: ChipTone }) {
  const palette = CHIP_PALETTES[tone ?? "default"];
  return (
    <Box
      component="span"
      px={10}
      py={5}
      style={{
        fontFamily: FONT_MONO,
        fontSize: 11.5,
        borderRadius: 6,
        border: `1px solid ${palette.border}`,
        color: palette.color,
        background: palette.background,
        transition: "color 0.6s ease, border-color 0.6s ease, background 0.6s ease",
      }}
    >
      {label}
    </Box>
  );
}
