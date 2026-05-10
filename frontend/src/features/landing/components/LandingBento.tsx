import { Box, Container, Group, Paper, Progress, Stack, Text, Title } from "@mantine/core";
import GradientText from "./parts/GradientText";
import Kicker from "./parts/Kicker";
import {
  ACCENT,
  AMBER,
  FONT_MONO,
  GRADIENT,
  INK,
  INK_DIM,
  LINE,
  LINE_2,
  MINT,
  MUTED,
} from "../theme";

type ChipTone = "hot" | "live";
const chips: { label: string; tone?: ChipTone }[] = [
  { label: "SQL injection", tone: "hot" },
  { label: "XSS" },
  { label: "IDOR", tone: "hot" },
  { label: "broken auth" },
  { label: "broken access" },
  { label: "SSRF" },
  { label: "CSRF" },
  { label: "insecure deser" },
  { label: "file upload" },
  { label: "session" },
  { label: "API security" },
  { label: "crypto" },
  { label: "misconfig" },
  { label: "OWASP Top 10 · core", tone: "live" },
];

function Chip({ label, tone }: { label: string; tone?: "hot" | "live" }) {
  const palette =
    tone === "hot"
      ? {
          color: ACCENT,
          border: "rgba(93,110,240,0.4)",
          background: "rgba(93,110,240,0.1)",
        }
      : tone === "live"
        ? {
            color: MINT,
            border: "rgba(109,240,200,0.3)",
            background: "rgba(109,240,200,0.06)",
          }
        : {
            color: INK_DIM,
            border: LINE_2,
            background: "rgba(255,255,255,0.02)",
          };
  return (
    <Box
      component="span"
      px={10}
      py={5}
      style={{
        fontFamily: FONT_MONO,
        fontSize: 11.5,
        borderRadius: 6,
        border: `1px solid ${palette.border}`,
        color: palette.color,
        background: palette.background,
      }}
    >
      {label}
    </Box>
  );
}

function Cell({
  span,
  rowSpan,
  accent,
  children,
}: {
  span: number;
  rowSpan?: number;
  accent?: boolean;
  children: React.ReactNode;
}) {
  return (
    <Paper
      p={22}
      radius={18}
      style={{
        position: "relative",
        gridColumn: `span ${span}`,
        gridRow: rowSpan ? `span ${rowSpan}` : undefined,
        border: accent ? "1px solid transparent" : `1px solid ${LINE}`,
        background: accent
          ? "linear-gradient(135deg,#5d6ef0 0%, #3b82f6 100%)"
          : "linear-gradient(180deg, #0d1322 0%, #0a0f1c 100%)",
        display: "flex",
        flexDirection: "column",
        justifyContent: "space-between",
        gap: 14,
        overflow: "hidden",
        minWidth: 0,
        transition: "border-color 0.2s, transform 0.2s",
      }}
    >
      {children}
    </Paper>
  );
}

function CellHead({
  tag,
  title,
  description,
  accent,
  big,
}: {
  tag: string;
  title: string;
  description: string;
  accent?: boolean;
  big?: boolean;
}) {
  return (
    <Box>
      <Box mb={8}>
        <Kicker size={10} style={accent ? { color: "rgba(255,255,255,0.85)" } : undefined}>
          {tag}
        </Kicker>
      </Box>
      <Text
        style={{
          fontSize: big ? 30 : 20,
          lineHeight: big ? 1.1 : 1.2,
          fontWeight: 600,
          letterSpacing: "-0.01em",
          margin: "10px 0 6px",
          color: accent ? "#fff" : INK,
        }}
      >
        {title}
      </Text>
      <Text
        style={{
          margin: 0,
          fontSize: big ? 15 : 13.5,
          color: accent ? "rgba(255,255,255,0.85)" : INK_DIM,
          lineHeight: 1.5,
          maxWidth: big ? 460 : undefined,
        }}
      >
        {description}
      </Text>
    </Box>
  );
}

function CodePeek({ children, inverted }: { children: React.ReactNode; inverted?: boolean }) {
  return (
    <Box
      px={11}
      py={9}
      style={{
        fontFamily: FONT_MONO,
        fontSize: 11,
        background: inverted ? "rgba(0,0,0,0.18)" : "rgba(0,0,0,0.3)",
        border: `1px solid ${inverted ? "rgba(255,255,255,0.22)" : LINE}`,
        borderRadius: 8,
        lineHeight: 1.6,
        color: inverted ? "rgba(255,255,255,0.85)" : MUTED,
      }}
    >
      {children}
    </Box>
  );
}

export default function LandingBento() {
  return (
    <Box component="section" id="bento" style={{ padding: "80px 0 40px" }}>
      <Container size="xl" px={32}>
        <Group
          justify="space-between"
          align="flex-end"
          pb={18}
          mb={32}
          style={{ borderBottom: `1px solid ${LINE}` }}
        >
          <Title
            order={2}
            style={{
              fontSize: 54,
              fontWeight: 600,
              letterSpacing: "-0.025em",
              margin: 0,
              lineHeight: 1,
              color: INK,
            }}
          >
            What you <GradientText>get</GradientText>.
          </Title>
          <Box visibleFrom="sm">
            <Kicker>Chapter · 03 / Features</Kicker>
          </Box>
        </Group>

        <Box
          className="bento-grid"
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(12, 1fr)",
            gridAutoRows: "minmax(260px, auto)",
            gap: 14,
          }}
        >
          {/* 01 BIG — Courses, labs, challenges */}
          <Cell span={7} rowSpan={2}>
            <CellHead
              tag="— 01"
              title="Courses, labs, challenges."
              description="A course holds multiple labs. Each lab spins up its own pod from a Docker image. Stack flag or multiple-choice challenges on top — every student in their own sandbox."
              big
            />
            <Group gap={6} style={{ flexWrap: "wrap", maxWidth: 560 }}>
              {chips.map((c) => (
                <Chip key={c.label} label={c.label} tone={c.tone} />
              ))}
            </Group>
          </Cell>

          {/* 02 deploy */}
          <Cell span={5}>
            <CellHead
              tag="— 02"
              title="On-premises by design."
              description="Runs on your own Kubernetes cluster. No external SaaS, no per-seat pricing, no student data leaving campus."
            />
            <Group
              gap={6}
              style={{ flexWrap: "wrap", fontFamily: FONT_MONO, fontSize: 10.5, color: MUTED }}
            >
              {["docker compose", "kubernetes", "keycloak"].map((p) => (
                <Group
                  key={p}
                  gap={6}
                  px={9}
                  py={5}
                  align="center"
                  style={{ border: `1px solid ${LINE_2}`, borderRadius: 6 }}
                >
                  <Box w={6} h={6} style={{ borderRadius: 99, background: MINT }} />
                  <Text style={{ fontFamily: FONT_MONO, fontSize: 10.5, color: MUTED }}>{p}</Text>
                </Group>
              ))}
            </Group>
          </Cell>

          {/* 03 Open-source (accent) */}
          <Cell span={5} accent>
            <CellHead
              tag="— 03"
              title="Open-source. No catch."
              description="Fork it. Brand it. Translate it. Run a hundred instances. We'd love a PR back, but you don't owe us one."
              accent
            />
            <CodePeek inverted>
              ${" "}
              <Text component="b" style={{ color: "#fff", fontWeight: 500 }}>
                git clone
              </Text>{" "}
              <Text component="span" style={{ color: "#dfe7ff" }}>
                github.com/PM4-ISTP/istp
              </Text>
              <br />${" "}
              <Text component="b" style={{ color: "#fff", fontWeight: 500 }}>
                docker compose
              </Text>{" "}
              up -d
            </CodePeek>
          </Cell>

          {/* 04 classroom */}
          <Cell span={4}>
            <CellHead
              tag="— 04"
              title="Built for academic workflows."
              description="Three roles — student, instructor, admin. University email-domain sign-up, per-course progress, no spreadsheets."
            />
            <Box
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: 8,
              }}
            >
              {[
                { n: "24", l: "students", w: 78 },
                { n: "9 / 12", l: "challenges", w: 75 },
                { n: "87%", l: "solved", w: 87 },
                { n: "3", l: "courses", w: 62 },
              ].map((s) => (
                <Stack
                  key={s.l}
                  gap={2}
                  px={10}
                  py={9}
                  style={{ border: `1px solid ${LINE}`, borderRadius: 8 }}
                >
                  <Text
                    style={{ fontFamily: FONT_MONO, fontSize: 16, color: INK, fontWeight: 600 }}
                  >
                    {s.n}
                  </Text>
                  <Text
                    style={{
                      fontSize: 10,
                      color: MUTED,
                      textTransform: "uppercase",
                      letterSpacing: "0.12em",
                    }}
                  >
                    {s.l}
                  </Text>
                  <Progress
                    value={s.w}
                    size={3}
                    mt={4}
                    radius="xl"
                    styles={{
                      root: { background: "rgba(255,255,255,0.06)" },
                      section: { background: GRADIENT },
                    }}
                  />
                </Stack>
              ))}
            </Box>
          </Cell>

          {/* 05 auto-graded */}
          <Cell span={4}>
            <CellHead
              tag="— 05"
              title="Auto-graded, instantly scored."
              description="Flags and multiple-choice answers grade themselves. Pods auto-terminate after 60 minutes idle to free up the cluster."
            />
            <CodePeek>
              <Text component="span" style={{ color: INK_DIM }}>
                # POST /api/submissions
              </Text>
              <br />✓ flag ·{" "}
              <Text component="b" style={{ color: MINT, fontWeight: 500 }}>
                matches
              </Text>
              <br />✓ score ·{" "}
              <Text component="span" style={{ color: ACCENT }}>
                +400 pts
              </Text>
              <br />
              <Text component="span" style={{ color: AMBER }}>
                ⏲ pod idle · 58m left
              </Text>
            </CodePeek>
          </Cell>

          {/* 06 BYOC */}
          <Cell span={4}>
            <CellHead
              tag="— 06"
              title="Course, lab & challenge designer."
              description="Pick a Docker image, write a description, add flag or multiple-choice challenges. Done. The platform handles pods, scoring and lifecycle."
            />
            <CodePeek>
              <Text component="span" style={{ color: INK_DIM }}>
                # new lab
              </Text>
              <br />
              <Text component="span" style={{ color: ACCENT }}>
                image:
              </Text>{" "}
              <Text component="b" style={{ color: MINT, fontWeight: 500 }}>
                ghcr.io/school/sql-inject:1.0
              </Text>
              <br />
              <Text component="span" style={{ color: ACCENT }}>
                challenges:
              </Text>{" "}
              5{" "}
              <Text component="span" style={{ color: MUTED }}>
                ·
              </Text>{" "}
              <Text component="span" style={{ color: ACCENT }}>
                port:
              </Text>{" "}
              8080
            </CodePeek>
          </Cell>
        </Box>
      </Container>

      <style>{`
        @media (max-width: 900px) {
          .bento-grid {
            grid-template-columns: repeat(6, 1fr) !important;
            grid-auto-rows: auto !important;
          }
          .bento-grid > * {
            grid-column: span 6 !important;
            grid-row: auto !important;
          }
        }
      `}</style>
    </Box>
  );
}
