import type { CSSProperties, ReactNode } from "react";
import { Box, Stack, Title } from "@mantine/core";
import Kicker from "@/src/shared/components/brand/Kicker";
import { INK } from "../../theme";

type SectionHeaderProps = {
  kicker?: ReactNode;
  children: ReactNode;
  align?: "center" | "start";
  innerRef?: React.Ref<HTMLDivElement>;
  className?: string;
  style?: CSSProperties;
  titleStyle?: CSSProperties;
  fontSize?: number | string;
};

export default function SectionHeader({
  kicker,
  children,
  align = "start",
  innerRef,
  className,
  style,
  titleStyle,
  fontSize = 54,
}: SectionHeaderProps) {
  return (
    <Stack
      ref={innerRef}
      className={className}
      gap={10}
      align={align === "center" ? "center" : "flex-start"}
      style={{ textAlign: align === "center" ? "center" : "left", ...style }}
    >
      {kicker ? <Box>{typeof kicker === "string" ? <Kicker>{kicker}</Kicker> : kicker}</Box> : null}
      <Title
        order={2}
        style={{
          fontSize,
          fontWeight: 600,
          letterSpacing: "-0.025em",
          margin: 0,
          color: INK,
          lineHeight: 1,
          ...titleStyle,
        }}
      >
        {children}
      </Title>
    </Stack>
  );
}
