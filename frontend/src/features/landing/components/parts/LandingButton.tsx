import { Button, type ButtonProps, type ElementProps } from "@mantine/core";
import type { CSSProperties, ComponentType } from "react";

const AnyButton = Button as ComponentType<Record<string, unknown>>;
import { GRADIENT, INK, LINE } from "../../theme";

type LandingButtonProps = ButtonProps &
  ElementProps<"button", keyof ButtonProps> & {
    tone?: "primary" | "ghost";
    href?: string;
    component?: string;
    target?: string;
    rel?: string;
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
  radius = "md" as ButtonProps["radius"],
  href,
  component,
  target,
  rel,
  ...rest
}: LandingButtonProps) {
  const toneStyle = tone === "primary" ? PRIMARY_STYLE : GHOST_STYLE;
  const resolvedComponent = component ?? (href ? "a" : undefined);
  const isExternal = !!href && /^https?:\/\//i.test(href);
  return (
    <AnyButton
      radius={radius}
      variant={tone === "ghost" ? "default" : variant}
      style={{ ...toneStyle, ...((style as CSSProperties) ?? {}) }}
      component={resolvedComponent}
      href={href}
      target={target ?? (isExternal ? "_blank" : undefined)}
      rel={rel ?? (isExternal ? "noopener noreferrer" : undefined)}
      // Mantine's Button slots have overflow: hidden + line-height: 1, which
      // clips descenders (g, p, y, …). Open them up.
      styles={
        {
          root: { overflow: "visible" },
          inner: { overflow: "visible" },
          label: { lineHeight: 1.4, overflow: "visible" },
        } as ButtonProps["styles"]
      }
      {...rest}
    />
  );
}
