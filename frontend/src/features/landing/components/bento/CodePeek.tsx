import { Box } from "@mantine/core";
import type { ReactNode } from "react";
import { FONT_MONO, LINE, MUTED } from "../../theme";

export default function CodePeek({
  children,
  inverted,
}: {
  children: ReactNode;
  inverted?: boolean;
}) {
  return (
    <Box
      className="code-peek"
      px={11}
      py={9}
      style={{
        fontFamily: FONT_MONO,
        fontSize: 11,
        background: inverted ? "rgba(0,0,0,0.18)" : "rgba(0,0,0,0.3)",
        border: `1px solid ${inverted ? "rgba(255,255,255,0.22)" : LINE}`,
        borderRadius: 8,
        lineHeight: 1.6,
        color: inverted ? "rgba(255,255,255,0.85)" : MUTED,
      }}
    >
      {children}
    </Box>
  );
}
