"use client";

import { Box, type BoxProps } from "@mantine/core";
import type { CSSProperties, ReactNode } from "react";
import { surfaceTokens, type SurfaceElevation, type SurfaceVariant } from "@/src/shared/lib/theme";

export interface SurfaceCardProps extends Omit<BoxProps, "style"> {
  variant?: SurfaceVariant;
  elevation?: SurfaceElevation;
  radius?: "sm" | "md";
  padding?: CSSProperties["padding"];
  style?: CSSProperties;
  children?: ReactNode;
}

export function SurfaceCard({
  variant = "default",
  elevation = "md",
  radius = "md",
  padding = "1.5rem",
  style,
  children,
  ...rest
}: SurfaceCardProps) {
  const surfaceStyle: CSSProperties = {
    background: surfaceTokens.background[variant],
    border: surfaceTokens.border,
    borderRadius: surfaceTokens.radius[radius],
    boxShadow: surfaceTokens.shadow[elevation],
    padding,
    ...style,
  };

  return (
    <Box style={surfaceStyle} {...rest}>
      {children}
    </Box>
  );
}
