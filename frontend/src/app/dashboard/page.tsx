import { getServerSession } from "next-auth";
import { authOptions } from "@/src/lib/auth";
import { Grid, GridCol, Group, Paper, RingProgress, SimpleGrid, Stack, Text } from "@mantine/core";
import { IconArrowRight, IconBolt, IconBug, IconLock, IconShieldCheck } from "@tabler/icons-react";
import DashboardStyles from "./DashboardStyles";
import DashboardHero from "./DashboardHero";
import CourseCard from "./CourseCard";
import { labelStyle } from "./_shared";

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
    <Stack p="xl" gap="xl">
      <DashboardStyles />

      <DashboardHero firstName={firstName} dateStr={dateStr} />

      {/* Main content row */}
      <Grid gutter="md">
        {/* Continue learning */}
        <GridCol span={{ base: 12, md: 8 }}>
          <Stack gap="sm">
            <Group justify="space-between" align="center">
              <Text style={{ ...labelStyle, color: "var(--istp-heading-color)", fontSize: "0.82rem" }}>
                Continue Learning
              </Text>
              <Group gap={4} style={{ cursor: "pointer", color: "#3B82F6" }}>
                <Text size="sm" c="blue" fw={600}>
                  View all
                </Text>
                <IconArrowRight size={15} color="#3B82F6" />
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
            <Paper withBorder radius="lg" p="lg" style={{ borderColor: "var(--istp-card-border)" }}>
              <Stack gap="sm" align="center">
                <Text style={{ ...labelStyle, alignSelf: "flex-start", fontSize: "0.82rem" }}>
                  Overall Progress
                </Text>
                <RingProgress
                  size={130}
                  thickness={13}
                  sections={[{ value: 33, color: "blue" }]}
                  label={
                    <Text size="md" fw={700} ta="center" c="blue">
                      33%
                    </Text>
                  }
                />
                <Text size="sm" c="dimmed" ta="center">
                  Placeholder — 2 of 6 courses completed
                </Text>
              </Stack>
            </Paper>


          </Stack>
        </GridCol>
      </Grid>
    </Stack>
  );
}
