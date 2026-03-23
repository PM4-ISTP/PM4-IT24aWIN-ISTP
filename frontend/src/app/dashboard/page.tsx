import { getServerSession } from "next-auth";
import Image from "next/image";
import { authOptions } from "@/src/lib/auth";
import { Stack, Title, Text } from "@mantine/core";

export default async function Home() {
  const session = await getServerSession(authOptions);
  const name = session?.user?.name ?? "there";
  const firstName = name.split(" ")[0];

  return (
    <div style={{ minHeight: "100vh", background: "#0a0a0f" }}>
      {/* Hero Section */}
      <div
        style={{
          position: "relative",
          minHeight: "60vh",
          display: "flex",
          alignItems: "center",
          padding: "3rem 2.5rem",
          overflow: "hidden",
          background: "#04062B",
        }}
      >
        {/* Glow orbs */}
        <div
          style={{
            position: "absolute",
            top: "10%",
            right: "15%",
            width: 400,
            height: 400,
            borderRadius: "50%",
            background: "radial-gradient(circle, rgba(59,130,246,0.15) 0%, transparent 70%)",
            filter: "blur(40px)",
          }}
        />
        <div
          style={{
            position: "absolute",
            bottom: "5%",
            left: "10%",
            width: 300,
            height: 300,
            borderRadius: "50%",
            background: "radial-gradient(circle, rgba(139,92,246,0.12) 0%, transparent 70%)",
            filter: "blur(40px)",
          }}
        />

        {/* Content */}
        <div
          style={{
            position: "relative",
            zIndex: 1,
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))",
            gap: "clamp(0.75rem, 2vw, 1.5rem)",
            alignItems: "center",
            width: "100%",
          }}
        >
          <Stack gap="xl" style={{ maxWidth: 700 }}>
            <Text
              style={{
                alignSelf: "flex-start",
                padding: "0.55rem 0.9rem",
                borderRadius: 999,
                border: "1px solid rgba(34,211,238,0.22)",
                background: "rgba(34,211,238,0.08)",
                color: "#89ecff",
                fontFamily: "var(--font-geist-mono), monospace",
                fontSize: "0.78rem",
                fontWeight: 700,
                letterSpacing: "0.24em",
                textTransform: "uppercase",
              }}
            >
              Interactive Security Training Platform
            </Text>

            <div>
              <Text
                style={{
                  color: "rgba(255,255,255,0.72)",
                  fontFamily: "var(--font-geist-sans), sans-serif",
                  fontSize: "clamp(1.1rem, 2.1vw, 1.4rem)",
                  fontWeight: 700,
                  letterSpacing: "0.02em",
                  marginBottom: 14,
                }}
              >
                Hey, {firstName}
              </Text>

              <Title
                order={1}
                style={{
                  fontFamily: "var(--font-geist-sans), sans-serif",
                  fontSize: "clamp(3rem, 7vw, 5.6rem)",
                  fontWeight: 950,
                  lineHeight: 0.92,
                  letterSpacing: "-0.055em",
                  color: "#ffffff",
                  textWrap: "balance",
                }}
              >
                Train With
                <br />
                <span
                  style={{
                    color: "#22d3ee",
                    textShadow: "0 0 28px rgba(34, 211, 238, 0.28)",
                  }}
                >
                  Precision
                </span>
              </Title>
            </div>

            <Text
              style={{
                color: "rgba(255,255,255,0.7)",
                fontSize: "clamp(1.05rem, 1.9vw, 1.2rem)",
                lineHeight: 1.8,
                maxWidth: 560,
              }}
            >
              Level up your cybersecurity skills through real-world labs, sharper challenges and a
              focused training space built for offensive and defensive practice.
            </Text>
          </Stack>

          <div
            style={{
              display: "flex",
              justifyContent: "flex-start",
              alignItems: "center",
              width: "100%",
            }}
          >
            <div
              style={{
                position: "relative",
                width: "100%",
                maxWidth: 520,
              }}
            >
              <div
                style={{
                  position: "absolute",
                  inset: "12% 2% -2% 10%",
                  borderRadius: 32,
                  background: "radial-gradient(circle, rgba(34,211,238,0.22) 0%, transparent 72%)",
                  filter: "blur(26px)",
                }}
              />
              <div
                style={{
                  position: "relative",
                  padding: "0.75rem",
                  borderRadius: 30,
                  border: "1px solid rgba(255,255,255,0.08)",
                  background:
                    "linear-gradient(180deg, rgba(255,255,255,0.08) 0%, rgba(255,255,255,0.03) 100%)",
                  boxShadow: "0 32px 80px rgba(0, 0, 0, 0.42)",
                }}
              >
                <Image
                  src="/images/home.png"
                  alt="Cybersecurity training illustration"
                  width={520}
                  height={520}
                  priority
                  style={{
                    display: "block",
                    width: "100%",
                    height: "auto",
                    borderRadius: 22,
                  }}
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Stats placeholder */}
      <div style={{ padding: "2.5rem", background: "#0a0a0f" }}>
        <Text
          style={{
            color: "rgba(255,255,255,0.35)",
            fontSize: "0.8rem",
            letterSpacing: "0.15em",
            textTransform: "uppercase",
            marginBottom: 4,
          }}
        >
          Your Statistics
        </Text>
        <Title order={2} style={{ color: "rgba(255,255,255,0.15)", fontWeight: 700 }}>
          (Coming soon)
        </Title>
      </div>
    </div>
  );
}
