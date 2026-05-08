import { getServerSession } from "next-auth";
import { authOptions } from "@/src/shared/lib/auth";
import { redirect } from "next/navigation";
import { Box } from "@mantine/core";
import LandingNav from "@/src/features/landing/components/LandingNav";
import LandingHero from "@/src/features/landing/components/LandingHero";
import LandingVideo from "@/src/features/landing/components/LandingVideo";
import LandingBento from "@/src/features/landing/components/LandingBento";
import LandingScreenshots from "@/src/features/landing/components/LandingScreenshots";
import LandingCta from "@/src/features/landing/components/LandingCta";
import LandingFooter from "@/src/features/landing/components/LandingFooter";
import { FONT_SANS, INK, LANDING_BG } from "@/src/features/landing/theme";

export default async function Home() {
  const session = await getServerSession(authOptions);

  if (session) {
    redirect("/dashboard");
  }

  return (
    <Box
      style={{
        minHeight: "100vh",
        background: `
          radial-gradient(900px 500px at 80% -10%, rgba(93,110,240,0.18), transparent 60%),
          radial-gradient(700px 460px at 10% 8%, rgba(109,240,200,0.06), transparent 60%),
          ${LANDING_BG}
        `,
        color: INK,
        fontFamily: FONT_SANS,
        WebkitFontSmoothing: "antialiased",
        position: "relative",
        overflowX: "hidden",
      }}
    >
      {/* Subtle 48px grid scaffolding with radial mask */}
      <Box
        aria-hidden
        style={{
          position: "absolute",
          inset: 0,
          pointerEvents: "none",
          backgroundImage: `
            linear-gradient(rgba(255,255,255,0.025) 1px, transparent 1px),
            linear-gradient(90deg, rgba(255,255,255,0.025) 1px, transparent 1px)
          `,
          backgroundSize: "48px 48px",
          WebkitMaskImage:
            "radial-gradient(ellipse 1100px 700px at 50% 200px, #000 30%, transparent 75%)",
          maskImage:
            "radial-gradient(ellipse 1100px 700px at 50% 200px, #000 30%, transparent 75%)",
        }}
      />

      <Box style={{ position: "relative", zIndex: 1 }}>
        <LandingNav />
        <LandingHero />
        <LandingVideo />
        <LandingBento />
        <LandingScreenshots />
        <LandingCta />
        <LandingFooter />
      </Box>
    </Box>
  );
}
