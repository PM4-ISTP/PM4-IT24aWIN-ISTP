import { Button, type ButtonProps, type ElementProps } from "@mantine/core";
import type { CSSProperties } from "react";
import { GRADIENT, INK, LINE } from "../../theme";

type LandingButtonProps = ButtonProps &
  ElementProps<"button", keyof ButtonProps> & {
    tone?: "primary" | "ghost";
    component?: React.ElementType;
    href?: string;
  };

const PRIMARY_STYLE: CSSProperties = {
  background: GRADIENT,
  border: "none",
  color: "#fff",
  fontWeight: 500,
  boxShadow: "0 8px 24px -8px rgba(93,110,240,0.7), 0 0 0 1px rgba(255,255,255,0.1) inset",
};

const GHOST_STYLE: CSSProperties = {
  background: "rgba(255,255,255,0.02)",
  border: `1px solid ${LINE}`,
  color: INK,
  fontWeight: 500,
};

export default function LandingButton({
  tone = "primary",
  style,
  variant,
  radius = "md",
  ...rest
}: LandingButtonProps) {
  const toneStyle = tone === "primary" ? PRIMARY_STYLE : GHOST_STYLE;
  return (
    <Button
      radius={radius}
      variant={tone === "ghost" ? "default" : variant}
      style={{ ...toneStyle, ...((style as CSSProperties) ?? {}) }}
      {...rest}
    />
  );
}
