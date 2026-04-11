import { getServerSession } from "next-auth";
import { authOptions } from "@/src/lib/auth";
import { redirect } from "next/navigation";
import { Box, Container, Stack, Title, Text, Badge } from "@mantine/core";
import Login from "@/src/components/Login";
import { TeamSection } from "@/src/components/TeamSection";

const heroBackground = "linear-gradient(160deg, #0b1120 0%, #0e1a2e 45%, #0b1624 100%)";

const cardStyle: React.CSSProperties = {
  width: "100%",
  maxWidth: 360,
  background: "rgba(255,255,255,0.04)",
  border: "1px solid rgba(255,255,255,0.08)",
  borderRadius: 14,
  padding: "32px 28px",
  boxShadow: "0 8px 32px rgba(0,0,0,0.4)",
};

export default async function Home() {
  const session = await getServerSession(authOptions);

  if (session) {
    redirect("/dashboard");
  }

  return (
    <Box
      style={{
        minHeight: "100vh",
        background: heroBackground,
        WebkitFontSmoothing: "antialiased",
        MozOsxFontSmoothing: "grayscale",
        fontFamily: "var(--font-space-grotesk), sans-serif",
        display: "flex",
        flexDirection: "column",
      }}
    >
      {/* Top nav */}
      <Box
        component="header"
        style={{
          borderBottom: "1px solid rgba(255,255,255,0.06)",
          padding: "18px 40px",
          display: "flex",
          alignItems: "center",
        }}
      >
        <Text
          style={{
            color: "#e2e8f0",
            fontFamily: "var(--font-space-grotesk), sans-serif",
            fontWeight: 700,
            fontSize: "1rem",
            letterSpacing: "0.04em",
          }}
        >
          ISTP
        </Text>
      </Box>

      {/* Hero */}
      <Container
        size="sm"
        style={{
          flex: 1,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          paddingTop: 64,
          paddingBottom: 64,
        }}
      >
        <Stack gap="xl" align="center" style={{ textAlign: "center", width: "100%" }}>
          <Badge
            variant="outline"
            size="sm"
            style={{
              color: "#60a5fa",
              borderColor: "rgba(96,165,250,0.25)",
              background: "rgba(96,165,250,0.06)",
              fontSize: "0.7rem",
              letterSpacing: "0.1em",
              textTransform: "uppercase",
              fontFamily: "var(--font-space-grotesk), sans-serif",
            }}
          >
            Cybersecurity Training Platform
          </Badge>

          <Title
            order={1}
            style={{
              color: "#f1f5f9",
              fontFamily: "var(--font-space-grotesk), sans-serif",
              fontWeight: 700,
              fontSize: "clamp(2rem, 5vw, 3.25rem)",
              lineHeight: 1.2,
              letterSpacing: "-0.02em",
              maxWidth: 540,
            }}
          >
            Learn Security{" "}
            <span
              style={{
                background: "linear-gradient(90deg, #60a5fa, #818cf8)",
                WebkitBackgroundClip: "text",
                WebkitTextFillColor: "transparent",
                backgroundClip: "text",
              }}
            >
              Hands-On
            </span>
          </Title>

          <Text
            style={{
              color: "#94a3b8",
              fontSize: "1.05rem",
              lineHeight: 1.75,
              maxWidth: 460,
            }}
          >
            Structured courses, interactive exercises, and real-world scenarios to build practical
            cybersecurity skills.
          </Text>

          {/* Login card */}
          <div style={cardStyle}>
            <Stack gap="md">
              <Stack gap={4} align="center">
                <Text
                  style={{
                    color: "#e2e8f0",
                    fontWeight: 600,
                    fontSize: "0.95rem",
                    fontFamily: "var(--font-space-grotesk), sans-serif",
                  }}
                >
                  Sign in to your account
                </Text>
                <Text
                  style={{
                    color: "#64748b",
                    fontSize: "0.82rem",
                    fontFamily: "var(--font-space-grotesk), sans-serif",
                  }}
                >
                  Continue your learning journey
                </Text>
              </Stack>
              <Login />
            </Stack>
          </div>

          {/* Team */}
          <TeamSection />
        </Stack>
      </Container>

      {/* Footer */}
      <Box
        component="footer"
        style={{
          borderTop: "1px solid rgba(255,255,255,0.06)",
          padding: "16px 40px",
          textAlign: "center",
        }}
      >
        <Text
          style={{
            color: "rgba(255,255,255,0.35)",
            fontSize: "0.78rem",
            fontFamily: "var(--font-space-grotesk), sans-serif",
          }}
        >
          Interactive Security Training Platform
        </Text>
      </Box>
    </Box>
  );
}
