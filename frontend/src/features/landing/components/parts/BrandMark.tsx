import { Box } from "@mantine/core";
import Image from "next/image";
import { GRADIENT } from "../../theme";

export default function BrandMark({ size = 30 }: { size?: number }) {
  return (
    <Box
      w={size}
      h={size}
      style={{
        borderRadius: size * 0.27,
        background: GRADIENT,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        boxShadow: "0 0 0 1px rgba(255,255,255,0.12) inset, 0 6px 20px rgba(93,110,240,0.4)",
        flexShrink: 0,
        overflow: "hidden",
      }}
    >
      <Image
        src="/brand/logoISTP.png"
        alt="ISTP Logo"
        width={size}
        height={size}
        priority={size >= 28}
        style={{
          width: size,
          height: size,
          objectFit: "cover",
          objectPosition: "center",
        }}
      />
    </Box>
  );
}
