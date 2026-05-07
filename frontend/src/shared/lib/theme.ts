export const surfaceTokens = {
  background: {
    subtle: "rgba(255,255,255,0.02)",
    default: "rgba(255,255,255,0.03)",
    strong: "rgba(255,255,255,0.04)",
  },
  border: "1px solid rgba(255,255,255,0.08)",
  radius: {
    sm: 12,
    md: 14,
  },
  shadow: {
    sm: "0 2px 8px rgba(0,0,0,0.2)",
    md: "0 4px 24px rgba(0,0,0,0.25)",
    lg: "0 8px 32px rgba(0,0,0,0.4)",
  },
} as const;

export type SurfaceVariant = "subtle" | "default" | "strong";
export type SurfaceElevation = "sm" | "md" | "lg";
