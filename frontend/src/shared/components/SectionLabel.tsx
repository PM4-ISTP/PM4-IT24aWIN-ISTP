import { Text } from "@mantine/core";
import type { CSSProperties, ReactNode } from "react";

/** Shared uppercase eyebrow label used for section headings across the app. */
export const sectionLabelStyle: CSSProperties = {
  fontFamily: "var(--font-space-grotesk), sans-serif",
  textTransform: "uppercase",
  letterSpacing: "0.1em",
  fontSize: "0.72rem",
  fontWeight: 700,
  color: "rgba(255,255,255,0.45)",
};

export default function SectionLabel({
  children,
  style,
}: {
  children: ReactNode;
  style?: CSSProperties;
}) {
  return <Text style={{ ...sectionLabelStyle, ...style }}>{children}</Text>;
}
