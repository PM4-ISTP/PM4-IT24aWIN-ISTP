import { getServerSession } from "next-auth";
import { authOptions } from "@/src/lib/auth";
import { Grid, GridCol, Group, RingProgress, SimpleGrid, Stack, Text, Box } from "@mantine/core";
import { IconArrowRight, IconBolt, IconBug, IconLock, IconShieldCheck } from "@tabler/icons-react";
import DashboardStyles from "./DashboardStyles";
import DashboardHero from "./DashboardHero";
import CourseCard from "./CourseCard";

const sectionLabelStyle: React.CSSProperties = {
  fontFamily: "var(--font-space-grotesk), sans-serif",
  textTransform: "uppercase",
  letterSpacing: "0.1em",
  fontSize: "0.72rem",
  fontWeight: 700,
  color: "rgba(255,255,255,0.45)",
};

export default async function Home() {
  const session = await getServerSession(authOptions);
  const name = session?.user?.name ?? "there";
  const firstName = name.split(" ")[0];

  const today = new Date();
  const dateStr = today.toLocaleDateString("en-GB", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  });

  return (
    <Stack gap="xl">
      <DashboardStyles />

      <DashboardHero firstName={firstName} dateStr={dateStr} />

      {/* Main content row */}
      <Grid gutter="md">
        {/* Continue learning */}
        <GridCol span={{ base: 12, md: 8 }}>
          <Stack gap="sm">
            <Group justify="space-between" align="center">
              <Text style={sectionLabelStyle}>Continue Learning</Text>
              <Group gap={4} style={{ cursor: "pointer" }}>
                <Text
                  size="sm"
                  fw={600}
                  style={{
                    color: "#60a5fa",
                    fontFamily: "var(--font-space-grotesk), sans-serif",
                  }}
                >
                  View all
                </Text>
                <IconArrowRight size={15} color="#60a5fa" />
              </Group>
            </Group>
            <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
              <CourseCard
                title="Introduction to Web Application Security"
                topic="Web Security"
                progress={42}
                icon={<IconShieldCheck size={16} />}
              />
              <CourseCard
                title="Common Vulnerabilities and Exploits (CVE)"
                topic="Vulnerabilities"
                progress={15}
                icon={<IconBug size={16} />}
              />
              <CourseCard
                title="Cryptography Fundamentals"
                topic="Cryptography"
                progress={68}
                icon={<IconLock size={16} />}
              />
              <CourseCard
                title="Network Penetration Testing"
                topic="Pentesting"
                progress={5}
                icon={<IconBolt size={16} />}
              />
            </SimpleGrid>
          </Stack>
        </GridCol>

        {/* Right column */}
        <GridCol span={{ base: 12, md: 4 }}>
          <Stack gap="md">
            {/* Overall progress */}
            <Box
              style={{
                background: "rgba(255,255,255,0.04)",
                border: "1px solid rgba(255,255,255,0.08)",
                borderRadius: 14,
                padding: "1.5rem",
                boxShadow: "0 4px 24px rgba(0,0,0,0.25)",
              }}
            >
              <Stack gap="sm" align="center">
                <Text style={{ ...sectionLabelStyle, alignSelf: "flex-start" }}>
                  Overall Progress
                </Text>
                <RingProgress
                  size={130}
                  thickness={13}
                  sections={[{ value: 33, color: "#2563eb" }]}
                  label={
                    <Text size="md" fw={700} ta="center" style={{ color: "#60a5fa" }}>
                      33%
                    </Text>
                  }
                />
                <Text size="sm" ta="center" style={{ color: "#64748b" }}>
                  Placeholder — 2 of 6 courses completed
                </Text>
              </Stack>
            </Box>
          </Stack>
        </GridCol>
      </Grid>
    </Stack>
  );
}
