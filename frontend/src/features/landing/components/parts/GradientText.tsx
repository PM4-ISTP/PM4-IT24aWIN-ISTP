import { Box } from "@mantine/core";
import type { ReactNode } from "react";
import { GRADIENT } from "../../theme";

export default function GradientText({ children }: { children: ReactNode }) {
  return (
    <Box
      component="span"
      style={{
        background: GRADIENT,
        WebkitBackgroundClip: "text",
        backgroundClip: "text",
        color: "transparent",
        display: "inline-block",
      }}
    >
      {children}
    </Box>
  );
}
