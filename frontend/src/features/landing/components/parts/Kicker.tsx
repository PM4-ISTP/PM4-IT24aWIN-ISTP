import { Text } from "@mantine/core";
import type { CSSProperties, ReactNode } from "react";
import { FONT_MONO, MUTED } from "../../theme";

interface KickerProps {
  children: ReactNode;
  size?: number;
  letterSpacing?: string;
  style?: CSSProperties;
}

export default function Kicker({
  children,
  size = 11,
  letterSpacing = "0.18em",
  style,
}: KickerProps) {
  return (
    <Text
      style={{
        fontFamily: FONT_MONO,
        fontSize: size,
        letterSpacing,
        textTransform: "uppercase",
        color: MUTED,
        fontWeight: 500,
        ...style,
      }}
    >
      {children}
    </Text>
  );
}
