import { Button, type ButtonProps, type ElementProps } from "@mantine/core";
import type { CSSProperties, ComponentType } from "react";
import { GRADIENT, INK, LINE } from "@/src/shared/lib/theme";

const AnyButton = Button as ComponentType<Record<string, unknown>>;

/**
 * Visual intent of the button:
 * - "primary" = brand gradient (Save, Create, Search, Submit)
 * - "ghost"   = subtle outlined surface (secondary actions)
 * - "danger"  = red (destructive: Delete, Logout, Remove)
 */
export type AppButtonTone = "primary" | "ghost" | "danger";

type AppButtonProps = ButtonProps &
  ElementProps<"button", keyof ButtonProps> & {
    tone?: AppButtonTone;
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

const DANGER_STYLE: CSSProperties = {
  background: "rgba(240,109,138,0.12)",
  border: "1px solid rgba(240,109,138,0.35)",
  color: "#f06d8a",
  fontWeight: 500,
};

const TONE_STYLES: Record<AppButtonTone, CSSProperties> = {
  primary: PRIMARY_STYLE,
  ghost: GHOST_STYLE,
  danger: DANGER_STYLE,
};

/**
 * Shared app button. Used across the landing page and the dashboard so primary
 * actions look identical everywhere.
 */
export default function AppButton({
  tone = "primary",
  style,
  variant,
  radius = "md" as ButtonProps["radius"],
  href,
  component,
  target,
  rel,
  ...rest
}: AppButtonProps) {
  const toneStyle = TONE_STYLES[tone];
  const resolvedComponent = component ?? (href ? "a" : undefined);
  const isExternal = !!href && /^https?:\/\//i.test(href);
  return (
    <AnyButton
      radius={radius}
      variant={tone === "primary" ? variant : "default"}
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
