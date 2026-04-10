import { getServerSession } from "next-auth";
import { authOptions } from "@/src/lib/auth";
import {
  Badge,
  Box,
  Button,
  Grid,
  GridCol,
  Group,
  Paper,
  Progress,
  RingProgress,
  SimpleGrid,
  Stack,
  Text,
  ThemeIcon,
  Title,
} from "@mantine/core";
import {
  IconBook2,
  IconBolt,
  IconTrophy,
  IconClock,
  IconArrowRight,
  IconShieldCheck,
  IconBug,
  IconLock,
  IconTerminal2,
  IconChevronRight,
} from "@tabler/icons-react";

const labelStyle: React.CSSProperties = {
  fontFamily: "var(--font-space-grotesk), 'Space Grotesk', sans-serif",
  textTransform: "uppercase",
  letterSpacing: "0.14em",
  fontSize: "0.6rem",
  fontWeight: 700,
  color: "#5B606B",
};

const statAccentColors: Record<string, string> = {
  blue: "#3B82F6",
  teal: "#10B981",
  orange: "#F59E0B",
  grape: "#8B5CF6",
};

function StatCard({
  icon,
  label,
  value,
  sub,
  color,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  sub?: string;
  color: string;
}) {
  const accent = statAccentColors[color] ?? "#3B82F6";
  return (
    <Paper
      withBorder
      radius="lg"
      p="lg"
      style={{ borderColor: "#E5EEFF", borderTop: `3px solid ${accent}` }}
    >
      <Group align="flex-start" justify="space-between" wrap="nowrap">
        <Stack gap={4}>
          <Text style={labelStyle}>{label}</Text>
          <Text fw={700} size="xl" style={{ color: "#001E41", lineHeight: 1.2 }}>
            {value}
          </Text>
          {sub && (
            <Text size="xs" c="dimmed">
              {sub}
            </Text>
          )}
        </Stack>
        <ThemeIcon size="lg" radius="md" color={color} variant="light">
          {icon}
        </ThemeIcon>
      </Group>
    </Paper>
  );
}

function PlaceholderCourseCard({
  title,
  topic,
  progress,
  icon,
  accentColor = "#3B82F6",
}: {
  title: string;
  topic: string;
  progress: number;
  icon: React.ReactNode;
  accentColor?: string;
}) {
  return (
    <Paper
      withBorder
      radius="lg"
      p="lg"
      style={{ borderColor: "#E5EEFF", borderLeft: `3px solid ${accentColor}` }}
    >
      <Stack gap="sm">
        <Group align="flex-start" justify="space-between" wrap="nowrap">
          <Stack gap={2} style={{ flex: 1 }}>
            <Text fw={600} size="sm" lineClamp={2} style={{ color: "#001E41" }}>
              {title}
            </Text>
            <Badge size="xs" variant="light" color="blue">
              {topic}
            </Badge>
          </Stack>
          <ThemeIcon size="md" radius="md" color="blue" variant="light">
            {icon}
          </ThemeIcon>
        </Group>
        <Box>
          <Group justify="space-between" mb={4}>
            <Text size="xs" c="dimmed">
              Progress
            </Text>
            <Text size="xs" fw={600} c="blue">
              {progress}%
            </Text>
          </Group>
          <Progress value={progress} size="sm" radius="xl" color="blue" />
        </Box>
      </Stack>
    </Paper>
  );
}

function ActivityItem({
  label,
  time,
  color,
}: {
  label: string;
  time: string;
  color: string;
}) {
  return (
    <Group justify="space-between" wrap="nowrap">
      <Group gap="sm" wrap="nowrap">
        <Box
          style={{
            width: 8,
            height: 8,
            borderRadius: "50%",
            background: color,
            flexShrink: 0,
          }}
        />
        <Text size="sm" c="dimmed">
          {label}
        </Text>
      </Group>
      <Text size="xs" c="dimmed" style={{ flexShrink: 0 }}>
        {time}
      </Text>
    </Group>
  );
}

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
    <Stack p="xl" gap="xl" style={{ background: "#F4F7FF", minHeight: "100%" }}>
      {/* Hero banner */}
      <Box
        style={{
          background: "linear-gradient(135deg, #060D1A 0%, #0A1628 50%, #0D1F5C 100%)",
          borderRadius: 16,
          padding: "2rem 2.5rem",
          position: "relative",
          overflow: "hidden",
        }}
      >
        {/* Dot grid overlay */}
        <Box
          style={{
            position: "absolute",
            inset: 0,
            backgroundImage: "radial-gradient(circle, rgba(59,130,246,0.18) 1px, transparent 1px)",
            backgroundSize: "24px 24px",
            pointerEvents: "none",
          }}
        />
        {/* Blue glow top-right */}
        <Box
          style={{
            position: "absolute",
            top: -60,
            right: -40,
            width: 240,
            height: 240,
            borderRadius: "50%",
            background: "radial-gradient(circle, rgba(59,130,246,0.35) 0%, transparent 70%)",
            pointerEvents: "none",
          }}
        />
        {/* Cyan glow bottom-left */}
        <Box
          style={{
            position: "absolute",
            bottom: -60,
            left: -40,
            width: 180,
            height: 180,
            borderRadius: "50%",
            background: "radial-gradient(circle, rgba(6,182,212,0.2) 0%, transparent 70%)",
            pointerEvents: "none",
          }}
        />
        <Stack gap={6} style={{ position: "relative" }}>
          <Text style={{ ...labelStyle, color: "rgba(255,255,255,0.45)" }}>{dateStr}</Text>
          <Title
            order={1}
            style={{
              fontFamily: "var(--font-manrope), 'Manrope', sans-serif",
              fontWeight: 800,
              color: "#fff",
              fontSize: "2rem",
            }}
          >
            Welcome back,{" "}
            <span
              style={{
                fontFamily: "var(--font-orbitron), 'Orbitron', sans-serif",
                fontWeight: 900,
                background: "linear-gradient(120deg, #3B82F6 0%, #06B6D4 60%, #818CF8 100%)",
                WebkitBackgroundClip: "text",
                WebkitTextFillColor: "transparent",
                backgroundClip: "text",
              }}
            >
              {firstName}!
            </span>
          </Title>
          <Text size="sm" style={{ color: "rgba(255,255,255,0.55)" }}>
            Here&apos;s an overview of your learning progress.
          </Text>
          <Group mt={12} gap="sm">
            <Button
              size="sm"
              radius="xl"
              variant="filled"
              color="blue"
              rightSection={<IconChevronRight size={14} />}
            >
              Browse Courses
            </Button>
            <Button
              size="sm"
              radius="xl"
              variant="outline"
              style={{ borderColor: "rgba(255,255,255,0.3)", color: "#fff" }}
            >
              View Leaderboard
            </Button>
          </Group>
        </Stack>
      </Box>

      {/* Stats row */}
      <SimpleGrid cols={{ base: 1, xs: 2, md: 4 }} spacing="md">
        <StatCard
          icon={<IconBook2 size={18} />}
          label="Enrolled Courses"
          value="—"
          sub="Placeholder"
          color="blue"
        />
        <StatCard
          icon={<IconTrophy size={18} />}
          label="Completed"
          value="—"
          sub="Placeholder"
          color="teal"
        />
        <StatCard
          icon={<IconBolt size={18} />}
          label="Current Streak"
          value="—"
          sub="Placeholder"
          color="orange"
        />
        <StatCard
          icon={<IconClock size={18} />}
          label="Hours Learned"
          value="—"
          sub="Placeholder"
          color="grape"
        />
      </SimpleGrid>

      {/* Main content row */}
      <Grid gutter="md">
        {/* Continue learning */}
        <GridCol span={{ base: 12, md: 8 }}>
          <Stack gap="sm">
            <Group justify="space-between" align="center">
              <Text style={{ ...labelStyle, color: "#001E41" }}>Continue Learning</Text>
              <Group gap={4} style={{ cursor: "pointer", color: "#3B82F6" }}>
                <Text size="xs" c="blue" fw={600}>
                  View all
                </Text>
                <IconArrowRight size={13} color="#3B82F6" />
              </Group>
            </Group>
            <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
              <PlaceholderCourseCard
                title="Introduction to Web Application Security"
                topic="Web Security"
                progress={42}
                icon={<IconShieldCheck size={16} />}
                accentColor="#3B82F6"
              />
              <PlaceholderCourseCard
                title="Common Vulnerabilities and Exploits (CVE)"
                topic="Vulnerabilities"
                progress={15}
                icon={<IconBug size={16} />}
                accentColor="#EF4444"
              />
              <PlaceholderCourseCard
                title="Cryptography Fundamentals"
                topic="Cryptography"
                progress={68}
                icon={<IconLock size={16} />}
                accentColor="#10B981"
              />
              <PlaceholderCourseCard
                title="Network Penetration Testing"
                topic="Pentesting"
                progress={5}
                icon={<IconBolt size={16} />}
                accentColor="#F59E0B"
              />
            </SimpleGrid>
          </Stack>
        </GridCol>

        {/* Right column */}
        <GridCol span={{ base: 12, md: 4 }}>
          <Stack gap="md">
            {/* Overall progress */}
            <Paper withBorder radius="lg" p="lg" style={{ borderColor: "#E5EEFF" }}>
              <Stack gap="sm" align="center">
                <Text style={{ ...labelStyle, alignSelf: "flex-start" }}>Overall Progress</Text>
                <RingProgress
                  size={120}
                  thickness={12}
                  sections={[{ value: 33, color: "blue" }]}
                  label={
                    <Text size="sm" fw={700} ta="center" c="blue">
                      33%
                    </Text>
                  }
                />
                <Text size="xs" c="dimmed" ta="center">
                  Placeholder — 2 of 6 courses completed
                </Text>
              </Stack>
            </Paper>

            {/* Security Tip of the Day */}
            <Paper
              withBorder
              radius="lg"
              p="lg"
              style={{
                borderColor: "#1E3A5F",
                background: "linear-gradient(135deg, #060D1A 0%, #0A1628 100%)",
              }}
            >
              <Stack gap="xs">
                <Group gap="xs">
                  <ThemeIcon size="sm" radius="md" color="yellow" variant="light">
                    <IconTerminal2 size={14} />
                  </ThemeIcon>
                  <Text style={{ ...labelStyle, color: "rgba(255,255,255,0.45)" }}>
                    Security Tip
                  </Text>
                </Group>
                <Text size="sm" style={{ color: "rgba(255,255,255,0.85)", lineHeight: 1.6 }}>
                  Always enable multi-factor authentication — it blocks over 99% of automated
                  account compromise attacks.
                </Text>
              </Stack>
            </Paper>

            {/* Recent Activity */}
            <Paper withBorder radius="lg" p="lg" style={{ borderColor: "#E5EEFF" }}>
              <Stack gap="sm">
                <Text style={labelStyle}>Recent Activity</Text>
                <ActivityItem
                  label="Completed lesson: SQL Injection"
                  time="2h ago"
                  color="#3B82F6"
                />
                <ActivityItem
                  label="Started: Cryptography Fundamentals"
                  time="Yesterday"
                  color="#10B981"
                />
                <ActivityItem
                  label="Earned badge: Quick Learner"
                  time="2d ago"
                  color="#F59E0B"
                />
                <ActivityItem
                  label="Enrolled in: Network Pentesting"
                  time="3d ago"
                  color="#8B5CF6"
                />
                <Text size="xs" c="dimmed" ta="center" mt={4}>
                  Placeholder data
                </Text>
              </Stack>
            </Paper>
          </Stack>
        </GridCol>
      </Grid>
    </Stack>
  );
}
