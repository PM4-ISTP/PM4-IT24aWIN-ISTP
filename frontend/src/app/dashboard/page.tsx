import { getServerSession } from "next-auth";
import { authOptions } from "@/src/lib/auth";
import {
  Badge,
  Box,
  Grid,
  GridCol,
  Group,
  Paper,
  Progress,
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
} from "@tabler/icons-react";

const labelStyle: React.CSSProperties = {
  fontFamily: "var(--font-space-grotesk), 'Space Grotesk', sans-serif",
  textTransform: "uppercase",
  letterSpacing: "0.14em",
  fontSize: "0.6rem",
  fontWeight: 700,
  color: "#5B606B",
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
  return (
    <Paper withBorder radius="lg" p="lg" style={{ borderColor: "#E5EEFF" }}>
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
}: {
  title: string;
  topic: string;
  progress: number;
  icon: React.ReactNode;
}) {
  return (
    <Paper withBorder radius="lg" p="lg" style={{ borderColor: "#E5EEFF" }}>
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
    <Stack p="xl" gap="xl">
      {/* Welcome header */}
      <Stack gap={4}>
        <Text style={labelStyle}>{dateStr}</Text>
        <Title
          order={1}
          style={{
            fontFamily: "var(--font-manrope), 'Manrope', sans-serif",
            fontWeight: 800,
            color: "#001E41",
            fontSize: "2rem",
          }}
        >
          Welcome back, {firstName} 👋
        </Title>
        <Text size="sm" c="dimmed">
          Here's an overview of your learning progress.
        </Text>
      </Stack>

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
              />
              <PlaceholderCourseCard
                title="Common Vulnerabilities and Exploits (CVE)"
                topic="Vulnerabilities"
                progress={15}
                icon={<IconBug size={16} />}
              />
              <PlaceholderCourseCard
                title="Cryptography Fundamentals"
                topic="Cryptography"
                progress={68}
                icon={<IconLock size={16} />}
              />
              <PlaceholderCourseCard
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
            <Paper withBorder radius="lg" p="lg" style={{ borderColor: "#E5EEFF" }}>
              <Stack gap="sm">
                <Text style={labelStyle}>Overall Progress</Text>
                <Group justify="space-between">
                  <Text size="sm" c="dimmed">
                    Courses completed
                  </Text>
                  <Text size="sm" fw={700} c="blue">
                    33%
                  </Text>
                </Group>
                <Progress value={33} size="md" radius="xl" color="blue" />
                <Text size="xs" c="dimmed" ta="center">
                  Placeholder — 2 of 6 courses completed
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
