import { Box } from "@mantine/core";
import { GRADIENT } from "@/src/shared/lib/theme";

export default function BrandMark({ size = 30 }: { size?: number }) {
  const radius = size * 0.3;
  return (
    <Box
      w={size}
      h={size}
      style={{
        position: "relative",
        borderRadius: radius,
        overflow: "hidden",
        background: GRADIENT,
        boxShadow:
          "0 0 0 1px rgba(255,255,255,0.16) inset, 0 10px 26px rgba(0,0,0,0.32), 0 0 24px rgba(93,110,240,0.18), 0 0 22px rgba(109,240,200,0.12)",
        flexShrink: 0,
      }}
    >
      {/* Rim glow */}
      <Box
        style={{
          position: "absolute",
          inset: 0,
          borderRadius: radius,
          background:
            "radial-gradient(70% 65% at 18% 10%, rgba(93,110,240,0.28) 0%, rgba(93,110,240,0) 60%), radial-gradient(70% 65% at 92% 18%, rgba(59,130,246,0.20) 0%, rgba(59,130,246,0) 60%), radial-gradient(80% 80% at 70% 110%, rgba(109,240,200,0.18) 0%, rgba(109,240,200,0) 60%)",
          pointerEvents: "none",
        }}
      />

      {/* Inner border */}
      <Box
        style={{
          position: "absolute",
          inset: 0,
          borderRadius: radius,
          boxShadow: "0 0 0 1px rgba(255,255,255,0.18) inset, 0 0 0 1px rgba(0,0,0,0.20)",
          pointerEvents: "none",
        }}
      />

      {/* Gloss */}
      <Box
        style={{
          position: "absolute",
          inset: 0,
          borderRadius: radius,
          background:
            "linear-gradient(180deg, rgba(255,255,255,0.18) 0%, rgba(255,255,255,0.06) 18%, rgba(255,255,255,0) 52%)",
          opacity: 0.9,
          pointerEvents: "none",
        }}
      />

      <Box
        component="img"
        src="/images/brand/istp_logo.png"
        alt="ISTP logo"
        style={{
          display: "block",
          width: size,
          height: size,
          objectFit: "cover",
          filter: "drop-shadow(0 6px 18px rgba(0,0,0,0.28))",
          transform: "scale(0.98)",
        }}
      />
    </Box>
  );
}
