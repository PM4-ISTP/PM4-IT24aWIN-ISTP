import { Box } from "@mantine/core";
import BrowserFrame from "./parts/BrowserFrame";
import LabCard from "./hero-terminal/LabCard";
import Sidebar from "./hero-terminal/Sidebar";
import StatColumn from "./hero-terminal/StatColumn";
import { GRADIENT } from "../theme";

export default function HeroTerminal() {
  return (
    <Box style={{ margin: "80px auto 0", maxWidth: 1080, position: "relative" }}>
      <Box
        style={{
          position: "absolute",
          inset: -40,
          background: GRADIENT,
          filter: "blur(60px)",
          opacity: 0.18,
          borderRadius: 40,
          zIndex: 0,
          pointerEvents: "none",
        }}
      />
      <BrowserFrame
        url="istp.pm4.init-lab.ch/courses/web-security"
        style={{
          position: "relative",
          zIndex: 1,
          boxShadow: "0 30px 80px -20px rgba(0,0,0,0.6), 0 0 0 1px rgba(255,255,255,0.04) inset",
        }}
      >
        <Box
          className="hero-app-body"
          style={{
            display: "grid",
            gridTemplateColumns: "200px 1fr 280px",
            minHeight: 480,
          }}
        >
          <Sidebar />
          <LabCard />
          <StatColumn />
        </Box>
      </BrowserFrame>

      <style>{`
        @media (max-width: 900px) {
          .hero-app-body {
            grid-template-columns: 1fr !important;
          }
        }
      `}</style>
    </Box>
  );
}
