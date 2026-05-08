import { Box, Text } from "@mantine/core";
import { FONT_MONO, GRADIENT } from "../../theme";

export default function BrandMark({ size = 30 }: { size?: number }) {
  return (
    <Box
      w={size}
      h={size}
      style={{
        borderRadius: size * 0.27,
        background: GRADIENT,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        boxShadow:
          "0 0 0 1px rgba(255,255,255,0.12) inset, 0 6px 20px rgba(93,110,240,0.4)",
        flexShrink: 0,
      }}
    >
      <Text
        style={{
          color: "#fff",
          fontFamily: FONT_MONO,
          fontWeight: 700,
          fontSize: Math.round(size * 0.4),
          mixBlendMode: "screen",
        }}
      >
        {size >= 28 ? "{ }" : "{}"}
      </Text>
    </Box>
  );
}
