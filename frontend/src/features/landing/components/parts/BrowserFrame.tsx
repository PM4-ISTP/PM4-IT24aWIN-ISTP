import type { CSSProperties, ReactNode } from "react";
import { Box, Group, Text } from "@mantine/core";
import { FONT_MONO, LINE, LINE_2, MUTED } from "../../theme";

type DotStyle = "color" | "muted";

type BrowserFrameProps = {
  url: string;
  children: ReactNode;
  dots?: DotStyle;
  className?: string;
  innerRef?: React.Ref<HTMLDivElement>;
  style?: CSSProperties;
};

const COLOR_DOTS = ["#f06d6d", "#f5b462", "#6df0a0"];

export default function BrowserFrame({
  url,
  children,
  dots = "color",
  className,
  innerRef,
  style,
}: BrowserFrameProps) {
  return (
    <Box
      ref={innerRef}
      className={className}
      style={{
        border: `1px solid ${LINE_2}`,
        borderRadius: 14,
        overflow: "hidden",
        background: "linear-gradient(180deg,#0c1120 0%, #080c18 100%)",
        boxShadow: "0 30px 80px -20px rgba(0,0,0,0.6)",
        display: "flex",
        flexDirection: "column",
        minWidth: 0,
        ...style,
      }}
    >
      <Group
        gap={dots === "color" ? 8 : 6}
        align="center"
        px={dots === "color" ? 16 : 12}
        py={dots === "color" ? 12 : 10}
        style={{
          borderBottom: `1px solid ${LINE}`,
          background: "rgba(255,255,255,0.02)",
        }}
      >
        <Group gap={6}>
          {dots === "color"
            ? COLOR_DOTS.map((c) => (
                <Box key={c} w={11} h={11} style={{ borderRadius: 99, background: c }} />
              ))
            : [0, 1, 2].map((d) => (
                <Box
                  key={d}
                  w={9}
                  h={9}
                  style={{ borderRadius: 99, background: "rgba(255,255,255,0.18)" }}
                />
              ))}
        </Group>
        <Text
          ml={dots === "color" ? 12 : 10}
          px={dots === "color" ? 12 : 10}
          py={3}
          style={{
            fontFamily: FONT_MONO,
            fontSize: dots === "color" ? 11.5 : 10.5,
            color: MUTED,
            background: "rgba(255,255,255,0.04)",
            borderRadius: 6,
          }}
        >
          {url}
        </Text>
      </Group>
      {children}
    </Box>
  );
}
