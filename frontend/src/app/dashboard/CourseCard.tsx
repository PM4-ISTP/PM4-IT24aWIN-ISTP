import { Badge, Box, Group, Paper, Progress, Stack, Text, ThemeIcon } from "@mantine/core";

export default function CourseCard({
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
    <Paper
      withBorder
      radius="lg"
      p="lg"
      className="dashboard-course-card"
      style={{ borderColor: "var(--istp-card-border)" }}
    >
      <Stack gap="sm">
        <Group align="flex-start" justify="space-between" wrap="nowrap">
          <Stack gap={2} style={{ flex: 1 }}>
            <Text fw={600} size="md" lineClamp={2} style={{ color: "var(--istp-heading-color)" }}>
              {title}
            </Text>
            <Badge size="sm" variant="light" color="blue">
              {topic}
            </Badge>
          </Stack>
          <ThemeIcon size="md" radius="md" color="blue" variant="light">
            {icon}
          </ThemeIcon>
        </Group>
        <Box>
          <Group justify="space-between" mb={4}>
            <Text size="sm" c="dimmed">
              Progress
            </Text>
            <Text size="sm" fw={600} c="blue">
              {progress}%
            </Text>
          </Group>
          <Progress value={progress} size="sm" radius="xl" color="blue" />
        </Box>
      </Stack>
    </Paper>
  );
}
